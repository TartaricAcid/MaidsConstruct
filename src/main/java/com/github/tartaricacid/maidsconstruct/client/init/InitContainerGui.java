package com.github.tartaricacid.maidsconstruct.client.init;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import com.github.tartaricacid.maidsconstruct.client.container.SmelteryConfigContainer;
import com.github.tartaricacid.maidsconstruct.client.gui.SmelteryConfigGui;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = MaidsConstruct.MOD_ID)
public final class InitContainerGui {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> MenuScreens.register(SmelteryConfigContainer.TYPE, SmelteryConfigGui::new));
    }
}
