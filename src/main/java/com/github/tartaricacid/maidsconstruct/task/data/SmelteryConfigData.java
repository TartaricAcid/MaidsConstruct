package com.github.tartaricacid.maidsconstruct.task.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SmelteryConfigData(boolean craftIngots, boolean craftBlocks, boolean sortInventory) {
    public static final SmelteryConfigData DEFAULT = new SmelteryConfigData(true, true, true);

    public static final Codec<SmelteryConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("craft_ingots", true).forGetter(SmelteryConfigData::craftIngots),
            Codec.BOOL.optionalFieldOf("craft_blocks", true).forGetter(SmelteryConfigData::craftBlocks),
            Codec.BOOL.optionalFieldOf("sort_inventory", true).forGetter(SmelteryConfigData::sortInventory)
    ).apply(instance, SmelteryConfigData::new));
}
