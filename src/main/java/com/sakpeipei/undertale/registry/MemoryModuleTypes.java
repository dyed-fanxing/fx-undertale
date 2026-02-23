package com.sakpeipei.undertale.registry;

import com.sakpeipei.undertale.Undertale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class MemoryModuleTypes {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, Undertale.MOD_ID);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Unit>> GLOBAL_COOLDOWN = MEMORY_MODULE_TYPES.register("global_cooldown", () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Unit>> COOLDOWN_1 = MEMORY_MODULE_TYPES.register("cooldown_1", () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Unit>> COOLDOWN_2 = MEMORY_MODULE_TYPES.register("cooldown_2", () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Unit>> COOLDOWN_3 = MEMORY_MODULE_TYPES.register("cooldown_3", () -> new MemoryModuleType<>(Optional.empty()));


    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Byte>> PHASE =
            MEMORY_MODULE_TYPES.register("phase", () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Float>> STAMINA =
            MEMORY_MODULE_TYPES.register("stamina", () -> new MemoryModuleType<>(Optional.empty()));


    public static void register(IEventBus bus) {
        MEMORY_MODULE_TYPES.register(bus);
    }
}
