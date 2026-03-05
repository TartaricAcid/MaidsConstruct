package com.github.tartaricacid.maidsconstruct.init;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import com.github.tartaricacid.maidsconstruct.task.SmelteryWorkState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

public class InitMemories {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORIES =
            DeferredRegister.create(ForgeRegistries.MEMORY_MODULE_TYPES, MaidsConstruct.MOD_ID);

    public static final RegistryObject<MemoryModuleType<SmelteryWorkState>> SMELTERY_STATE =
            MEMORIES.register("smeltery_state", () -> new MemoryModuleType<>(Optional.empty()));

    public static final RegistryObject<MemoryModuleType<BlockPos>> SMELTERY_POS =
            MEMORIES.register("smeltery_pos", () -> new MemoryModuleType<>(Optional.empty()));
}
