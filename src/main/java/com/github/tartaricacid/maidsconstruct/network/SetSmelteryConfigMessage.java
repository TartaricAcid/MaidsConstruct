package com.github.tartaricacid.maidsconstruct.network;

import com.github.tartaricacid.maidsconstruct.init.InitTaskData;
import com.github.tartaricacid.maidsconstruct.task.data.SmelteryConfigData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetSmelteryConfigMessage {
    private final int entityId;
    private final boolean craftIngots;
    private final boolean craftBlocks;
    private final boolean sortInventory;
    private final boolean ignoreAllowlistTag;

    public SetSmelteryConfigMessage(int entityId, boolean craftIngots, boolean craftBlocks,
                                    boolean sortInventory, boolean ignoreAllowlistTag) {
        this.entityId = entityId;
        this.craftIngots = craftIngots;
        this.craftBlocks = craftBlocks;
        this.sortInventory = sortInventory;
        this.ignoreAllowlistTag = ignoreAllowlistTag;
    }

    public static void encode(SetSmelteryConfigMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
        buf.writeBoolean(message.craftIngots);
        buf.writeBoolean(message.craftBlocks);
        buf.writeBoolean(message.sortInventory);
        buf.writeBoolean(message.ignoreAllowlistTag);
    }

    public static SetSmelteryConfigMessage decode(FriendlyByteBuf buf) {
        return new SetSmelteryConfigMessage(buf.readInt(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(SetSmelteryConfigMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) {
                    return;
                }
                Entity entity = sender.level().getEntity(message.entityId);
                if (entity instanceof EntityMaid maid && maid.isOwnedBy(sender)) {
                    SmelteryConfigData data = new SmelteryConfigData(message.craftIngots, message.craftBlocks,
                            message.sortInventory, message.ignoreAllowlistTag);
                    maid.setAndSyncData(InitTaskData.SMELTERY_CONFIG, data);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
