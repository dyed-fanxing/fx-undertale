package com.sakpeipei.undertale.particle.options;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sakpeipei.undertale.registry.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public record GrowTrackEntityOption(float size, short lifetime, int entityId) implements ParticleOptions {

    // 用于数据包和持久化存储，如：指令读取，保存至世界存档数据等，以JSON/NBT存储，人读取用，一般客户端
    public static final MapCodec<GrowTrackEntityOption> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("size").forGetter(p -> p.size),
                    Codec.SHORT.fieldOf("lifetime").forGetter(p -> p.lifetime),
                    Codec.INT.fieldOf("entityId").forGetter(p -> p.entityId)
            ).apply(instance, GrowTrackEntityOption::new)
    );

    // 网络流 解码/编码，用于服务端 客户端传输数据，开销小，网络传输用，服务端传输用
    public static final StreamCodec<RegistryFriendlyByteBuf, GrowTrackEntityOption> STREAM_CODEC =
            StreamCodec.of(
                    (buf, option) -> {
                        buf.writeFloat(option.size);
                        buf.writeShort(option.lifetime);
                        buf.writeVarInt(option.entityId);
                    },
                    buf -> new GrowTrackEntityOption(buf.readFloat(), buf.readShort(), buf.readVarInt())
            );

    @Override
    public @NotNull ParticleType<?> getType() {
        return ParticleRegistry.BALL_GROW.get();
    }
}
