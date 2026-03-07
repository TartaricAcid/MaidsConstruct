package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.smeltery.CastingHelper;
import com.github.tartaricacid.maidsconstruct.util.smeltery.SmelteryHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

import java.util.List;

public class MaidSmelteryPourTask extends MaidSmelteryActionTask {
    @Override
    protected SmelteryWorkState getRequiredState() {
        return SmelteryWorkState.POURING;
    }

    @Override
    protected SmelteryWorkState getNextState() {
        return SmelteryWorkState.WAITING_CAST;
    }

    @Override
    protected void performAction(ServerLevel level, EntityMaid maid) {
        maid.getBrain().getMemory(InitMemories.SMELTERY_POS.get()).ifPresent(smelteryPos -> {
            BlockEntity be = level.getBlockEntity(smelteryPos);
            if (!(be instanceof HeatingStructureBlockEntity smeltery)) {
                return;
            }

            List<Pair<BlockPos, BlockPos>> targets = CastingHelper.findAllPouringTargets(level, smeltery);
            if (targets.isEmpty()) {
                return;
            }

            // 确保流体量最多的流体在底部（浇注口只输出底部流体）
            SmelteryHelper.ensureLargestFluidAtBottom(smeltery.getTank());

            // 激活连接到此冶炼炉的权重最高的浇筑口
            Pair<BlockPos, BlockPos> target = targets.get(0);
            BlockEntity faucetBe = level.getBlockEntity(target.getFirst());
            if (faucetBe instanceof FaucetBlockEntity faucet && !faucet.isPouring()) {
                faucet.activate();
                maid.swing(InteractionHand.MAIN_HAND);
            }
        });
    }
}
