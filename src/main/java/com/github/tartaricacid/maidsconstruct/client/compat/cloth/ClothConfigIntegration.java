package com.github.tartaricacid.maidsconstruct.client.compat.cloth;

import com.github.tartaricacid.maidsconstruct.config.MaidsConstructConfig;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class ClothConfigIntegration {
    @SubscribeEvent
    public void onAddClothConfig(AddClothConfigEvent event) {
        MutableComponent title = Component.translatable("config.maidsconstruct.category");
        ConfigCategory category = event.getRoot().getOrCreateCategory(title);

        MutableComponent name = Component.translatable("config.maidsconstruct.allowed_fuels");
        MutableComponent tooltip = Component.translatable("config.maidsconstruct.allowed_fuels.tooltip");
        List<String> value = Lists.newArrayList(MaidsConstructConfig.ALLOWED_FUELS.get());
        List<String> defaultValue = Lists.newArrayList(MaidsConstructConfig.ALLOWED_FUELS.getDefault());

        category.addEntry(event.getEntryBuilder()
                .startStrList(name, value)
                .setDefaultValue(defaultValue)
                .setTooltip(tooltip)
                .setSaveConsumer(MaidsConstructConfig.ALLOWED_FUELS::set)
                .build());
    }
}
