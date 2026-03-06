package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitTaskData;
import com.github.tartaricacid.maidsconstruct.task.data.SmelteryConfigData;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.Tags;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 自动合成行为：将背包中的粒合成锭、锭合成块。
 * 独立于冶炼炉状态机运行，定期检查背包并执行合成。
 */
public class MaidCompactCraftTask extends MaidCheckRateTask {
    /**
     * 每分钟才检查一次背包，避免过于频繁地扫描和合成，影响性能。
     */
    private static final int CHECK_INTERVAL = 60 * 20;

    /**
     * 能够合成的原料
     */
    private static final TagKey<Item> NUGGETS_TAG = Tags.Items.NUGGETS;
    private static final TagKey<Item> INGOTS_TAG = Tags.Items.INGOTS;
    private static final TagKey<Item> STORAGE_BLOCKS_TAG = Tags.Items.STORAGE_BLOCKS;

    /**
     * 仅用于合成判断的临时容器，无实际功能
     */
    private static final AbstractContainerMenu DUMMY_MENU = new AbstractContainerMenu(MenuType.CRAFTING, 0) {
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void slotsChanged(Container container) {
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }
    };

    public MaidCompactCraftTask() {
        super(ImmutableMap.of());
        this.setMaxCheckRate(CHECK_INTERVAL);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (!super.checkExtraStartConditions(level, maid)) {
            return false;
        }
        SmelteryConfigData config = maid.getData(InitTaskData.SMELTERY_CONFIG);
        if (config == null) {
            config = SmelteryConfigData.DEFAULT;
        }
        return config.craftIngots() || config.craftBlocks() || config.sortInventory();
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        SmelteryConfigData config = maid.getData(InitTaskData.SMELTERY_CONFIG);
        if (config == null) {
            config = SmelteryConfigData.DEFAULT;
        }

        IItemHandler inv = maid.getAvailableInv(true);

        // 尝试合成粒 → 锭
        if (config.craftIngots()) {
            Map<Item, IntList> nuggetSlots = Maps.newHashMap();

            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.is(NUGGETS_TAG)) {
                    nuggetSlots.computeIfAbsent(stack.getItem(), k -> new IntArrayList()).add(i);
                }
            }

            for (var entry : nuggetSlots.entrySet()) {
                int total = countItems(inv, entry.getValue());
                if (total >= 9) {
                    tryCompact(level, inv, entry.getKey(), entry.getValue(), total);
                }
            }
        }

        // 尝试合成锭 → 块
        if (config.craftBlocks()) {
            Map<Item, IntList> ingotSlots = Maps.newHashMap();

            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.is(INGOTS_TAG)) {
                    ingotSlots.computeIfAbsent(stack.getItem(), k -> new IntArrayList()).add(i);
                }
            }

            for (var entry : ingotSlots.entrySet()) {
                int total = countItems(inv, entry.getValue());
                if (total >= 9) {
                    tryCompact(level, inv, entry.getKey(), entry.getValue(), total);
                }
            }
        }

        // 合成完毕后整理背包
        if (config.sortInventory()) {
            sortInventory(inv);
        }
    }

    /**
     * 按照 块 → 锭 → 粒 → 其他 的顺序整理背包。
     * 先将所有物品取出，排序后重新放入。
     */
    private void sortInventory(IItemHandler inv) {
        int slots = inv.getSlots();

        // 取出所有物品
        List<ItemStack> items = Lists.newArrayList();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = inv.extractItem(i, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        // 先合并同类物品
        List<ItemStack> merged = Lists.newArrayList();
        for (ItemStack stack : items) {
            boolean found = false;
            for (ItemStack existing : merged) {
                if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                    int transfer = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                    existing.grow(transfer);
                    stack.shrink(transfer);
                    if (stack.isEmpty()) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found && !stack.isEmpty()) {
                merged.add(stack);
            }
        }

        // 排序：块(0) → 锭(1) → 粒(2) → 其他(3)，同类型按物品名分组，同物品按数量降序
        merged.sort(Comparator.comparingInt(this::getSortPriority)
                .thenComparing(stack -> stack.getItem().toString())
                .thenComparing(ItemStack::getCount, Comparator.reverseOrder()));

        // 放回背包
        for (ItemStack stack : merged) {
            ItemHandlerHelper.insertItemStacked(inv, stack, false);
        }
    }

    /**
     * 获取物品的排序优先级，数值越小越靠前。
     */
    private int getSortPriority(ItemStack stack) {
        if (stack.is(STORAGE_BLOCKS_TAG)) {
            return 0;
        }
        if (stack.is(INGOTS_TAG)) {
            return 1;
        }
        if (stack.is(NUGGETS_TAG)) {
            return 2;
        }
        return 3;
    }

    private int countItems(IItemHandler inv, List<Integer> slots) {
        int total = 0;
        for (int slot : slots) {
            total += inv.getStackInSlot(slot).getCount();
        }
        return total;
    }

    /**
     * 尝试将 9 个相同物品合成为一个输出物品。
     * 每次调用最多合成 total/9 组。
     */
    private void tryCompact(ServerLevel level, IItemHandler inv, Item input, List<Integer> slots, int total) {
        int craftCount = total / 9;
        if (craftCount <= 0) {
            return;
        }

        // 构建 3x3 合成网格来查找配方
        ItemStack testInput = new ItemStack(input);
        CraftingContainer container = createCraftingContainer(testInput);

        Optional<CraftingRecipe> recipeOpt = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, container, level);

        if (recipeOpt.isEmpty()) {
            return;
        }

        ItemStack result = recipeOpt.get().assemble(container, level.registryAccess());
        if (result.isEmpty()) {
            return;
        }

        // 执行合成：每次合成消耗 9 个，产出 result
        for (int i = 0; i < craftCount; i++) {
            // 先检查输出是否能放入背包
            ItemStack outputCopy = result.copy();
            ItemStack simRemainder = ItemHandlerHelper.insertItemStacked(inv, outputCopy, true);
            if (!simRemainder.isEmpty()) {
                // 背包放不下，停止合成
                break;
            }

            // 消耗 9 个输入物品
            int toConsume = 9;
            for (int slot : slots) {
                if (toConsume <= 0) {
                    break;
                }
                int count = Math.min(toConsume, inv.getStackInSlot(slot).getCount());
                ItemStack extracted = inv.extractItem(slot, count, false);
                toConsume -= extracted.getCount();
            }

            // 放入输出物品
            ItemHandlerHelper.insertItemStacked(inv, result.copy(), false);
        }
    }

    /**
     * 创建一个用于配方查找的 3x3 合成容器，所有格子填入同一物品。
     */
    private CraftingContainer createCraftingContainer(ItemStack input) {
        TransientCraftingContainer container = new TransientCraftingContainer(DUMMY_MENU, 3, 3);
        for (int i = 0; i < 9; i++) {
            container.setItem(i, input.copy());
        }
        return container;
    }
}
