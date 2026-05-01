package com.fanxing.fx_undertale.registry;

import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MemoryModuleTypes {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, FxUndertale.MOD_ID);

    public static void register(IEventBus bus) {
        MEMORY_MODULE_TYPES.register(bus);
    }
}
