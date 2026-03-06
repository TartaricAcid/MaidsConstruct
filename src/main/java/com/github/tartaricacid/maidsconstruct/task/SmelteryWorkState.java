package com.github.tartaricacid.maidsconstruct.task;

public enum SmelteryWorkState {
    /**
     * 空闲状态，什么都不做
     */
    IDLE,
    /**
     * 往冶炼炉里添加燃料
     */
    FUELING,
    /**
     * 往冶炼炉里添加要冶炼的物品，并取出无法熔化的物品
     */
    INTERACTING,
    /**
     * 等待物品冶炼完成
     */
    WAITING_MELT,
    /**
     * 从冶炼炉里浇筑冶炼好的液体
     */
    POURING,
    /**
     * 等待浇筑冷却完成
     */
    WAITING_CAST,
    /**
     * 收集浇筑好的物品
     */
    COLLECTING
}
