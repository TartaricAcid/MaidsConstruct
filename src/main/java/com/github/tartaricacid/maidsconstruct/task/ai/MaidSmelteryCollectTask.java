package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.SmelteryBubbles;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

public class MaidSmelteryCollectTask extends MaidSmelteryActionTask {
    @Override
    protected SmelteryWorkState getRequiredState() {
        return SmelteryWorkState.COLLECTING;
    }

    @Override
    protected SmelteryWorkState getNextState() {
        return SmelteryWorkState.IDLE;
    }

    @Override
    protected void performAction(ServerLevel level, EntityMaid maid) {
        maid.getBrain().getMemory(InitEntities.TARGET_POS.get())
                .map(PositionTracker::currentBlockPosition)
                .ifPresent(castingPos -> collectFromCasting(level, maid, castingPos));
    }

    private void collectFromCasting(ServerLevel level, EntityMaid maid, net.minecraft.core.BlockPos castingPos) {
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
    }
}
