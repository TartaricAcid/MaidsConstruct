package com.github.tartaricacid.maidsconstruct.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipeLookup;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedDrainBlock;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SmelteryHelper {

    /**
     * 在女仆限制范围内查找最近的已成型冶炼炉/熔铸炉控制器。
     */
    @Nullable
    public static BlockPos findNearestSmeltery(ServerLevel level, EntityMaid maid) {
        BlockPos center = maid.hasRestriction() ? maid.getRestrictCenter() : maid.blockPosition();
        int range = (int) maid.getRestrictRadius();
        double closestDist = Double.MAX_VALUE;
        BlockPos closest = null;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -range; z <= range; z++) {
                    mutablePos.setWithOffset(center, x, y, z);
                    BlockEntity be = level.getBlockEntity(mutablePos);
                    if (be instanceof HeatingStructureBlockEntity smeltery) {
                        if (smeltery.getStructure() != null) {
                            double dist = mutablePos.distSqr(maid.blockPosition());
                            if (dist < closestDist) {
                                closestDist = dist;
                                closest = mutablePos.immutable();
                            }
                        }
                    }
                }
            }
        }
        return closest;
    }

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
                if (!fluid.isEmpty() && fluid.getFluid().isSame(net.minecraft.world.level.material.Fluids.LAVA)) {
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

    /**
     * 查找冶炼炉结构周围的所有浇口。
     * 通过扫描结构外围（包括边角）查找 FaucetBlockEntity，
     * 并验证浇口相邻方块中存在排液孔（SearedDrainBlock，冶炼炉和熔铸炉通用）。
     */
    public static List<BlockPos> findFaucets(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        StructureData structure = smeltery.getStructure();
        if (structure == null) {
            return List.of();
        }

        List<BlockPos> faucets = new ArrayList<>();
        BlockPos min = structure.getMinPos();
        BlockPos max = structure.getMaxPos();

        // 扫描结构外围两格范围，确保覆盖所有边角位置
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX() - 2; x <= max.getX() + 2; x++) {
                for (int z = min.getZ() - 2; z <= max.getZ() + 2; z++) {
                    // 跳过结构内部的位置
                    if (x >= min.getX() && x <= max.getX() && z >= min.getZ() && z <= max.getZ()) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (!(be instanceof FaucetBlockEntity)) {
                        continue;
                    }
                    // 验证浇口相邻方块中存在排液孔
                    if (hasAdjacentDrain(level, pos)) {
                        faucets.add(pos);
                    }
                }
            }
        }
        return faucets;
    }

    /**
     * 检查指定位置的相邻方块中是否存在排液孔（SearedDrainBlock）。
     */
    private static boolean hasAdjacentDrain(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).getBlock() instanceof SearedDrainBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找浇口下方的浇铸台/浇铸盆（位于 faucetPos.below()）。
     */
    @Nullable
    public static BlockPos findCastingBelow(ServerLevel level, BlockPos faucetPos) {
        BlockPos below = faucetPos.below();
        BlockEntity be = level.getBlockEntity(below);
        if (be instanceof CastingBlockEntity) {
            return below;
        }
        return null;
    }

    /**
     * 查找最佳的浇口+浇铸目标配对用于浇铸（用作行走目标）。
     * 如果没有合适的目标，返回 null。
     */
    @Nullable
    public static Pair<BlockPos, BlockPos> findBestPouringTarget(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<Pair<BlockPos, BlockPos>> targets = findAllPouringTargets(level, smeltery);
        return targets.isEmpty() ? null : targets.get(0);
    }

    /**
     * 查找所有可用的浇口+浇铸目标配对，按优先级排序。
     * 优先级：浇铸盆（流体足够铸块时）> 带铸模的浇铸台 > 浇铸盆（流体不足时）。
     */
    public static List<Pair<BlockPos, BlockPos>> findAllPouringTargets(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> faucets = findFaucets(level, smeltery);
        SmelteryTank<?> tank = smeltery.getTank();
        int fluidAmount = tank.getContained();
        if (fluidAmount <= 0) {
            return List.of();
        }

        List<Pair<Pair<BlockPos, BlockPos>, Integer>> scored = new ArrayList<>();

        for (BlockPos faucetPos : faucets) {
            BlockPos castingPos = findCastingBelow(level, faucetPos);
            if (castingPos == null) {
                continue;
            }

            BlockEntity castingBe = level.getBlockEntity(castingPos);
            if (!(castingBe instanceof CastingBlockEntity casting)) {
                continue;
            }

            // 如果输出槽已有物品，跳过（需要先收集）
            if (!casting.getItem(CastingBlockEntity.OUTPUT).isEmpty()) {
                continue;
            }

            // 如果浇口正在浇铸，跳过
            BlockEntity faucetBe = level.getBlockEntity(faucetPos);
            if (faucetBe instanceof FaucetBlockEntity faucet && faucet.isPouring()) {
                continue;
            }

            // 如果浇铸台已有流体（正在浇铸/冷却中），跳过
            if (isCastingBusy(casting)) {
                continue;
            }

            int score = scoreCastingTarget(casting, fluidAmount);
            if (score > 0) {
                scored.add(Pair.of(Pair.of(faucetPos, castingPos), score));
            }
        }

        // 按分数降序排列
        scored.sort((a, b) -> Integer.compare(b.getSecond(), a.getSecond()));
        return scored.stream().map(Pair::getFirst).toList();
    }

    /**
     * 为浇铸目标评分以进行优先级选择。
     * 分数越高 = 目标越优。
     * 如果目标无法接受流体，返回 -1。
     * 优先级：浇铸盆（流体足够铸块时=810mB）> 锭铸模（90mB）> 粒铸模（10mB）
     * 浇铸台只允许锭铸模和粒铸模，其他铸模返回 -1。
     * 流体不足以完成一次浇铸的目标也返回 -1。
     */
    private static int scoreCastingTarget(CastingBlockEntity casting, int availableFluid) {
        if (casting instanceof CastingBlockEntity.Basin) {
            // 浇铸盆制造块（金属需要810mB）。只在流体充足时优先选择浇铸盆。
            if (availableFluid >= FluidValues.METAL_BLOCK) {
                return 3;
            }
            return -1;
        }

        // 浇铸台：检查输入槽是否有铸模
        ItemStack inputCast = casting.getItem(CastingBlockEntity.INPUT);
        if (inputCast.isEmpty()) {
            return -1;
        }

        // 只允许锭铸模和粒铸模，检查流体是否充足
        Item castItem = inputCast.getItem();
        if (isIngotCast(castItem)) {
            return availableFluid >= FluidValues.INGOT ? 2 : -1;
        }
        if (isNuggetCast(castItem)) {
            return availableFluid >= FluidValues.NUGGET ? 1 : -1;
        }

        // 不支持的铸模类型
        return -1;
    }

    /**
     * 检查物品是否为锭铸模（金质/沙质/红沙质）。
     */
    private static boolean isIngotCast(Item item) {
        return TinkerSmeltery.ingotCast.values().contains(item);
    }

    /**
     * 检查物品是否为粒铸模（金质/沙质/红沙质）。
     */
    private static boolean isNuggetCast(Item item) {
        return TinkerSmeltery.nuggetCast.values().contains(item);
    }

    /**
     * 查找输出槽中有已完成物品的浇铸台/浇铸盆。
     */
    @Nullable
    public static Pair<BlockPos, BlockPos> findCollectableOutput(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> faucets = findFaucets(level, smeltery);
        for (BlockPos faucetPos : faucets) {
            BlockPos castingPos = findCastingBelow(level, faucetPos);
            if (castingPos == null) {
                continue;
            }

            BlockEntity be = level.getBlockEntity(castingPos);
            if (be instanceof CastingBlockEntity casting) {
                if (!casting.getItem(CastingBlockEntity.OUTPUT).isEmpty()) {
                    return Pair.of(faucetPos, castingPos);
                }
            }
        }
        return null;
    }

    /**
     * 检查冶炼炉附近是否有任何浇铸台含有铸模。
     */
    public static boolean hasCastingWithMold(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> faucets = findFaucets(level, smeltery);
        for (BlockPos faucetPos : faucets) {
            BlockPos castingPos = findCastingBelow(level, faucetPos);
            if (castingPos == null) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(castingPos);
            if (be instanceof CastingBlockEntity.Basin) {
                return true; // 浇铸盆不需要铸模
            }
            if (be instanceof CastingBlockEntity casting) {
                if (!casting.getItem(CastingBlockEntity.INPUT).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查冶炼炉附近是否有任何浇口正在浇铸。
     */
    public static boolean isAnyFaucetPouring(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> faucets = findFaucets(level, smeltery);
        for (BlockPos faucetPos : faucets) {
            BlockEntity be = level.getBlockEntity(faucetPos);
            if (be instanceof FaucetBlockEntity faucet && faucet.isPouring()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查浇铸方块实体是否繁忙（含有流体 = 正在浇铸或冷却中）。
     * 由于 tank 字段为私有，使用 FLUID_HANDLER 能力来检查。
     */
    public static boolean isCastingBusy(CastingBlockEntity casting) {
        return casting.getCapability(ForgeCapabilities.FLUID_HANDLER)
                .map(handler -> {
                    for (int i = 0; i < handler.getTanks(); i++) {
                        if (!handler.getFluidInTank(i).isEmpty()) {
                            return true;
                        }
                    }
                    return false;
                }).orElse(false);
    }

    /**
     * 检查冶炼炉附近是否有任何浇铸台正在冷却（含有流体但尚无输出物品）。
     */
    public static boolean isAnyCastingCooling(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> faucets = findFaucets(level, smeltery);
        for (BlockPos faucetPos : faucets) {
            BlockPos castingPos = findCastingBelow(level, faucetPos);
            if (castingPos == null) continue;

            BlockEntity be = level.getBlockEntity(castingPos);
            if (be instanceof CastingBlockEntity casting) {
                if (isCastingBusy(casting) && casting.getItem(CastingBlockEntity.OUTPUT).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

}
