package com.github.tartaricacid.maidsconstruct.client.compat.cloth;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.github.tartaricacid.maidsconstruct.config.MaidsConstructConfig.ALLOWED_FUELS;
import static com.github.tartaricacid.maidsconstruct.config.MaidsConstructConfig.MAID_SMELTERY_IMMUNITY;

public class ClothConfigIntegration {
    @SubscribeEvent
    public void onAddClothConfig(AddClothConfigEvent event) {
        MutableComponent title = Component.translatable("config.maidsconstruct.category");
        ConfigCategory category = event.getRoot().getOrCreateCategory(title);

        MutableComponent immunityName = Component.translatable("config.maidsconstruct.maid_smeltery_immunity");
        MutableComponent immunityTooltip = Component.translatable("config.maidsconstruct.maid_smeltery_immunity.tooltip");
        category.addEntry(event.getEntryBuilder()
                .startBooleanToggle(immunityName, MAID_SMELTERY_IMMUNITY.get())
                .setDefaultValue(MAID_SMELTERY_IMMUNITY.getDefault())
                .setTooltip(immunityTooltip)
                .setSaveConsumer(MAID_SMELTERY_IMMUNITY::set)
                .build());

        MutableComponent fuelsName = Component.translatable("config.maidsconstruct.allowed_fuels");
        MutableComponent fuelsTooltip = Component.translatable("config.maidsconstruct.allowed_fuels.tooltip");
        category.addEntry(event.getEntryBuilder()
                .startStrList(fuelsName, Lists.newArrayList(ALLOWED_FUELS.get()))
                .setDefaultValue(Lists.newArrayList(ALLOWED_FUELS.getDefault()))
                .setTooltip(fuelsTooltip)
                .setSaveConsumer(ALLOWED_FUELS::set)
                .build());
    }
}
