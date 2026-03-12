package com.fanxing.fx_undertale.particle.options;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.fanxing.fx_undertale.registry.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public record GrowOption(float size, short lifetime) implements ParticleOptions {
    // 用于数据包和持久化存储，如：指令读取，保存至世界存档数据等，以JSON/NBT存储，人读取用，一般客户端
    public static final MapCodec<GrowOption> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("size").forGetter(GrowOption::size),
                    Codec.SHORT.fieldOf("lifetime").forGetter(GrowOption::lifetime)
            ).apply(instance, GrowOption::new)
    );

    // 网络流 解码/编码，用于服务端 客户端传输数据，开销小，网络传输用，服务端传输用
    public static final StreamCodec<RegistryFriendlyByteBuf, GrowOption> STREAM_CODEC =
            StreamCodec.of(
                    (buf, option) -> {
                        buf.writeFloat(option.size);
                        buf.writeShort(option.lifetime);
                    },
                    buf -> new GrowOption(buf.readFloat(), buf.readShort())
            );


    @Override
    public @NotNull ParticleType<GrowOption> getType() {
        return ParticleTypes.HALO_SCALE.get();
    }
}
