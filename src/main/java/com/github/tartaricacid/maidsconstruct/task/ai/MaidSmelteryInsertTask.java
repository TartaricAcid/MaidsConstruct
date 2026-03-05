package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.SmelteryHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

public class MaidSmelteryInsertTask extends MaidSmelteryActionTask {
    @Override
    protected SmelteryWorkState getRequiredState() {
        return SmelteryWorkState.INSERTING;
    }

    @Override
    protected SmelteryWorkState getNextState() {
        return SmelteryWorkState.WAITING_MELT;
    }

    @Override
    protected void performAction(ServerLevel level, EntityMaid maid) {
        maid.getBrain().getMemory(InitMemories.SMELTERY_POS.get()).ifPresent(smelteryPos -> {
            BlockEntity be = level.getBlockEntity(smelteryPos);
            if (be instanceof HeatingStructureBlockEntity smeltery) {
                smeltery.getCapability(ForgeCapabilities.ITEM_HANDLER)
                        .ifPresent(smelteryInv -> insertItems(maid, smeltery, smelteryInv, level));
            }
        });
    }

    private void insertItems(EntityMaid maid, HeatingStructureBlockEntity smeltery, IItemHandler smelteryInv, ServerLevel level) {
        IItemHandler maidInv = maid.getAvailableInv(true);

        boolean inserted = false;
        // 追踪下一个可熔炼槽位
        int nextSmelterySlot = 0;

        for (int maidSlot = 0; maidSlot < maidInv.getSlots(); maidSlot++) {
            ItemStack maidStack = maidInv.getStackInSlot(maidSlot);
            if (maidStack.isEmpty()) {
                continue;
            }

            // 检查此物品是否为允许投入冶炼炉的原料（含合金规避检查）
            if (!SmelteryHelper.isSmeltingInput(maidStack, smeltery)) {
                continue;
            }

            // 尽可能多地将此堆叠中的物品插入空的冶炼炉槽位
            while (!maidStack.isEmpty() && nextSmelterySlot < smelteryInv.getSlots()) {
                // 查找下一个空的冶炼炉槽位
                while (nextSmelterySlot < smelteryInv.getSlots() && !smelteryInv.getStackInSlot(nextSmelterySlot).isEmpty()) {
                    nextSmelterySlot++;
                }
                if (nextSmelterySlot >= smelteryInv.getSlots()) {
                    // 冶炼炉已满
                    break;
                }

                ItemStack toInsert = maidStack.copyWithCount(1);
                ItemStack remainder = smelteryInv.insertItem(nextSmelterySlot, toInsert, false);
                if (remainder.isEmpty()) {
                    maidStack.shrink(1);
                    inserted = true;
                    nextSmelterySlot++;
                } else {
                    // 槽位拒绝了该物品
                    // 理论上不应该发生，但是以防万一
                    break;
                }
            }

            // 如果冶炼炉已满，停止
            if (nextSmelterySlot >= smelteryInv.getSlots()) {
                break;
            }
        }

        if (inserted) {
            maid.swing(InteractionHand.MAIN_HAND);
        }
    }
}
