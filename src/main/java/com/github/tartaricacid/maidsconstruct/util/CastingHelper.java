package com.github.tartaricacid.maidsconstruct.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.AbstractCastingBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedDrainBlock;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CastingHelper {
    /**
     * 查找冶炼炉结构关联的所有浇注口。
     * 通过 forEachContained 遍历结构方块，定位排液孔（SearedDrainBlock），
     * 然后检查排液孔四周水平方向是否有浇注口（FaucetBlockEntity）。
     * 边线的排液孔可能在多个方向连接浇注口，全部纳入结果。
     */
    public static List<BlockPos> findFaucets(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        StructureData structure = smeltery.getStructure();
        if (structure == null) {
            return List.of();
        }

        Set<BlockPos> faucetSet = Sets.newHashSet();

        structure.forEachContained(mutablePos -> {
            if (!(level.getBlockState(mutablePos).getBlock() instanceof SearedDrainBlock)) {
                return;
            }

            // 检查排液孔四周水平方向是否有浇注口
            // 边线排液孔（位于结构边缘）可能在多个朝外方向连接浇注口
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adjacent = mutablePos.relative(dir);

                // 只检查结构外部的位置（排液孔朝外的方向）
                if (structure.isInside(adjacent)) {
                    continue;
                }

                BlockEntity be = level.getBlockEntity(adjacent);
                if (be instanceof FaucetBlockEntity) {
                    faucetSet.add(adjacent.immutable());
                }
            }
        });

        return Lists.newArrayList(faucetSet);
    }

    /**
     * 查找浇注口下方的浇铸台/浇铸盆（位于 faucetPos.below()）。
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
     * 查找最佳的浇注口+浇铸目标配对用于浇铸（用作行走目标）。
     * 如果没有合适的目标，返回 null。
     */
    @Nullable
    public static Pair<BlockPos, BlockPos> findBestPouringTarget(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<Pair<BlockPos, BlockPos>> targets = findAllPouringTargets(level, smeltery);
        return targets.isEmpty() ? null : targets.get(0);
    }

    /**
     * 查找所有可用的浇注口+浇铸目标配对，按优先级排序。
     * 优先级：浇铸盆（流体足够铸块时）> 带铸模的浇铸台 > 浇铸盆（流体不足时）。
     */
    public static List<Pair<BlockPos, BlockPos>> findAllPouringTargets(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> faucets = findFaucets(level, smeltery);
        SmelteryTank<HeatingStructureBlockEntity> tank = smeltery.getTank();

        // 获取量最多的流体（将被移到底部作为浇注口输出）
        FluidStack maxFluid = SmelteryHelper.getMaxFluidStack(tank);
        if (maxFluid.isEmpty()) {
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

            // 如果浇注口正在浇铸，跳过
            BlockEntity faucetBe = level.getBlockEntity(faucetPos);
            if (faucetBe instanceof FaucetBlockEntity faucet && faucet.isPouring()) {
                continue;
            }

            // 如果浇铸台已有流体
            if (isCastingBusy(casting)) {
                // 检查是否为"卡住"状态：有流体但冷却未开始（流体量不足）
                // 如果冶炼炉有匹配的流体，可以尝试补充来恢复冷却
                if (casting.getCoolingTime() < 0 && canRefillStuckCasting(casting, maxFluid)) {
                    // 补充卡住的浇铸台，给予最高优先级（4）
                    scored.add(Pair.of(Pair.of(faucetPos, castingPos), 4));
                }
                continue;
            }

            int score = scoreCastingTarget(level, casting, maxFluid);
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
     * 优先级：浇铸盆（流体足够铸块时=810mB）> 锭铸模（90mB）= 宝石铸模（100mB）> 粒铸模（10mB）
     * 浇铸台只允许锭铸模、宝石铸模和粒铸模，其他铸模返回 -1。
     * 流体不足以完成一次浇铸的目标也返回 -1。
     * 额外检查流体是否能与铸模/浇铸盆产出有效配方，避免无效浇铸。
     */
    private static int scoreCastingTarget(ServerLevel level, CastingBlockEntity casting, FluidStack fluid) {
        int availableFluid = fluid.getAmount();

        // 如果是铸造盆
        if (casting instanceof CastingBlockEntity.Basin) {
            RecipeType<ICastingRecipe> type = TinkerRecipeTypes.CASTING_BASIN.get();

            ItemStack basinCast = casting.getItem(CastingBlockEntity.INPUT);
            TagKey<Item> emptyCastTag = casting.getEmptyCastTag();

            // 焦褐铸造盆（requireCast=true）需要在 INPUT 槽放置空铸模才能浇铸
            if (casting.getBlockState().getBlock() instanceof AbstractCastingBlock castingBlock
                && castingBlock.isRequireCast()) {
                // 如果当前铸造盆没有空铸模，无法浇铸
                if (!basinCast.is(emptyCastTag)) {
                    return -1;
                }
            } else {
                // 否则，要么为空，要么为 emptyCastTag
                if (!basinCast.isEmpty() && !basinCast.is(emptyCastTag)) {
                    return -1;
                }
            }

            // 浇铸盆制造块（金属需要810mB）。只在流体充足时优先选择浇铸盆。
            if (availableFluid >= FluidValues.METAL_BLOCK
                && hasCastingRecipe(level, ItemStack.EMPTY, fluid, type)) {
                return 3;
            }
            return -1;
        }

        // 浇铸台：检查输入槽是否有铸模
        ItemStack inputCast = casting.getItem(CastingBlockEntity.INPUT);
        if (inputCast.isEmpty()) {
            return -1;
        }

        // 只允许锭铸模、宝石铸模和粒铸模，检查流体量是否充足且配方是否存在
        Item castItem = inputCast.getItem();
        RecipeType<ICastingRecipe> type = TinkerRecipeTypes.CASTING_TABLE.get();
        if (isIngotCast(castItem)) {
            return availableFluid >= FluidValues.INGOT
                   && hasCastingRecipe(level, inputCast, fluid, type) ? 2 : -1;
        }
        if (isGemCast(castItem)) {
            return availableFluid >= FluidValues.GEM
                   && hasCastingRecipe(level, inputCast, fluid, type) ? 2 : -1;
        }
        if (isNuggetCast(castItem)) {
            return availableFluid >= FluidValues.NUGGET
                   && hasCastingRecipe(level, inputCast, fluid, type) ? 1 : -1;
        }

        // 不支持的铸模类型
        return -1;
    }

    /**
     * 检查给定铸模和流体的组合是否存在有效的浇铸配方。
     */
    private static boolean hasCastingRecipe(ServerLevel level, ItemStack cast, FluidStack fluidStack,
                                            RecipeType<ICastingRecipe> recipeType) {
        VirtualCastingContainer container = new VirtualCastingContainer(cast, fluidStack);
        return level.getRecipeManager().getRecipeFor(recipeType, container, level).isPresent();
    }

    /**
     * 检查"卡住"的浇铸台是否可以通过补充流体来恢复。
     * 条件：冶炼炉中最大的流体与浇铸台中已有的流体类型相同。
     */
    private static boolean canRefillStuckCasting(CastingBlockEntity casting, FluidStack smelteryFluid) {
        return casting.getCapability(ForgeCapabilities.FLUID_HANDLER)
                .map(handler -> {
                    FluidStack castingFluid = handler.getFluidInTank(0);
                    return !castingFluid.isEmpty() && castingFluid.isFluidEqual(smelteryFluid);
                }).orElse(false);
    }

    /**
     * 检查物品是否为锭铸模（金质/沙质/红沙质）。
     */
    private static boolean isIngotCast(Item item) {
        return TinkerSmeltery.ingotCast.values().contains(item);
    }

    /**
     * 检查物品是否为宝石铸模（金质/沙质/红沙质）。
     */
    private static boolean isGemCast(Item item) {
        return TinkerSmeltery.gemCast.values().contains(item);
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
     * 检查冶炼炉附近是否有任何浇注口正在浇铸。
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
     * 检查冶炼炉附近是否有任何浇铸台正在有效冷却（含有流体、尚无输出物品且冷却正在进行）。
     * 如果浇铸台含有流体但 coolingTime 为 -1，说明流体量不足以触发配方，不视为正在冷却。
     */
    public static boolean isAnyCastingCooling(ServerLevel level, HeatingStructureBlockEntity smeltery) {
        List<BlockPos> faucets = findFaucets(level, smeltery);
        for (BlockPos faucetPos : faucets) {
            BlockPos castingPos = findCastingBelow(level, faucetPos);
            if (castingPos == null) {
                continue;
            }

            BlockEntity be = level.getBlockEntity(castingPos);
            if (be instanceof CastingBlockEntity casting) {
                // 有流体、无产出、且冷却正在进行（coolingTime >= 0 表示配方已匹配并开始冷却）
                if (isCastingBusy(casting) && casting.getItem(CastingBlockEntity.OUTPUT).isEmpty()
                    && casting.getCoolingTime() >= 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
