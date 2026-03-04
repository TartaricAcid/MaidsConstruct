package com.github.tartaricacid.maidsconstruct;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MaidsConstruct.MOD_ID)
public class MaidsConstruct {
    public static final String MOD_ID = "maidsconstruct";

    public MaidsConstruct() {
        InitMemories.MEMORIES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
