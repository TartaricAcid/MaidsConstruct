package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.SmelteryHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

public class MaidSmelterySearchTask extends MaidCheckRateTask {
    public static final String[] NO_CAST_BUBBLES = {
            "chat.maidsconstruct.no_cast.0",
            "chat.maidsconstruct.no_cast.1",
            "chat.maidsconstruct.no_cast.2"
    };
    public static final String[] INVENTORY_FULL_BUBBLES = {
            "chat.maidsconstruct.inventory_full.0",
            "chat.maidsconstruct.inventory_full.1",
            "chat.maidsconstruct.inventory_full.2"
    };
    public static final String[] COLLECT_SUCCESS_BUBBLES = {
            "chat.maidsconstruct.collect_success.0",
            "chat.maidsconstruct.collect_success.1",
            "chat.maidsconstruct.collect_success.2"
    };
    private static final int MAX_DELAY = 60;
    private static final String[] FUELING_BUBBLES = {
            "chat.maidsconstruct.state.fueling.0",
            "chat.maidsconstruct.state.fueling.1",
            "chat.maidsconstruct.state.fueling.2"
    };
    private static final String[] INSERTING_BUBBLES = {
            "chat.maidsconstruct.state.inserting.0",
            "chat.maidsconstruct.state.inserting.1",
            "chat.maidsconstruct.state.inserting.2"
    };
    private static final String[] POURING_BUBBLES = {
            "chat.maidsconstruct.state.pouring.0",
            "chat.maidsconstruct.state.pouring.1",
            "chat.maidsconstruct.state.pouring.2"
    };
    private static final String[] COLLECTING_BUBBLES = {
            "chat.maidsconstruct.state.collecting.0",
            "chat.maidsconstruct.state.collecting.1",
            "chat.maidsconstruct.state.collecting.2"
    };
    private static final String[] WAITING_MELT_BUBBLES = {
            "chat.maidsconstruct.state.waiting_melt.0",
            "chat.maidsconstruct.state.waiting_melt.1",
            "chat.maidsconstruct.state.waiting_melt.2"
    };
    private static final String[] WAITING_CAST_BUBBLES = {
            "chat.maidsconstruct.state.waiting_cast.0",
            "chat.maidsconstruct.state.waiting_cast.1",
            "chat.maidsconstruct.state.waiting_cast.2"
    };
    private final float speed;
    private final int verticalSearchRange;

    public MaidSmelterySearchTask(float speed, int verticalSearchRange) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_ABSENT
        ));
        this.speed = speed;
        this.verticalSearchRange = verticalSearchRange;
        this.setMaxCheckRate(MAX_DELAY);
    }

    private static final int MAX_CHAT_BUBBLES = 2;

    public static String randomBubble(EntityMaid maid, String[] pool) {
        return pool[maid.getRandom().nextInt(pool.length)];
    }

    public static void addBubbleIfNotTooMany(EntityMaid maid, String langKey) {
        if (maid.getChatBubbleManager().getChatBubbleDataCollection().size() < MAX_CHAT_BUBBLES) {
            maid.getChatBubbleManager().addTextChatBubble(langKey);
        }
    }

    public static void clearActionMemories(EntityMaid maid) {
        maid.getBrain().eraseMemory(InitEntities.TARGET_POS.get());
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(InitMemories.ACTION_TARGET_POS.get());
    }

    public static void clearAllMemories(EntityMaid maid) {
        clearActionMemories(maid);
        maid.getBrain().eraseMemory(InitMemories.SMELTERY_STATE.get());
        maid.getBrain().eraseMemory(InitMemories.SMELTERY_POS.get());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (!super.checkExtraStartConditions(level, maid)) {
            return false;
        }
        return evaluateAndAct(level, maid);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        // 实际逻辑在 checkExtraStartConditions 中设置记忆。
        // start() 在检查通过后调用，但我们已在 evaluateAndAct() 中设置了行走目标。
    }

    private boolean evaluateAndAct(ServerLevel level, EntityMaid maid) {
        // 获取或查找冶炼炉位置
        BlockPos smelteryPos = maid.getBrain().getMemory(InitMemories.SMELTERY_POS.get()).orElse(null);

        // 验证存储的冶炼炉是否仍然有效
        if (smelteryPos != null) {
            BlockEntity be = level.getBlockEntity(smelteryPos);
            if (!(be instanceof HeatingStructureBlockEntity smeltery) || smeltery.getStructure() == null) {
                clearAllMemories(maid);
                smelteryPos = null;
            }
        }

        // 如果没有冶炼炉，查找一个
        if (smelteryPos == null) {
            smelteryPos = SmelteryHelper.findNearestSmeltery(level, maid);
            if (smelteryPos == null) {
                this.setNextCheckTickCount(100);
                return false;
            }
            maid.getBrain().setMemory(InitMemories.SMELTERY_POS.get(), smelteryPos);
        }

        BlockEntity be = level.getBlockEntity(smelteryPos);
        if (!(be instanceof HeatingStructureBlockEntity smeltery)) {
            clearAllMemories(maid);
            return false;
        }

        // 获取当前工作状态
        SmelteryWorkState state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get())
                .orElse(SmelteryWorkState.IDLE);

        return switch (state) {
            case IDLE -> handleIdle(level, maid, smeltery, smelteryPos);
            case WAITING_MELT -> handleWaitingMelt(level, maid, smeltery);
            case WAITING_CAST -> handleWaitingCast(level, maid, smeltery);
            default -> false; // 其他状态由动作行为处理
        };
    }

    private boolean handleIdle(ServerLevel level, EntityMaid maid, HeatingStructureBlockEntity smeltery, BlockPos smelteryPos) {
        // 优先级1：从浇铸台收集已完成的物品
        Pair<BlockPos, BlockPos> collectable = SmelteryHelper.findCollectableOutput(level, smeltery);
        if (collectable != null) {
            return setActionTarget(maid, collectable.getSecond(), SmelteryWorkState.COLLECTING);
        }

        // 优先级2：如果浇口正在浇铸或浇铸台正在冷却，等待
        if (SmelteryHelper.isAnyFaucetPouring(level, smeltery) || SmelteryHelper.isAnyCastingCooling(level, smeltery)) {
            maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.WAITING_CAST);
            showStateBubble(maid, SmelteryWorkState.WAITING_CAST);
            this.setNextCheckTickCount(20);
            return false;
        }

        // 优先级3：如果流体罐有流体且所有物品已熔化，进行浇铸
        if (!SmelteryHelper.isTankEmpty(smeltery) && SmelteryHelper.allItemsMelted(smeltery)) {
            Pair<BlockPos, BlockPos> pouringTarget = SmelteryHelper.findBestPouringTarget(level, smeltery);
            if (pouringTarget != null) {
                return setActionTarget(maid, pouringTarget.getFirst(), SmelteryWorkState.POURING);
            }

            // 未找到合适的浇铸目标，但冶炼炉还有空位且女仆有矿石 → 继续投入
            if (SmelteryHelper.hasEmptySlots(smeltery) && SmelteryHelper.hasMeltableItems(maid)) {
                return setActionTarget(maid, smelteryPos, SmelteryWorkState.INSERTING);
            }

            // 未找到合适的浇铸目标，也没有可投入的矿石
            if (!SmelteryHelper.hasCastingWithMold(level, smeltery)) {
                addBubbleIfNotTooMany(maid, randomBubble(maid, NO_CAST_BUBBLES));
                this.setNextCheckTickCount(200);
                return false;
            }

            // 有铸模但全部繁忙 - 等待
            this.setNextCheckTickCount(40);
            return false;
        }

        // 优先级4：如果物品仍在熔炼中，等待
        if (!SmelteryHelper.allItemsMelted(smeltery)) {
            maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.WAITING_MELT);
            showStateBubble(maid, SmelteryWorkState.WAITING_MELT);
            this.setNextCheckTickCount(40);
            return false;
        }

        // 优先级5：流体罐为空，无物品在熔炼 -> 插入新物品或添加燃料

        // 检查是否需要燃料并且有燃料
        if (SmelteryHelper.needsFuel(smeltery)) {
            if (SmelteryHelper.hasLavaItems(maid)) {
                BlockPos tankPos = SmelteryHelper.findFillableTank(level, smeltery);
                if (tankPos != null) {
                    return setActionTarget(maid, tankPos, SmelteryWorkState.FUELING);
                }
            }
            // 女仆没有岩浆，且燃料罐中也没有储备 -> 不投入物品，等待玩家补充燃料
            if (!SmelteryHelper.hasFuelReserve(level, smeltery)) {
                this.setNextCheckTickCount(100);
                return false;
            }
        }

        // 检查女仆是否有可熔炼物品（合金规避：仅在流体罐为空时插入）
        if (SmelteryHelper.isTankEmpty(smeltery) && SmelteryHelper.hasMeltableItems(maid)) {
            if (SmelteryHelper.hasEmptySlots(smeltery)) {
                return setActionTarget(maid, smelteryPos, SmelteryWorkState.INSERTING);
            }
        }

        // 无事可做
        if (!SmelteryHelper.hasMeltableItems(maid) && SmelteryHelper.isTankEmpty(smeltery)) {
            // 没有物品也没有流体 - 女仆无事可做
            this.setNextCheckTickCount(200);
            return false;
        }

        this.setNextCheckTickCount(60);
        return false;
    }

    private boolean handleWaitingMelt(ServerLevel level, EntityMaid maid, HeatingStructureBlockEntity smeltery) {
        if (SmelteryHelper.allItemsMelted(smeltery)) {
            maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.IDLE);
            this.setNextCheckTickCount(5);
            return false;
        }
        // 仍在熔炼中，稍后再检查
        this.setNextCheckTickCount(40);
        return false;
    }

    private boolean handleWaitingCast(ServerLevel level, EntityMaid maid, HeatingStructureBlockEntity smeltery) {
        // 检查是否有浇铸台的输出已准备好收集
        Pair<BlockPos, BlockPos> collectable = SmelteryHelper.findCollectableOutput(level, smeltery);
        if (collectable != null) {
            return setActionTarget(maid, collectable.getSecond(), SmelteryWorkState.COLLECTING);
        }

        // 检查是否仍在冷却/浇铸
        if (SmelteryHelper.isAnyFaucetPouring(level, smeltery) || SmelteryHelper.isAnyCastingCooling(level, smeltery)) {
            this.setNextCheckTickCount(20);
            return false;
        }

        // 已无冷却中的物品 - 检查流体罐是否还有流体可以继续浇铸
        if (!SmelteryHelper.isTankEmpty(smeltery)) {
            Pair<BlockPos, BlockPos> pouringTarget = SmelteryHelper.findBestPouringTarget(level, smeltery);
            if (pouringTarget != null) {
                return setActionTarget(maid, pouringTarget.getFirst(), SmelteryWorkState.POURING);
            }
            // 没有可用目标，等待当前目标完成
            this.setNextCheckTickCount(40);
            return false;
        }

        // 流体罐已空，浇铸完成，返回空闲状态
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.IDLE);
        this.setNextCheckTickCount(5);
        return false;
    }

    private boolean setActionTarget(EntityMaid maid, BlockPos targetPos, SmelteryWorkState newState) {
        BehaviorUtils.setWalkAndLookTargetMemories(maid, targetPos, speed, 1);
        maid.getBrain().setMemory(InitEntities.TARGET_POS.get(), new BlockPosTracker(targetPos));
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), newState);
        maid.getBrain().setMemory(InitMemories.ACTION_TARGET_POS.get(), targetPos);
        showStateBubble(maid, newState);
        this.setNextCheckTickCount(5);
        return true;
    }

    private void showStateBubble(EntityMaid maid, SmelteryWorkState state) {
        String[] pool = switch (state) {
            case FUELING -> FUELING_BUBBLES;
            case INSERTING -> INSERTING_BUBBLES;
            case POURING -> POURING_BUBBLES;
            case COLLECTING -> COLLECTING_BUBBLES;
            case WAITING_MELT -> WAITING_MELT_BUBBLES;
            case WAITING_CAST -> WAITING_CAST_BUBBLES;
            default -> null;
        };
        if (pool != null) {
            addBubbleIfNotTooMany(maid, randomBubble(maid, pool));
        }
    }
}
