package com.github.tartaricacid.maidsconstruct.task;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import com.github.tartaricacid.maidsconstruct.task.ai.*;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
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
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TaskSmeltery implements IMaidTask {
    public static final ResourceLocation UID = new ResourceLocation(MaidsConstruct.MOD_ID, "smeltery");

    private static final Supplier<ItemStack> ICON = Suppliers.memoize(() -> {
        ResourceLocation id = new ResourceLocation("tconstruct:mighty_smelting");
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return Objects.requireNonNullElse(item, Items.AIR).getDefaultInstance();
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
        return InitSounds.MAID_IDLE.get();
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
    public List<String> getDescription(EntityMaid maid) {
        return List.of(
                "task.maidsconstruct.smeltery.desc.1",
                "task.maidsconstruct.smeltery.desc.2"
        );
    }
}
