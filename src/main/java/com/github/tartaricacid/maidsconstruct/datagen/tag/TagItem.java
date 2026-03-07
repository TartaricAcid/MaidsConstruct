package com.github.tartaricacid.maidsconstruct.datagen.tag;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.concurrent.CompletableFuture;

public class TagItem extends ItemTagsProvider {
    /**
     * 女仆默认会向 冶炼炉/熔铸炉 投掷
     */
    public static final TagKey<Item> SMELTERY_ALLOWLIST = ItemTags.create(new ResourceLocation(MaidsConstruct.MOD_ID, "smeltery_allowlist"));

    /**
     * 粗粒 tag，forge 没有，但是匠魂支持
     */
    public static final TagKey<Item> RAW_NUGGETS = ItemTags.create(new ResourceLocation("forge", "raw_nuggets"));

    public TagItem(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider,
                   CompletableFuture<TagLookup<Block>> pBlockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, pLookupProvider, pBlockTags, MaidsConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    @SuppressWarnings("all")
    protected void addTags(HolderLookup.Provider provider) {
        tag(SMELTERY_ALLOWLIST).add(
                // 匠魂的两个泥砖也可以烧制
                TinkerSmeltery.grout.asItem(),
                TinkerSmeltery.netherGrout.asItem()
        ).addTags(
                // forge:ores 原矿石
                Tags.Items.ORES,
                // forge:raw_materials 粗矿
                Tags.Items.RAW_MATERIALS,
                // forge:dusts 粉末
                Tags.Items.DUSTS
        ).addOptionalTag(
                // forge:raw_nuggets 粗粒（匠魂支持）
                RAW_NUGGETS
        );
    }
}
