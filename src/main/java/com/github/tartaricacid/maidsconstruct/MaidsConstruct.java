package com.github.tartaricacid.maidsconstruct;

import com.github.tartaricacid.maidsconstruct.client.compat.cloth.ClothConfigIntegration;
import com.github.tartaricacid.maidsconstruct.config.MaidsConstructConfig;
import com.github.tartaricacid.maidsconstruct.init.InitContainers;
import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.LoadingModList;

@Mod(MaidsConstruct.MOD_ID)
public class MaidsConstruct {
    public static final String MOD_ID = "maidsconstruct";
    public static final String CLOTH_CONFIG = "cloth_config";

    public MaidsConstruct() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MaidsConstructConfig.init());

        // 其他注册
        InitMemories.MEMORIES.register(modBus);
        InitContainers.CONTAINER_TYPE.register(modBus);

        // 如果 cloth config api 模组存在，注册女仆的配置界面
        if (FMLEnvironment.dist == Dist.CLIENT && LoadingModList.get().getModFileById(CLOTH_CONFIG) != null) {
            MinecraftForge.EVENT_BUS.register(new ClothConfigIntegration());
        }
    }
}
