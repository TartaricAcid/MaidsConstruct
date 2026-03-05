package com.github.tartaricacid.maidsconstruct.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeLookup;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

import java.util.List;

import static net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM;

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
        SmelteryTank<HeatingStructureBlockEntity> tank = smeltery.getTank();
        return tank.getFluids().isEmpty() || tank.getContained() == 0;
    }

    /**
     * 检查冶炼炉是否需要燃料。
     */
    public static boolean needsFuel(HeatingStructureBlockEntity smeltery) {
        return !smeltery.getFuelModule().hasFuel();
    }

    /**
     * 检查女仆背包中是否有允许投入冶炼炉的物品。
     * 仅限 forge:ores 和 forge:raw_materials 标签的物品，防止将成品（锭/块等）放回冶炼炉。
     */
    public static boolean hasMeltableItems(EntityMaid maid, HeatingStructureBlockEntity smeltery) {
        IItemHandler inv = maid.getAvailableInv(false);
        for (int i = 0; i < inv.getSlots(); i++) {
            if (isSmeltingInput(inv.getStackInSlot(i), smeltery)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查物品是否为允许投入冶炼炉的原料。
     * 仅限 forge:ores 和 forge:raw_materials 标签的物品，且熔炼后产生的流体不会与冶炼炉中已有流体形成合金。
     */
    public static boolean isSmeltingInput(ItemStack stack, HeatingStructureBlockEntity smeltery) {
        if (stack.isEmpty()) {
            return false;
        }
        // 属于 forge:ores 或 forge:raw_materials 标签
        if (!stack.is(Items.ORES) && !stack.is(Items.RAW_MATERIALS)) {
            return false;
        }
        // 查找熔炼配方
        MeltingRecipeLookup.MeltingFluid meltingFluid = MeltingRecipeLookup.findFluid(stack.getItem());
        if (meltingFluid.isEmpty()) {
            return false;
        }
        // 检查温度是否符合熔炼条件
        int smelteryTemperature = smeltery.getFuelModule().getTemperature();
        if (meltingFluid.temperature() > smelteryTemperature) {
            return false;
        }
        // 如果冶炼炉流体罐为空，任何可熔炼物品都可以投入
        if (isTankEmpty(smeltery)) {
            return true;
        }
        // 只有冶炼炉会生成合金，故需要做合金判断
        if (smeltery instanceof SmelteryBlockEntity smelteryBlockEntity) {
            // 冶炼炉中已有流体，检查投入后是否会形成合金
            FluidStack outputFluid = meltingFluid.result().get();
            return !hasAlloyResult(smelteryBlockEntity, outputFluid);
        }
        // 其他类型的加热结构不考虑合金配方，直接允许投入
        return true;
    }

    /**
     * 检查将新流体加入冶炼炉后是否会触发合金配方。
     * 构建一个包含现有流体和新流体的虚拟罐，检查所有合金配方是否匹配。
     */
    private static boolean hasAlloyResult(SmelteryBlockEntity smeltery, FluidStack newFluid) {
        Level level = smeltery.getLevel();
        if (level == null) {
            return false;
        }
        // 构建虚拟罐：现有流体 + 新流体
        VirtualAlloyTank virtualTank = new VirtualAlloyTank(smeltery);
        virtualTank.addFluid(newFluid);
        // 能找到合金配方
        return !level.getRecipeManager()
                .getRecipesFor(TinkerRecipeTypes.ALLOYING.get(), virtualTank, level)
                .isEmpty();
    }

    /**
     * 获取冶炼炉流体罐中单种流体的最大量。
     * 冶炼炉只能输出底部流体，所以评分时应以最大单种流体量为准。
     */
    public static int getMaxFluidAmount(SmelteryTank<?> tank) {
        int max = 0;
        for (FluidStack fluid : tank.getFluids()) {
            if (fluid.getAmount() > max) {
                max = fluid.getAmount();
            }
        }
        return max;
    }

    /**
     * 将冶炼炉中流体量最多的流体移动到底部（使其成为浇注口的输出流体）。
     * 如果最大流体已经在底部，不做任何操作。
     */
    public static void ensureLargestFluidAtBottom(SmelteryTank<?> tank) {
        List<FluidStack> fluids = tank.getFluids();
        if (fluids.size() <= 1) {
            return;
        }
        int maxIndex = 0;
        int maxAmount = 0;
        for (int i = 0; i < fluids.size(); i++) {
            if (fluids.get(i).getAmount() > maxAmount) {
                maxAmount = fluids.get(i).getAmount();
                maxIndex = i;
            }
        }
        if (maxIndex != 0) {
            tank.moveFluidToBottom(maxIndex);
        }
    }

    /**
     * 检查女仆背包中是否有含有燃料物品（通过 Forge 流体能力检测，兼容模组桶）。
     */
    public static boolean hasFuelItems(EntityMaid maid) {
        IItemHandler inv = maid.getAvailableInv(false);
        for (int i = 0; i < inv.getSlots(); i++) {
            if (containsFuel(inv.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查物品是否含有燃料（通过 FLUID_HANDLER_ITEM 能力）。
     * <p>
     * FIXME：目前仅检测岩浆，但是实际上燃料还可能是烈焰血
     */
    public static boolean containsFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getCapability(FLUID_HANDLER_ITEM).map(handler -> {
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluid = handler.getFluidInTank(i);
                if (!fluid.isEmpty() && fluid.getFluid().isSame(Fluids.LAVA)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }
}
