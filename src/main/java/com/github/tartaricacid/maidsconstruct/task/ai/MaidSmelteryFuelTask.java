package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.config.MaidsConstructConfig;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.util.smeltery.SmelteryHelper;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.library.fluid.EmptyFluidHandlerItem;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;

public class MaidSmelteryFuelTask extends MaidSmelteryActionTask {
    @Override
    protected SmelteryWorkState getRequiredState() {
        return SmelteryWorkState.FUELING;
    }

    @Override
    protected SmelteryWorkState getNextState() {
        return SmelteryWorkState.IDLE;
    }

    @Override
    protected void performAction(ServerLevel level, EntityMaid maid) {
        maid.getBrain().getMemory(InitEntities.TARGET_POS.get())
                .map(PositionTracker::currentBlockPosition)
                .ifPresent(tankPos -> {
                    BlockEntity be = level.getBlockEntity(tankPos);
                    if (be instanceof HeatingStructureBlockEntity smeltery && smeltery.getStructure() != null) {
                        MultitankFuelModule fuelModule = smeltery.getFuelModule();
                        this.fillFuelFromInventory(maid, fuelModule);
                    }
                });
    }

    /**
     * 从女仆背包中查找含有燃料流体的容器物品，通过 Forge 流体能力将燃料转移到燃料罐。
     * 仅使用配置中允许的燃料流体（默认为熔岩和烈焰血）。
     * 兼容所有实现了 FLUID_HANDLER_ITEM 能力的模组容器。
     */
    private void fillFuelFromInventory(EntityMaid maid, MultitankFuelModule fuelModule) {
        IItemHandler inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!SmelteryHelper.containsFuel(stack)) {
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

            // 从物品容器中抽取燃料流体
            FluidStack drained = this.drainFuel(itemHandler);

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
            int accepted = fuelModule.fill(drained, FluidAction.SIMULATE);
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
                    new FluidStack(drained, accepted),
                    FluidAction.EXECUTE
            );

            fuelModule.fill(actualDrained, FluidAction.EXECUTE);

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

    /**
     * 从物品流体容器中模拟抽取燃料流体。
     * 按配置中允许的流体进行查找。
     */
    private FluidStack drainFuel(IFluidHandlerItem handler) {
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack fluid = handler.getFluidInTank(i);
            if (!fluid.isEmpty() && MaidsConstructConfig.isAllowedFuel(fluid.getFluid())) {
                return handler.drain(new FluidStack(fluid, Integer.MAX_VALUE), FluidAction.SIMULATE);
            }
        }
        return FluidStack.EMPTY;
    }
}
