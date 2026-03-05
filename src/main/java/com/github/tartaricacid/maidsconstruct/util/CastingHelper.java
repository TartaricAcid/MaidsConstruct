package com.github.tartaricacid.maidsconstruct.util;

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

public class CastingHelper {

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
