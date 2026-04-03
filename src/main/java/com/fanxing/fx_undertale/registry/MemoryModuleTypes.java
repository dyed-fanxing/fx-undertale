package com.fanxing.fx_undertale.registry;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.ai.AttackNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class MemoryModuleTypes {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = DeferredRegister.create(BuiltInRegistries.MEMORY_MODULE_TYPE, FxUndertale.MOD_ID);


    public static final Supplier<MemoryModuleType<Unit>> ATTACKING = MEMORY_MODULE_TYPES.register("attacking", () -> new MemoryModuleType<>(Optional.empty()));
    public static final Supplier<MemoryModuleType<Unit>> MOVE_LOCKING = MEMORY_MODULE_TYPES.register("move_locking", () -> new MemoryModuleType<>(Optional.empty()));
    public static final Supplier<MemoryModuleType<Unit>> COOLDOWN_1 = MEMORY_MODULE_TYPES.register("cooldown_1", () -> new MemoryModuleType<>(Optional.empty()));
    public static final Supplier<MemoryModuleType<Unit>> COOLDOWN_2 = MEMORY_MODULE_TYPES.register("cooldown_2", () -> new MemoryModuleType<>(Optional.empty()));
    public static final Supplier<MemoryModuleType<Unit>> COOLDOWN_3 = MEMORY_MODULE_TYPES.register("cooldown_3", () -> new MemoryModuleType<>(Optional.empty()));
    public static final Supplier<MemoryModuleType<Unit>> COOLDOWN_4 = MEMORY_MODULE_TYPES.register("cooldown_4", () -> new MemoryModuleType<>(Optional.empty()));
    public static final Supplier<MemoryModuleType<Set<AttackNode<? extends LivingEntity>>>> ACTIVE_ATTACK_NODES = MEMORY_MODULE_TYPES.register("active_attack_nodes", () -> new MemoryModuleType<>(Optional.empty()));


    public static void register(IEventBus bus) {
        MEMORY_MODULE_TYPES.register(bus);
    }
}
