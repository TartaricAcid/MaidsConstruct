package com.github.tartaricacid.maidsconstruct.util.recipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;

import javax.annotation.Nullable;

/**
 * 虚拟浇铸容器，用于查询浇铸配方是否存在。
 * 模拟一个包含铸模物品和流体的浇铸台/浇铸盆环境。
 */
public class VirtualCastingContainer implements ICastingContainer {
    private final ItemStack cast;
    private final FluidStack fluidStack;

    public VirtualCastingContainer(ItemStack cast, FluidStack fluidStack) {
        this.cast = cast;
        this.fluidStack = fluidStack;
    }

    @Override
    @NotNull
    public ItemStack getStack() {
        return cast;
    }

    @Override
    @NotNull
    public Fluid getFluid() {
        return fluidStack.getFluid();
    }

    @Nullable
    @Override
    public CompoundTag getFluidTag() {
        return this.fluidStack.getTag();
    }
}
