package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.SmelteryHelper;
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
    private static final double INTERACT_RANGE_SQ = 6.25;

    public MaidSmelteryPourTask() {
        super(ImmutableMap.of(
                InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<SmelteryWorkState> state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get());
        if (state.isEmpty() || state.get() != SmelteryWorkState.POURING) {
            return false;
        }
        return maid.getBrain().getMemory(InitMemories.ACTION_TARGET_POS.get())
                .map(pos -> maid.blockPosition().distSqr(pos) < INTERACT_RANGE_SQ)
                .orElse(false);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        // 激活连接到此冶炼炉的所有可用浇口，而不是只激活一个
        maid.getBrain().getMemory(InitMemories.SMELTERY_POS.get()).ifPresent(smelteryPos -> {
            BlockEntity be = level.getBlockEntity(smelteryPos);
            if (be instanceof HeatingStructureBlockEntity smeltery) {
                List<Pair<BlockPos, BlockPos>> targets = SmelteryHelper.findAllPouringTargets(level, smeltery);
                boolean activated = false;
                for (Pair<BlockPos, BlockPos> target : targets) {
                    BlockEntity faucetBe = level.getBlockEntity(target.getFirst());
                    if (faucetBe instanceof FaucetBlockEntity faucet && !faucet.isPouring()) {
                        faucet.activate();
                        activated = true;
                    }
                }
                if (activated) {
                    maid.swing(InteractionHand.MAIN_HAND);
                }
            }
        });

        // 转换到等待浇铸状态
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.WAITING_CAST);
        MaidSmelterySearchTask.clearActionMemories(maid);
    }
}
