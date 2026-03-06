package com.github.tartaricacid.maidsconstruct.init;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import com.github.tartaricacid.maidsconstruct.client.container.SmelteryConfigContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class InitContainers {
    public static final DeferredRegister<MenuType<?>> CONTAINER_TYPE =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MaidsConstruct.MOD_ID);

    public static final RegistryObject<MenuType<SmelteryConfigContainer>> SMELTERY_CONFIG =
            CONTAINER_TYPE.register("smeltery_config_container", () -> SmelteryConfigContainer.TYPE);
}
