package com.github.tartaricacid.maidsconstruct.init;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import com.github.tartaricacid.maidsconstruct.network.SetSmelteryConfigMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class InitNetwork {
    private static final String VERSION = "1.0.0";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MaidsConstruct.MOD_ID, "network"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void init() {
        CHANNEL.registerMessage(0, SetSmelteryConfigMessage.class,
                SetSmelteryConfigMessage::encode, SetSmelteryConfigMessage::decode, SetSmelteryConfigMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
