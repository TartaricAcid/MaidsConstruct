package com.github.tartaricacid.maidsconstruct.init;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import com.github.tartaricacid.maidsconstruct.task.data.SmelteryConfigData;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import net.minecraft.resources.ResourceLocation;

public final class InitTaskData {
    public static TaskDataKey<SmelteryConfigData> SMELTERY_CONFIG;

    public static void registerAll(TaskDataRegister register) {
        SMELTERY_CONFIG = register.register(new ResourceLocation(MaidsConstruct.MOD_ID, "smeltery_config"), SmelteryConfigData.CODEC);
    }
}
