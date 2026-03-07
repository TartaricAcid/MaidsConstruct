package com.github.tartaricacid.maidsconstruct.datagen;

import com.github.tartaricacid.maidsconstruct.datagen.tag.TagBlock;
import com.github.tartaricacid.maidsconstruct.datagen.tag.TagItem;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var registries = event.getLookupProvider();
        var vanillaPack = generator.getVanillaPack(true);
        var helper = event.getExistingFileHelper();

        var block = vanillaPack.addProvider(packOutput -> new TagBlock(packOutput, registries, helper));
        vanillaPack.addProvider(packOutput -> new TagItem(packOutput, registries, block.contentsGetter(), helper));
    }
}
