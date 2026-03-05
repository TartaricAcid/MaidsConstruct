package com.github.tartaricacid.maidsconstruct.task;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.Lists;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.List;

public class SmelteryExtraMaidBrain implements IExtraMaidBrain {
    /**
     * 清除与移动相关的记忆，通常在需要重置行动状态时调用。
     */
    public static void clearActionMemories(EntityMaid maid) {
        maid.getBrain().eraseMemory(InitEntities.TARGET_POS.get());
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    /**
     * 清除所有与冶炼炉相关的记忆，包括位置和状态。
     */
    public static void clearAllMemories(EntityMaid maid) {
        clearActionMemories(maid);
        maid.getBrain().eraseMemory(InitMemories.SMELTERY_STATE.get());
        maid.getBrain().eraseMemory(InitMemories.SMELTERY_POS.get());
    }

    @Override
    public List<MemoryModuleType<?>> getExtraMemoryTypes() {
        return Lists.newArrayList(
                InitMemories.SMELTERY_STATE.get(),
                InitMemories.SMELTERY_POS.get()
        );
    }
}
