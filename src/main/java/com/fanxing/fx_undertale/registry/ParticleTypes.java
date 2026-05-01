package com.fanxing.fx_undertale.registry;

import com.mojang.serialization.MapCodec;
import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, FxUndertale.MOD_ID);
    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }

    private static DeferredHolder<ParticleType<?>,SimpleParticleType> register(String name, boolean overrideLimiter) {
        return PARTICLE_TYPES.register(name,() -> new SimpleParticleType(overrideLimiter));
    }
    private static <T extends ParticleOptions> DeferredHolder<ParticleType<?>,ParticleType<T>> register(String name, boolean overrideLimiter,
            final Function<ParticleType<T>, MapCodec<T>> codecFactory,
            final Function<ParticleType<T>, StreamCodec<? super RegistryFriendlyByteBuf, T>> streamCodecFactory
    ) {
        return PARTICLE_TYPES.register(name, () -> new ParticleType<T>(overrideLimiter) {
            @Override
            public @NotNull MapCodec<T> codec() {
                return codecFactory.apply(this);
            }
            @Override
            public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodecFactory.apply(this);
            }
        });
    }
}
