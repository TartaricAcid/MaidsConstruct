package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryExtraMaidBrain;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.smeltery.CastingHelper;
import com.github.tartaricacid.maidsconstruct.util.smeltery.SmelteryBubbles;
import com.github.tartaricacid.maidsconstruct.util.smeltery.SmelteryHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidMoveToBlockTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidPathFindingBFS;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

public class MaidSmelterySearchTask extends MaidMoveToBlockTask {
    /**
     * 检查方块可达性的范围，默认检查寻路点周围 3x3x3 范围内的方块的可达性
     */
    private static final BoundingBox CHECK_RANGE = new BoundingBox(-1, -1, -1, 1, 1, 1);
    private static final int MAX_DELAY = 60;

    private final float speed;

    public MaidSmelterySearchTask(float speed, int verticalSearchRange) {
        super(speed, verticalSearchRange);
        this.speed = speed;
        this.setMaxCheckRate(MAX_DELAY);
    }

    @Override
    protected boolean shouldMoveTo(ServerLevel level, EntityMaid maid, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        // 必须是冶炼炉，并且结构完整
        return be instanceof HeatingStructureBlockEntity smeltery && smeltery.getStructure() != null;
    }

    @Override
    protected boolean checkPathReach(EntityMaid maid, MaidPathFindingBFS pathFinding, BlockPos pos) {
        for (int x = CHECK_RANGE.minX(); x <= CHECK_RANGE.maxX(); x++) {
            for (int y = CHECK_RANGE.minY(); y <= CHECK_RANGE.maxY(); y++) {
                for (int z = CHECK_RANGE.minZ(); z <= CHECK_RANGE.maxZ(); z++) {
                    if (pathFinding.canPathReach(pos.offset(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        // 需要额外检查并切换状态机
        return super.checkExtraStartConditions(level, maid)
               && this.evaluateAndAct(level, maid);
    }

    private boolean evaluateAndAct(ServerLevel level, EntityMaid maid) {
        // 获取或查找冶炼炉位置
        @Nullable BlockPos smelteryPos = maid.getBrain()
                .getMemory(InitMemories.SMELTERY_POS.get())
                .orElse(null);

        // 验证存储的冶炼炉是否仍然有效（结构完整且在工作范围内）
        if (smelteryPos != null) {
            BlockEntity be = level.getBlockEntity(smelteryPos);
            if (!(be instanceof HeatingStructureBlockEntity smeltery)
                || smeltery.getStructure() == null
                || !maid.isWithinRestriction(smelteryPos)) {
                SmelteryExtraMaidBrain.clearAllMemories(maid);
                smelteryPos = null;
            }
        }

        // 如果没有冶炼炉，使用 MaidMoveToBlockTask 的 BFS 搜索查找
        if (smelteryPos == null) {
            super.searchForDestination(level, maid);

            // 从 searchForDestination 设置的 TARGET_POS 中提取找到的位置
            smelteryPos = maid.getBrain().getMemory(InitEntities.TARGET_POS.get())
                    .map(PositionTracker::currentBlockPosition)
                    .orElse(null);

            // 如果仍然没有找到冶炼炉，稍后再试
            if (smelteryPos == null) {
                this.setNextCheckTickCount(200);
                return false;
            }

            // 缓存冶炼炉位置
            maid.getBrain().setMemory(InitMemories.SMELTERY_POS.get(), smelteryPos);

            // 清除 searchForDestination 设置的行走目标
            // 后续由 setActionTarget 设定实际目标
            maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            maid.getBrain().eraseMemory(InitEntities.TARGET_POS.get());
        }

        // 此时 smelteryPos 不为空
        if (level.getBlockEntity(smelteryPos) instanceof HeatingStructureBlockEntity smeltery) {
            // 获取当前工作状态
            SmelteryWorkState state = maid.getBrain()
                    .getMemory(InitMemories.SMELTERY_STATE.get())
                    .orElse(SmelteryWorkState.IDLE);

            return switch (state) {
                case IDLE -> handleIdle(level, maid, smeltery, smelteryPos);
                case WAITING_MELT -> handleWaitingMelt(maid, smeltery);
                case WAITING_CAST -> handleWaitingCast(level, maid, smeltery);
                // 其他状态下可能存在 TARGET_POS 丢失问题
                // 重置回 IDLE 以重新决策
                default -> resetToIdle(maid, 5);
            };
        }

        // 不太可能发生，但是依然清理记忆
        SmelteryExtraMaidBrain.clearAllMemories(maid);
        return false;
    }

    private boolean handleIdle(ServerLevel level, EntityMaid maid, HeatingStructureBlockEntity smeltery, BlockPos smelteryPos) {
        // 优先级 1：从浇铸台收集已完成的物品
        Pair<BlockPos, BlockPos> collectable = CastingHelper.findCollectableOutput(level, smeltery);
        if (collectable != null) {
            return setActionTarget(maid, collectable.getSecond(), SmelteryWorkState.COLLECTING);
        }

        // 优先级 2：如果浇注口正在浇铸或浇铸台正在冷却，等待
        if (CastingHelper.isAnyFaucetPouring(level, smeltery) || CastingHelper.isAnyCastingCooling(level, smeltery)) {
            maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.WAITING_CAST);
            SmelteryBubbles.showStateBubble(maid, SmelteryWorkState.WAITING_CAST);
            this.setNextCheckTickCount(20);
            return false;
        }

        // 优先级 3：如果冶炼炉有流体，进行浇铸
        if (!SmelteryHelper.isTankEmpty(smeltery)) {
            Pair<BlockPos, BlockPos> pouringTarget = CastingHelper.findBestPouringTarget(level, smeltery);
            if (pouringTarget != null) {
                return setActionTarget(maid, pouringTarget.getFirst(), SmelteryWorkState.POURING);
            }

            // 未找到合适的浇铸目标，但冶炼炉还有空位且女仆有矿石 → 继续投入
            if (SmelteryHelper.hasEmptySlots(smeltery) && SmelteryHelper.hasMeltableItems(maid, smeltery)) {
                return setActionTarget(maid, smelteryPos, SmelteryWorkState.INTERACTING);
            }

            // 未找到合适的浇铸目标，也没有可投入的矿石
            // 女仆发出聊天气泡提示玩家
            SmelteryBubbles.addBubbleIfNotTooMany(maid, SmelteryBubbles.randomBubble(maid, SmelteryBubbles.NO_CAST_BUBBLES));
            this.setNextCheckTickCount(200);
            return false;
        }

        // 优先级 4：如果物品仍在熔炼中，等待
        if (!SmelteryHelper.allMeltableItemsMelted(smeltery)) {
            maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.WAITING_MELT);
            SmelteryBubbles.showStateBubble(maid, SmelteryWorkState.WAITING_MELT);
            this.setNextCheckTickCount(MAX_DELAY);
            return false;
        }

        // 优先级 5：如果冶炼炉中有不可熔化的物品，前往冶炼炉提取
        if (SmelteryHelper.hasNonMeltableItems(smeltery)) {
            return setActionTarget(maid, smelteryPos, SmelteryWorkState.INTERACTING);
        }

        // 优先级 6：冶炼炉为空 -> 添加燃料（如果有的话，否则停止工作）
        if (SmelteryHelper.needsFuel(smeltery)) {
            // 尝试添加燃料（仅添加配置白名单中的流体燃料）
            if (SmelteryHelper.hasFuelItems(maid)) {
                return setActionTarget(maid, smelteryPos, SmelteryWorkState.FUELING);
            }

            // 女仆没有岩浆，且冶炼炉中也没有燃料 -> 不投入物品，等待玩家补充燃料
            SmelteryBubbles.showNoFuelBubble(maid);
            this.setNextCheckTickCount(200);
            return false;
        }

        // 优先级 7：检查女仆是否有可熔炼物品（合金规避：仅在冶炼炉为空时插入）
        if (SmelteryHelper.isTankEmpty(smeltery) && SmelteryHelper.hasMeltableItems(maid, smeltery)) {
            if (SmelteryHelper.hasEmptySlots(smeltery)) {
                return setActionTarget(maid, smelteryPos, SmelteryWorkState.INTERACTING);
            }
        }

        // 无事可做
        if (!SmelteryHelper.hasMeltableItems(maid, smeltery) && SmelteryHelper.isTankEmpty(smeltery)) {
            // 没有物品也没有流体 - 女仆无事可做
            SmelteryBubbles.showNoItemsBubble(maid);
            this.setNextCheckTickCount(200);
            return false;
        }

        this.setNextCheckTickCount(MAX_DELAY);
        return false;
    }

    private boolean handleWaitingMelt(EntityMaid maid, HeatingStructureBlockEntity smeltery) {
        if (SmelteryHelper.allMeltableItemsMelted(smeltery)) {
            return resetToIdle(maid, 5);
        }
        // 仍在熔炼中，稍后再检查
        this.setNextCheckTickCount(MAX_DELAY);
        return false;
    }

    private boolean handleWaitingCast(ServerLevel level, EntityMaid maid, HeatingStructureBlockEntity smeltery) {
        // 检查是否有浇铸台已经能够收集
        Pair<BlockPos, BlockPos> collectable = CastingHelper.findCollectableOutput(level, smeltery);
        if (collectable != null) {
            return setActionTarget(maid, collectable.getSecond(), SmelteryWorkState.COLLECTING);
        }

        // 检查是否仍在冷却/浇铸
        if (CastingHelper.isAnyFaucetPouring(level, smeltery) || CastingHelper.isAnyCastingCooling(level, smeltery)) {
            this.setNextCheckTickCount(20);
            return false;
        }

        // 已无冷却中的物品 - 检查流体罐是否还有流体可以继续浇铸
        if (!SmelteryHelper.isTankEmpty(smeltery)) {
            Pair<BlockPos, BlockPos> pouringTarget = CastingHelper.findBestPouringTarget(level, smeltery);
            if (pouringTarget != null) {
                return setActionTarget(maid, pouringTarget.getFirst(), SmelteryWorkState.POURING);
            }
            // 没有可用目标，表明此时浇筑台都不可用，切回 idle 状态等待
            return resetToIdle(maid, MAX_DELAY);
        }

        // 流体罐已空，浇铸完成，返回空闲状态
        return resetToIdle(maid, MAX_DELAY);
    }

    private boolean resetToIdle(EntityMaid maid, int maxDelay) {
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.IDLE);
        this.setNextCheckTickCount(maxDelay);
        return false;
    }

    private boolean setActionTarget(EntityMaid maid, BlockPos targetPos, SmelteryWorkState newState) {
        BehaviorUtils.setWalkAndLookTargetMemories(maid, targetPos, speed, 1);
        maid.getBrain().setMemory(InitEntities.TARGET_POS.get(), new BlockPosTracker(targetPos));
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), newState);
        SmelteryBubbles.showStateBubble(maid, newState);
        this.setNextCheckTickCount(5);
        return true;
    }
}
