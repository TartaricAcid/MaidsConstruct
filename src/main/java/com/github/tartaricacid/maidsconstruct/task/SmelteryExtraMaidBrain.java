package com.github.tartaricacid.maidsconstruct.task;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.google.common.collect.Lists;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.List;

public class SmelteryExtraMaidBrain implements IExtraMaidBrain {
    @Override
    public List<MemoryModuleType<?>> getExtraMemoryTypes() {
        return Lists.newArrayList(
                InitMemories.SMELTERY_STATE.get(),
                InitMemories.SMELTERY_POS.get(),
                InitMemories.ACTION_TARGET_POS.get()
        );
    }
}
