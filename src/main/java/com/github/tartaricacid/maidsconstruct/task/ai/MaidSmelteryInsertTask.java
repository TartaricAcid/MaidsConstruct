package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryExtraMaidBrain;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.SmelteryHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

import javax.annotation.Nullable;
import java.util.Optional;

public class MaidSmelteryInsertTask extends Behavior<EntityMaid> {
    private static final double INTERACT_RANGE_SQ = 2.5 * 2.5;

    public MaidSmelteryInsertTask() {
        super(ImmutableMap.of(InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<SmelteryWorkState> state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get());
        if (state.isEmpty() || state.get() != SmelteryWorkState.INSERTING) {
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
            if (be instanceof HeatingStructureBlockEntity smeltery) {
                smeltery.getCapability(ForgeCapabilities.ITEM_HANDLER)
                        .ifPresent(smelteryInv -> insertItems(maid, smeltery, smelteryInv, level));
            }
        });

        // 转换到等待熔炼状态
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.WAITING_MELT);
        SmelteryExtraMaidBrain.clearActionMemories(maid);
    }

    private void insertItems(EntityMaid maid, HeatingStructureBlockEntity smeltery, IItemHandler smelteryInv, ServerLevel level) {
        IItemHandler maidInv = maid.getAvailableInv(true);

        // 确定目标流体类型（合金规避）
        FluidStack targetFluid = determineTargetFluid(smeltery, maidInv);
        if (targetFluid == null) {
            return;
        }

        boolean inserted = false;
        // 追踪下一个可熔炼槽位
        int nextSmelterySlot = 0;

        for (int maidSlot = 0; maidSlot < maidInv.getSlots(); maidSlot++) {
            ItemStack maidStack = maidInv.getStackInSlot(maidSlot);
            if (maidStack.isEmpty()) {
                continue;
            }

            // 检查此物品熔炼后是否产生目标流体类型
            // FIXME：还有个问题，部分熔炼的材料是需要考虑温度的，温度不足不能冶炼
            if (!SmelteryHelper.isMeltableToFluid(maidStack, targetFluid, level)) {
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

    /**
     * 确定用于合金规避的目标流体类型。
     * 如果流体罐中有流体，只插入产生相同流体的物品。
     * 如果流体罐为空，选择第一个可熔炼物品的流体类型。
     */
    @Nullable
    private FluidStack determineTargetFluid(HeatingStructureBlockEntity smeltery, IItemHandler maidInv) {
        // 检查流体罐是否已有流体
        if (!SmelteryHelper.isTankEmpty(smeltery)) {
            // 使用流体罐中的第一种流体类型作为目标
            // FIXME: 这里假设冶炼炉中只有一种流体，实际可能存在多种流体共存的情况，依然会可能生成合金
            var fluids = smeltery.getTank().getFluids();
            if (!fluids.isEmpty()) {
                return fluids.get(0);
            }
        }

        // 流体罐为空 - 选择第一个允许投入的原料的输出流体类型
        for (int i = 0; i < maidInv.getSlots(); i++) {
            ItemStack stack = maidInv.getStackInSlot(i);
            if (!SmelteryHelper.isSmeltingInput(stack)) {
                continue;
            }
            FluidStack output = SmelteryHelper.getFluidOutput(stack);
            if (!output.isEmpty()) {
                return output;
            }
        }

        return null;
    }
}
