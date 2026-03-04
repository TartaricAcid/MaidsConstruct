package com.github.tartaricacid.maidsconstruct.compat;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.task.TaskSmeltery;
import com.github.tartaricacid.maidsconstruct.task.SmelteryExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.debug.target.DebugTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@LittleMaidExtension
public class LittleMaidCompat implements ILittleMaid {
    private static final int SMELTERY_COLOR = 0x9F00FF00;
    private static final int ACTION_TARGET_COLOR = 0x9FFF8000;
    private static final int LIFE_TIME = 2000;

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(new TaskSmeltery());
    }

    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        manager.addExtraMaidBrain(new SmelteryExtraMaidBrain());
    }

    @Override
    public Collection<? extends Function<EntityMaid, List<DebugTarget>>> getMaidDebugTargets() {
        return List.of(LittleMaidCompat::getSmelteryDebugTargets);
    }

    private static List<DebugTarget> getSmelteryDebugTargets(EntityMaid maid) {
        if (!TaskSmeltery.UID.equals(maid.getTask().getUid())) {
            return List.of();
        }

        List<DebugTarget> targets = new ArrayList<>();
        SmelteryWorkState state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get())
                .orElse(SmelteryWorkState.IDLE);

        maid.getBrain().getMemory(InitMemories.SMELTERY_POS.get()).ifPresent(pos ->
                targets.add(new DebugTarget(pos, SMELTERY_COLOR, "Smeltery [" + state.name() + "]", LIFE_TIME))
        );

        maid.getBrain().getMemory(InitMemories.ACTION_TARGET_POS.get()).ifPresent(pos ->
                targets.add(new DebugTarget(pos, ACTION_TARGET_COLOR, "Action Target", LIFE_TIME))
        );

        return targets;
    }
}
