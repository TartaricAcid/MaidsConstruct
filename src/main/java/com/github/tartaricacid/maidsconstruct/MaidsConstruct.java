package com.github.tartaricacid.maidsconstruct;

import com.github.tartaricacid.maidsconstruct.init.InitContainers;
import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.init.InitNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MaidsConstruct.MOD_ID)
public class MaidsConstruct {
    public static final String MOD_ID = "maidsconstruct";

    public MaidsConstruct() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        InitMemories.MEMORIES.register(modBus);
        InitContainers.CONTAINER_TYPE.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        InitNetwork.init();
    }
}
