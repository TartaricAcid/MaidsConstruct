package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryExtraMaidBrain;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.SmelteryHelper;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.library.fluid.EmptyFluidHandlerItem;

import java.util.Optional;

public class MaidSmelteryFuelTask extends Behavior<EntityMaid> {
    private static final double INTERACT_RANGE_SQ = 2.5 * 2.5;

    public MaidSmelteryFuelTask() {
        super(ImmutableMap.of(InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<SmelteryWorkState> state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get());
        if (state.isEmpty() || state.get() != SmelteryWorkState.FUELING) {
            return false;
        }
        return maid.getBrain().getMemory(InitEntities.TARGET_POS.get())
                .map(tracker -> maid.blockPosition().distSqr(tracker.currentBlockPosition()) < INTERACT_RANGE_SQ)
                .orElse(false);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getBrain().getMemory(InitEntities.TARGET_POS.get())
                .map(PositionTracker::currentBlockPosition)
                .ifPresent(tankPos -> {
                    BlockEntity be = level.getBlockEntity(tankPos);
                    if (be != null) {
                        be.getCapability(ForgeCapabilities.FLUID_HANDLER)
                                .ifPresent(tankHandler -> fillFuelFromInventory(maid, tankHandler));
                    }
                });

        // 返回空闲状态以进行下一步决策
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), SmelteryWorkState.IDLE);
        SmelteryExtraMaidBrain.clearActionMemories(maid);
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
            IFluidHandlerItem itemHandler = extracted
                    .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                    .orElse(EmptyFluidHandlerItem.INSTANCE);

            // 如果物品没有流体（理论上不应该发生，因为之前已经检查过），放回原物品并继续
            if (itemHandler == EmptyFluidHandlerItem.INSTANCE) {
                // 放回原物品
                ItemStack remaining = inv.insertItem(i, extracted, false);
                if (!remaining.isEmpty()) {
                    // 如果放回失败（理论上不应该发生），尝试生成在脚下
                    maid.spawnAtLocation(remaining);
                }
                continue;
            }

            // 从物品容器中抽取岩浆
            FluidStack drained = itemHandler.drain(
                    new FluidStack(Fluids.LAVA, Integer.MAX_VALUE),
                    FluidAction.SIMULATE
            );

            // 如果抽取失败（理论上不应该发生），正常返还
            if (drained.isEmpty()) {
                ItemStack remaining = inv.insertItem(i, extracted, false);
                if (!remaining.isEmpty()) {
                    // 如果放回失败（理论上不应该发生），尝试生成在脚下
                    maid.spawnAtLocation(remaining);
                }
                continue;
            }

            // 模拟填入燃料罐，获取能够放入的数量
            int accepted = tankHandler.fill(drained, FluidAction.SIMULATE);
            // 如果放入失败（一般不会发生），正常返还
            if (accepted <= 0) {
                ItemStack remaining = inv.insertItem(i, extracted, false);
                if (!remaining.isEmpty()) {
                    // 如果放回失败（理论上不应该发生），尝试生成在脚下
                    maid.spawnAtLocation(remaining);
                }
                continue;
            }

            // 实际执行：从物品抽取 -> 填入燃料罐
            FluidStack actualDrained = itemHandler.drain(
                    new FluidStack(Fluids.LAVA, accepted),
                    FluidAction.EXECUTE
            );

            tankHandler.fill(actualDrained, FluidAction.EXECUTE);

            // 将操作后的容器物品放回背包（例如空桶）
            ItemStack remaining = inv.insertItem(i, itemHandler.getContainer(), false);
            if (!remaining.isEmpty()) {
                // 如果放回失败（比如是可堆叠的储罐类物品），尝试放入其他位置
                ItemsUtil.giveItemToMaid(maid, remaining);
            }

            maid.swing(InteractionHand.MAIN_HAND);
            return;
        }
    }
}
