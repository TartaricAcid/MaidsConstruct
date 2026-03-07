package com.github.tartaricacid.maidsconstruct.util.recipe;

import com.github.tartaricacid.maidsconstruct.util.smeltery.SmelteryHelper;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.recipe.alloying.IAlloyTank;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仅用于合金配方查询的虚拟合金罐
 */
public class VirtualAlloyTank implements IAlloyTank {
    private final SmelteryBlockEntity smeltery;
    private final List<FluidStack> fluids;

    public VirtualAlloyTank(SmelteryBlockEntity smeltery) {
        this.smeltery = smeltery;
        this.fluids = smeltery.getTank()
                .getFluids().stream()
                .map(FluidStack::copy)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void addFluid(FluidStack fluid) {
        fluids.add(fluid.copy());
    }

    @Override
    public int getTemperature() {
        return SmelteryHelper.getFuelTemperature(smeltery);
    }

    @Override
    public int getTanks() {
        return fluids.size();
    }

    @Override
    @NotNull
    public FluidStack getFluidInTank(int tank) {
        return 0 <= tank && tank < fluids.size() ? fluids.get(tank) : FluidStack.EMPTY;
    }

    @Override
    public boolean canFit(@NotNull FluidStack fluid, int removed) {
        return true;
    }
}
