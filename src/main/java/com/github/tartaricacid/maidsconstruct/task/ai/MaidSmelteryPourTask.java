package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryExtraMaidBrain;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.CastingHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

import java.util.List;
import java.util.Optional;

public class MaidSmelteryPourTask extends Behavior<EntityMaid> {
    private static final double INTERACT_RANGE_SQ = 2.5 * 2.5;

    public MaidSmelteryPourTask() {
        super(ImmutableMap.of(InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<SmelteryWorkState> state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get());
        if (state.isEmpty() || state.get() != SmelteryWorkState.POURING) {
            return false;
        }
        return maid.getBrain().getMemory(InitEntities.TARGET_POS.get())
                .map(tracker -> maid.blockPosition().distSqr(tracker.currentBlockPosition()) < INTERACT_RANGE_SQ)
                .orElse(false);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getBrain().getMemory(InitMemories.SMELTERY_POS.get()).ifPresent(smelteryPos -> {
            BlockEntity be = level.getBlockEntity(smelteryPos);
            if (!(be instanceof HeatingStructureBlockEntity smeltery)) {
                return;
            }

            List<Pair<BlockPos, BlockPos>> targets = CastingHelper.findAllPouringTargets(level, smeltery);
            if (targets.isEmpty()) {
                return;
            }

            // 激活连接到此冶炼炉的权重最高的浇筑口
            Pair<BlockPos, BlockPos> target = targets.get(0);
            BlockEntity faucetBe = level.getBlockEntity(target.getFirst());
            if (faucetBe instanceof FaucetBlockEntity faucet && !faucet.isPouring()) {
                faucet.activate();
                maid.swing(InteractionHand.MAIN_HAND);
            }
        });

        // 转换到等待浇铸状态
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.WAITING_CAST);
        SmelteryExtraMaidBrain.clearActionMemories(maid);
    }
}
