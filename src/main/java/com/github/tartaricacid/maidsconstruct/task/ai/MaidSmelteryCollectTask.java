package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import java.util.Optional;

public class MaidSmelteryCollectTask extends Behavior<EntityMaid> {
    private static final double INTERACT_RANGE_SQ = 6.25;

    public MaidSmelteryCollectTask() {
        super(ImmutableMap.of(
                InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<SmelteryWorkState> state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get());
        if (state.isEmpty() || state.get() != SmelteryWorkState.COLLECTING) {
            return false;
        }
        return maid.getBrain().getMemory(InitMemories.ACTION_TARGET_POS.get())
                .map(pos -> maid.blockPosition().distSqr(pos) < INTERACT_RANGE_SQ)
                .orElse(false);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getBrain().getMemory(InitMemories.ACTION_TARGET_POS.get()).ifPresent(castingPos -> {
            BlockEntity be = level.getBlockEntity(castingPos);
            if (be instanceof CastingBlockEntity casting) {
                ItemStack output = casting.getItem(CastingBlockEntity.OUTPUT);
                if (!output.isEmpty()) {
                    IItemHandler maidInv = maid.getAvailableInv(false);
                    ItemStack remainder = ItemHandlerHelper.insertItemStacked(maidInv, output.copy(), true);
                    if (remainder.isEmpty()) {
                        // 可以放入全部输出物品
                        ItemHandlerHelper.insertItemStacked(maidInv, output.copy(), false);
                        casting.setItem(CastingBlockEntity.OUTPUT, ItemStack.EMPTY);
                        maid.swing(InteractionHand.MAIN_HAND);
                        MaidSmelterySearchTask.addBubbleIfNotTooMany(maid,
                                MaidSmelterySearchTask.randomBubble(maid, MaidSmelterySearchTask.COLLECT_SUCCESS_BUBBLES));
                    } else if (remainder.getCount() < output.getCount()) {
                        // 可以部分放入
                        ItemStack toInsert = output.copy();
                        toInsert.shrink(remainder.getCount());
                        ItemHandlerHelper.insertItemStacked(maidInv, toInsert, false);
                        casting.setItem(CastingBlockEntity.OUTPUT, remainder);
                        maid.swing(InteractionHand.MAIN_HAND);
                        MaidSmelterySearchTask.addBubbleIfNotTooMany(maid,
                                MaidSmelterySearchTask.randomBubble(maid, MaidSmelterySearchTask.COLLECT_SUCCESS_BUBBLES));
                    } else {
                        // 背包已满
                        MaidSmelterySearchTask.addBubbleIfNotTooMany(maid,
                                MaidSmelterySearchTask.randomBubble(maid, MaidSmelterySearchTask.INVENTORY_FULL_BUBBLES));
                    }
                }
            }
        });

        // 返回空闲状态以进行下一步决策
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.IDLE);
        MaidSmelterySearchTask.clearActionMemories(maid);
    }
}
