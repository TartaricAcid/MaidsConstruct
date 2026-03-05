package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryExtraMaidBrain;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.SmelteryBubbles;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import java.util.Optional;

public class MaidSmelteryCollectTask extends Behavior<EntityMaid> {
    private static final double INTERACT_RANGE_SQ = 2.5 * 2.5;

    public MaidSmelteryCollectTask() {
        super(ImmutableMap.of(InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<SmelteryWorkState> state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get());
        if (state.isEmpty() || state.get() != SmelteryWorkState.COLLECTING) {
            return false;
        }
        return maid.getBrain().getMemory(InitEntities.TARGET_POS.get())
                .map(tracker -> maid.blockPosition().distSqr(tracker.currentBlockPosition()) < INTERACT_RANGE_SQ)
                .orElse(false);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        var blockPos = maid.getBrain()
                .getMemory(InitEntities.TARGET_POS.get())
                .map(PositionTracker::currentBlockPosition);

        blockPos.ifPresent(castingPos -> {
            BlockEntity be = level.getBlockEntity(castingPos);
            if (!(be instanceof CastingBlockEntity casting)) {
                return;
            }

            ItemStack outputCopy = casting.getItem(CastingBlockEntity.OUTPUT).copy();
            if (outputCopy.isEmpty()) {
                return;
            }

            IItemHandler maidInv = maid.getAvailableInv(false);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(maidInv, outputCopy, true);

            String successKey = SmelteryBubbles.randomBubble(maid, SmelteryBubbles.COLLECT_SUCCESS_BUBBLES);
            String fullKey = SmelteryBubbles.randomBubble(maid, SmelteryBubbles.INVENTORY_FULL_BUBBLES);

            // 可以放入全部输出物品
            if (remainder.isEmpty()) {
                ItemsUtil.giveItemToMaid(maid, outputCopy);
                casting.setItem(CastingBlockEntity.OUTPUT, ItemStack.EMPTY);

                maid.swing(InteractionHand.MAIN_HAND);
                SmelteryBubbles.addBubbleIfNotTooMany(maid, successKey);
                return;
            }

            // 可以部分放入
            if (remainder.getCount() < outputCopy.getCount()) {
                outputCopy.shrink(remainder.getCount());
                ItemsUtil.giveItemToMaid(maid, outputCopy);
                casting.setItem(CastingBlockEntity.OUTPUT, remainder);

                maid.swing(InteractionHand.MAIN_HAND);
                SmelteryBubbles.addBubbleIfNotTooMany(maid, successKey);
                return;
            }

            // 背包已满
            SmelteryBubbles.addBubbleIfNotTooMany(maid, fullKey);
        });

        // 返回空闲状态以进行下一步决策
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.IDLE);
        SmelteryExtraMaidBrain.clearActionMemories(maid);
    }
}
