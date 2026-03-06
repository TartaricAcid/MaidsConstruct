package com.github.tartaricacid.maidsconstruct.config;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MaidsConstructConfig {
    /**
     * 允许女仆使用的燃料流体 ID 列表
     */
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_FUELS;

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("fuel");

        builder.comment("Fluid IDs that maids are allowed to use as smeltery fuel.");
        builder.comment("Default: [\"minecraft:lava\", \"tconstruct:blazing_blood\"]");
        ALLOWED_FUELS = builder.defineListAllowEmpty("AllowedFuels",
                Lists.newArrayList("minecraft:lava", "tconstruct:blazing_blood"),
                data -> data instanceof String str && ResourceLocation.isValidResourceLocation(str)
        );

        builder.pop();
        return builder.build();
    }

    /**
     * 检查给定流体是否在允许的燃料列表中。
     */
    public static boolean isAllowedFuel(Fluid fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        if (id == null) {
            return false;
        }
        String idStr = id.toString();
        return ALLOWED_FUELS.get().contains(idStr);
    }
}
