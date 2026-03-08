package com.github.tartaricacid.maidsconstruct.task.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SmelteryConfigData(boolean craftIngots, boolean craftBlocks,
                                 boolean sortInventory, boolean ignoreAllowlistTag) {
    public static final SmelteryConfigData DEFAULT = new SmelteryConfigData(true, true, true, false);

    public static final Codec<SmelteryConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("craft_ingots", true).forGetter(SmelteryConfigData::craftIngots),
            Codec.BOOL.optionalFieldOf("craft_blocks", true).forGetter(SmelteryConfigData::craftBlocks),
            Codec.BOOL.optionalFieldOf("sort_inventory", true).forGetter(SmelteryConfigData::sortInventory),
            Codec.BOOL.optionalFieldOf("ignore_allowlist_tag", false).forGetter(SmelteryConfigData::ignoreAllowlistTag)
    ).apply(instance, SmelteryConfigData::new));
}
