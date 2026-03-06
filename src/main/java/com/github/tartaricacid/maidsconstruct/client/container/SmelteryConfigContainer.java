package com.github.tartaricacid.maidsconstruct.client.container;

import com.github.tartaricacid.touhoulittlemaid.inventory.container.task.TaskConfigContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class SmelteryConfigContainer extends TaskConfigContainer {
    public static final MenuType<SmelteryConfigContainer> TYPE = IForgeMenuType.create(
            (windowId, inv, data) -> new SmelteryConfigContainer(windowId, inv, data.readInt()));

    public SmelteryConfigContainer(int id, Inventory inventory, int entityId) {
        super(TYPE, id, inventory, entityId);
    }
}
