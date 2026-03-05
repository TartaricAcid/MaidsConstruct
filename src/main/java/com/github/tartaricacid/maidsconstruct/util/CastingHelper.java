package com.github.tartaricacid.maidsconstruct.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import slimeknights.tconstruct.library.recipe.FluidValues;
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

        // 使用单种流体的最大量进行评分，而非总量
        // 因为冶炼炉只能输出底部的一种流体
        int fluidAmount = SmelteryHelper.getMaxFluidAmount(tank);
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

            // 如果浇注口正在浇铸，跳过
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
     * 检查冶炼炉附近是否有任何浇铸台正在冷却（含有流体但尚无输出物品）。
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
                if (isCastingBusy(casting) && casting.getItem(CastingBlockEntity.OUTPUT).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
