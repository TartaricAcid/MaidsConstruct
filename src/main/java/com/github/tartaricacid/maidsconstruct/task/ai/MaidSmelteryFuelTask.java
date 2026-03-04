package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;

import java.util.Optional;

public class MaidSmelteryFuelTask extends Behavior<EntityMaid> {
    private static final double INTERACT_RANGE_SQ = 6.25; // 2.5 格

    public MaidSmelteryFuelTask() {
        super(ImmutableMap.of(
                InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<SmelteryWorkState> state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get());
        if (state.isEmpty() || state.get() != SmelteryWorkState.FUELING) {
            return false;
        }
        return maid.getBrain().getMemory(InitMemories.ACTION_TARGET_POS.get())
                .map(pos -> maid.blockPosition().distSqr(pos) < INTERACT_RANGE_SQ)
                .orElse(false);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getBrain().getMemory(InitMemories.ACTION_TARGET_POS.get()).ifPresent(tankPos -> {
            BlockEntity be = level.getBlockEntity(tankPos);
            if (be != null) {
                be.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(tankHandler -> {
                    fillFuelFromInventory(maid, tankHandler);
                });
            }
        });

        // 返回空闲状态以进行下一步决策
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.IDLE);
        MaidSmelterySearchTask.clearActionMemories(maid);
    }

    /**
     * 从女仆背包中查找含有岩浆的容器物品，通过 Forge 流体能力将岩浆转移到燃料罐。
     * 兼容所有实现了 FLUID_HANDLER_ITEM 能力的模组容器。
     */
    private void fillFuelFromInventory(EntityMaid maid, IFluidHandler tankHandler) {
        IItemHandler inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!SmelteryHelper.containsLava(stack)) {
                continue;
            }

            // 提取物品的副本用于流体操作（流体操作会修改 ItemStack）
            ItemStack extracted = inv.extractItem(i, 1, false);
            IFluidHandlerItem itemHandler = extracted.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (itemHandler == null) {
                // 放回原物品
                inv.insertItem(i, extracted, false);
                continue;
            }

            // 从物品容器中抽取岩浆
            FluidStack drained = itemHandler.drain(
                    new FluidStack(Fluids.LAVA, Integer.MAX_VALUE),
                    IFluidHandler.FluidAction.SIMULATE
            );
            if (drained.isEmpty()) {
                inv.insertItem(i, extracted, false);
                continue;
            }

            // 尝试填入燃料罐
            int accepted = tankHandler.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                inv.insertItem(i, extracted, false);
                continue;
            }

            // 实际执行：从物品抽取 -> 填入燃料罐
            FluidStack actualDrained = itemHandler.drain(
                    new FluidStack(Fluids.LAVA, accepted),
                    IFluidHandler.FluidAction.EXECUTE
            );
            tankHandler.fill(actualDrained, IFluidHandler.FluidAction.EXECUTE);

            // 将操作后的容器物品放回背包（例如空桶）
            ItemStack container = itemHandler.getContainer();
            inv.insertItem(i, container, false);
            maid.swing(InteractionHand.MAIN_HAND);
            return;
        }
    }
}
