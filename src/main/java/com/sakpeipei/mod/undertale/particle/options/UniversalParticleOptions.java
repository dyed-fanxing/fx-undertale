package com.sakpeipei.mod.undertale.particle.options;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class UniversalParticleOptions implements ParticleOptions {
    private final ParticleType<?> type;
    private final CompoundTag data;

    public UniversalParticleOptions(ParticleType<?> type, CompoundTag data) {
        this.type = type;
        this.data = data;
    }

    // 用于数据包和持久化存储，如：指令读取，保存至世界存档数据等，以JSON/NBT存储，人读取用，一般客户端
    public static final MapCodec<UniversalParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BuiltInRegistries.PARTICLE_TYPE.byNameCodec().fieldOf("type").forGetter(UniversalParticleOptions::getType),
                    CompoundTag.CODEC.fieldOf("data").forGetter(UniversalParticleOptions::getData)
            ).apply(instance, UniversalParticleOptions::new)
    );

    // 网络流 解码/编码，用于服务端 客户端传输数据，开销小，网络传输用，服务端传输用
    public static final StreamCodec<RegistryFriendlyByteBuf, UniversalParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(BuiltInRegistries.PARTICLE_TYPE.key()),
                    UniversalParticleOptions::getType,
                    ByteBufCodecs.COMPOUND_TAG,
                    UniversalParticleOptions::getData,
                    UniversalParticleOptions::new
            );
    /*
        场景1：粒子命令
            /particle yourmod:glow_orb ^0 ^1 ^0 0 0 0 0 1 force
        后台流程：
            命令解析器使用 MAP_CODEC 将JSON参数转为 UniversalParticleOption
            服务端用 STREAM_CODEC 将粒子数据发送给客户端
        场景2：粒子发射器方块
            存档时：MAP_CODEC 将粒子参数保存为NBT
            加载时：MAP_CODEC 从NBT还原数据
            运行时：STREAM_CODEC 同步粒子效果给附近玩家
     */

    @Override
    public @NotNull ParticleType<?> getType() {
        return type;
    }

    public CompoundTag getData() {
        return data;
    }
}
