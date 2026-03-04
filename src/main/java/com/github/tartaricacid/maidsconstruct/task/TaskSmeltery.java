package com.github.tartaricacid.maidsconstruct.task;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import com.github.tartaricacid.maidsconstruct.task.ai.*;
import com.github.tartaricacid.maidsconstruct.util.SmelteryHelper;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TaskSmeltery implements IMaidTask {
    public static final ResourceLocation UID = new ResourceLocation(MaidsConstruct.MOD_ID, "smeltery");

    private static final Supplier<ItemStack> ICON = Suppliers.memoize(() -> {
        Item value = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct:mighty_smelting"));
        return Objects.requireNonNullElse(value, Items.AIR).getDefaultInstance();
    });

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return ICON.get();
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        return Lists.newArrayList(
                Pair.of(5, new MaidSmelterySearchTask(0.6f, 4)),
                Pair.of(6, new MaidSmelteryFuelTask()),
                Pair.of(6, new MaidSmelteryInsertTask()),
                Pair.of(6, new MaidSmelteryPourTask()),
                Pair.of(6, new MaidSmelteryCollectTask())
        );
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Collections.singletonList(Pair.of("has_meltable", SmelteryHelper::hasMeltableItems));
    }
}
