package com.github.tartaricacid.maidsconstruct.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeLookup;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 检查冶炼炉中的可熔化物品是否已全部熔化完毕。
     * 忽略不可熔化的物品（无配方或温度不够）。
     */
    public static boolean allMeltableItemsMelted(HeatingStructureBlockEntity smeltery) {
        IItemHandler inv = smeltery.getMeltingInventory();
        int temperature = SmelteryHelper.getFuelTemperature(smeltery);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && canBeMelted(stack, temperature)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查冶炼炉中是否存在不可熔化的物品（无熔炼配方或温度不够）。
     */
    public static boolean hasNonMeltableItems(HeatingStructureBlockEntity smeltery) {
        IItemHandler inv = smeltery.getMeltingInventory();
        int temperature = SmelteryHelper.getFuelTemperature(smeltery);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && !canBeMelted(stack, temperature)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查物品是否可以在给定温度下被熔化。
     */
    public static boolean canBeMelted(ItemStack stack, int temperature) {
        FluidStack result = MeltingRecipeLookup.findResult(stack.getItem(), temperature);
        return !result.isEmpty();
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
        FuelModule.FuelInfo fuelInfo = smeltery.getFuelModule().getFuelInfo();
        return fuelInfo.isEmpty();
    }

    /**
     * 获取燃料温度
     */
    public static int getFuelTemperature(HeatingStructureBlockEntity smeltery) {
        FuelModule.FuelInfo fuelInfo = smeltery.getFuelModule().getFuelInfo();
        return fuelInfo.getTemperature();
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
        // 检查温度是否符合熔炼条件
        int temperature = SmelteryHelper.getFuelTemperature(smeltery);
        FluidStack meltingFluid = MeltingRecipeLookup.findResult(stack.getItem(), temperature);
        if (meltingFluid.isEmpty()) {
            return false;
        }
        // 只有冶炼炉会生成合金，故需要做合金判断
        // 合金检查需要同时考虑：罐中已有流体 + 正在熔化的物品将产生的流体 + 新物品的流体
        if (smeltery instanceof SmelteryBlockEntity smelteryBlockEntity) {
            return !hasAlloyResult(smelteryBlockEntity, temperature, meltingFluid);
        }
        // 其他类型的加热结构不考虑合金配方，直接允许投入
        return true;
    }

    /**
     * 检查将新流体加入冶炼炉后是否会引入新的合金配方。
     * 通过对比"加入前"和"加入后"的合金配方集合来判断——
     * 如果合金已经不可避免（现有流体+正在熔化的物品已经会触发合金），则允许继续投入同类材料。
     */
    private static boolean hasAlloyResult(SmelteryBlockEntity smeltery, int temperature, FluidStack newFluid) {
        Level level = smeltery.getLevel();
        if (level == null) {
            return false;
        }

        // 第一步：构建基础虚拟罐（现有流体 + 正在熔化物品将产生的流体），检查已有的合金配方
        VirtualAlloyTank baseTank = new VirtualAlloyTank(smeltery);
        addMeltingItemFluids(smeltery, baseTank, temperature);

        Set<ResourceLocation> existingAlloys = level.getRecipeManager()
                .getRecipesFor(TinkerRecipeTypes.ALLOYING.get(), baseTank, level)
                .stream()
                .map(Recipe::getId)
                .collect(Collectors.toSet());

        // 第二步：在基础上加入新流体，检查是否出现了新的合金配方
        baseTank.addFluid(newFluid);

        // 只有当加入新流体后出现了之前不存在的合金配方时，才拒绝
        return level.getRecipeManager()
                .getRecipesFor(TinkerRecipeTypes.ALLOYING.get(), baseTank, level)
                .stream()
                .anyMatch(recipe -> !existingAlloys.contains(recipe.getId()));
    }

    /**
     * 将冶炼炉中正在熔化的物品产生的流体加入虚拟罐。
     */
    private static void addMeltingItemFluids(SmelteryBlockEntity smeltery, VirtualAlloyTank tank, int temperature) {
        IItemHandler meltingInv = smeltery.getMeltingInventory();
        for (int i = 0; i < meltingInv.getSlots(); i++) {
            ItemStack meltingStack = meltingInv.getStackInSlot(i);
            if (!meltingStack.isEmpty()) {
                FluidStack result = MeltingRecipeLookup.findResult(meltingStack.getItem(), temperature);
                if (!result.isEmpty()) {
                    tank.addFluid(result);
                }
            }
        }
    }

    /**
     * 获取冶炼炉流体罐中量最多的流体。
     * 该流体会被移到底部作为浇注口的输出流体。
     */
    public static FluidStack getMaxFluidStack(SmelteryTank<?> tank) {
        FluidStack maxFluid = FluidStack.EMPTY;
        for (FluidStack fluid : tank.getFluids()) {
            if (fluid.getAmount() > maxFluid.getAmount()) {
                maxFluid = fluid;
            }
        }
        return maxFluid;
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
