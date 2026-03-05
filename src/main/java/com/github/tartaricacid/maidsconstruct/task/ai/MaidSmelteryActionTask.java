package com.github.tartaricacid.maidsconstruct.task.ai;

import com.github.tartaricacid.maidsconstruct.init.InitMemories;
import com.github.tartaricacid.maidsconstruct.task.SmelteryExtraMaidBrain;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Optional;

/**
 * 冶炼炉动作任务的抽象父类。
 * 统一处理：状态检查、距离检查、状态转换和记忆清理。
 * 子类只需实现具体的动作逻辑。
 */
public abstract class MaidSmelteryActionTask extends Behavior<EntityMaid> {
    protected static final double INTERACT_RANGE_SQ = 2.5 * 2.5;

    protected MaidSmelteryActionTask() {
        super(ImmutableMap.of(InitEntities.TARGET_POS.get(), MemoryStatus.VALUE_PRESENT));
    }

    /**
     * 此任务要求的工作状态（用于 checkExtraStartConditions 检查）。
     */
    protected abstract SmelteryWorkState getRequiredState();

    /**
     * 动作完成后转换到的下一个工作状态。
     */
    protected abstract SmelteryWorkState getNextState();

    /**
     * 执行具体的动作逻辑。
     */
    protected abstract void performAction(ServerLevel level, EntityMaid maid);

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<SmelteryWorkState> state = maid.getBrain().getMemory(InitMemories.SMELTERY_STATE.get());
        if (state.isEmpty() || state.get() != this.getRequiredState()) {
            return false;
        }
        return maid.getBrain().getMemory(InitEntities.TARGET_POS.get())
                .map(tracker -> maid.blockPosition().distSqr(tracker.currentBlockPosition()) < INTERACT_RANGE_SQ)
                .orElse(false);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        this.performAction(level, maid);
        maid.getBrain().setMemory(InitMemories.SMELTERY_STATE.get(), this.getNextState());
        SmelteryExtraMaidBrain.clearActionMemories(maid);
    }
}
