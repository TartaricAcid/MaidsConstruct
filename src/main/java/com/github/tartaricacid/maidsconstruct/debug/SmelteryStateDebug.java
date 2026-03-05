package com.github.tartaricacid.maidsconstruct.debug;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.maidsconstruct.task.TaskSmeltery;
import com.github.tartaricacid.touhoulittlemaid.debug.target.DebugTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;

import java.util.List;

public class SmelteryStateDebug {
    private static final String DEBUG_TEXT = "Smeltery [%s]";
    private static final int SMELTERY_COLOR = 0x9F00FF00;
    private static final int LIFE_TIME = 2000;

    public static List<DebugTarget> getSmelteryDebugTargets(EntityMaid maid) {
        if (!TaskSmeltery.UID.equals(maid.getTask().getUid())) {
            return List.of();
        }

        List<DebugTarget> targets = Lists.newArrayList();
        SmelteryWorkState state = maid.getBrain()
                .getMemory(InitMemories.SMELTERY_STATE.get())
                .orElse(SmelteryWorkState.IDLE);

        maid.getBrain().getMemory(InitMemories.SMELTERY_POS.get()).ifPresent(pos -> {
            String text = DEBUG_TEXT.formatted(state.name());
            var target = new DebugTarget(pos, SMELTERY_COLOR, text, LIFE_TIME);
            targets.add(target);
        });

        return targets;
    }
}
