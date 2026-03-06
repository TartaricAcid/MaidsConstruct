package com.github.tartaricacid.maidsconstruct.compat;

import com.github.tartaricacid.maidsconstruct.debug.SmelteryStateDebug;
import com.github.tartaricacid.maidsconstruct.init.InitTaskData;
import com.github.tartaricacid.maidsconstruct.task.SmelteryExtraMaidBrain;
import com.github.tartaricacid.maidsconstruct.task.TaskSmeltery;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.debug.target.DebugTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@LittleMaidExtension
public class LittleMaidCompat implements ILittleMaid {
    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(new TaskSmeltery());
    }

    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        manager.addExtraMaidBrain(new SmelteryExtraMaidBrain());
    }

    @Override
    public void registerTaskData(TaskDataRegister register) {
        InitTaskData.registerAll(register);
    }

    @Override
    public Collection<? extends Function<EntityMaid, List<DebugTarget>>> getMaidDebugTargets() {
        return List.of(SmelteryStateDebug::getSmelteryDebugTargets);
    }
}
