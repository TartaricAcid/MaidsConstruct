package com.github.tartaricacid.maidsconstruct.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeLookup;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;

import javax.annotation.Nullable;
import java.util.List;

public class SmelteryHelper {

    /**
     * 检查冶炼炉是否有空的熔炼槽位。
     */
    public static boolean hasEmptySlots(HeatingStructureBlockEntity smeltery) {
        IItemHandler inv = smeltery.getMeltingInventory();
        for (int i = 0; i < inv.getSlots(); i++) {
            if (inv.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查冶炼炉中的所有物品是否已熔化完毕（无剩余物品）。
     */
    public static boolean allItemsMelted(HeatingStructureBlockEntity smeltery) {
        IItemHandler inv = smeltery.getMeltingInventory();
        for (int i = 0; i < inv.getSlots(); i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查冶炼炉的流体罐是否为空。
     */
    public static boolean isTankEmpty(HeatingStructureBlockEntity smeltery) {
        return smeltery.getTank().getFluids().isEmpty() || smeltery.getTank().getContained() == 0;
    }

    /**
     * 检查冶炼炉是否需要燃料。
     */
    public static boolean needsFuel(HeatingStructureBlockEntity smeltery) {
        return !smeltery.getFuelModule().hasFuel();
    }

    /**
     * 检查冶炼炉的燃料罐中是否还有岩浆储备。
     * 即使 needsFuel 返回 true（当前无活跃燃料），燃料罐中仍可能有岩浆可供自动消耗。
     */
    public static boolean hasFuelReserve(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> tanks = findTankBlocks(smeltery);
        for (BlockPos tankPos : tanks) {
            BlockEntity be = level.getBlockEntity(tankPos);
            if (be != null) {
                boolean hasLava = be.getCapability(ForgeCapabilities.FLUID_HANDLER).map(handler -> {
                    for (int i = 0; i < handler.getTanks(); i++) {
                        FluidStack fluid = handler.getFluidInTank(i);
                        if (!fluid.isEmpty() && fluid.getFluid().isSame(net.minecraft.world.level.material.Fluids.LAVA)) {
                            return true;
                        }
                    }
                    return false;
                }).orElse(false);
                if (hasLava) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查女仆背包中是否有允许投入冶炼炉的物品。
     * 仅限 forge:ores 和 forge:raw_materials 标签的物品，防止将成品（锭/块等）放回冶炼炉。
     */
    public static boolean hasMeltableItems(EntityMaid maid) {
        IItemHandler inv = maid.getAvailableInv(false);
        for (int i = 0; i < inv.getSlots(); i++) {
            if (isSmeltingInput(inv.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查物品是否为允许投入冶炼炉的原料。
     * 必须同时满足：属于 forge:ores 或 forge:raw_materials 标签，且有对应的熔炼配方。
     */
    public static boolean isSmeltingInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!stack.is(Tags.Items.ORES) && !stack.is(Tags.Items.RAW_MATERIALS)) {
            return false;
        }
        return MeltingRecipeLookup.canMelt(stack.getItem());
    }

    /**
     * 检查女仆背包中是否有含有岩浆的容器物品（通过 Forge 流体能力检测，兼容模组桶）。
     */
    public static boolean hasLavaItems(EntityMaid maid) {
        IItemHandler inv = maid.getAvailableInv(false);
        for (int i = 0; i < inv.getSlots(); i++) {
            if (containsLava(inv.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查物品是否含有岩浆（通过 FLUID_HANDLER_ITEM 能力）。
     */
    public static boolean containsLava(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluid = handler.getFluidInTank(i);
                if (!fluid.isEmpty() && fluid.getFluid().isSame(Fluids.LAVA)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    /**
     * 获取物品熔炼后产生的流体。
     * 如果物品不可熔炼，返回 FluidStack.EMPTY。
     */
    public static FluidStack getFluidOutput(ItemStack stack) {
        MeltingRecipeLookup.MeltingFluid fluid = MeltingRecipeLookup.findFluid(stack.getItem());
        if (fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return fluid.result().get();
    }

    /**
     * 检查物品是否为允许投入的原料，且熔炼后产生的流体与目标流体兼容（相同流体类型）。
     * 如果目标流体为空，则任何允许投入的原料均可接受。
     */
    public static boolean isMeltableToFluid(ItemStack stack, FluidStack targetFluid, Level level) {
        if (!isSmeltingInput(stack)) {
            return false;
        }
        FluidStack output = getFluidOutput(stack);
        if (output.isEmpty()) {
            return false;
        }
        if (targetFluid.isEmpty()) {
            return true;
        }
        return output.getFluid().isSame(targetFluid.getFluid());
    }

    /**
     * 查找冶炼炉结构中的燃料罐方块。
     */
    public static List<BlockPos> findTankBlocks(HeatingStructureBlockEntity smeltery) {
        StructureData structure = smeltery.getStructure();
        if (structure == null) {
            return List.of();
        }
        return structure.getTanks();
    }

    /**
     * 查找可以接受更多岩浆的燃料罐。
     */
    @Nullable
    public static BlockPos findFillableTank(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> tanks = findTankBlocks(smeltery);
        for (BlockPos tankPos : tanks) {
            BlockEntity be = level.getBlockEntity(tankPos);
            if (be != null) {
                boolean canFill = be.getCapability(ForgeCapabilities.FLUID_HANDLER).map(handler -> {
                    FluidStack lava = new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 1000);
                    return handler.fill(lava, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) > 0;
                }).orElse(false);
                if (canFill) {
                    return tankPos;
                }
            }
        }
        return null;
    }
}
