package com.fanxing.fx_undertale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.WhiteAshParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class CustomWhiteAshNoGravityParticle extends WhiteAshParticle {

    protected CustomWhiteAshNoGravityParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, 1.0F, sprites); // 传递速度 0，后续用传入的速度覆盖
        this.gravity = 0.0F;                    // 取消重力
        this.xd = vx;                   // 使用传入速度
        this.yd = vy;
        this.zd = vz;
    }

    // 可选：覆盖颜色或大小变化

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            RandomSource random = level.random;
            vx += (random.nextDouble() - 0.5) * 0.02;
            vy += (random.nextDouble() - 0.5) * 0.02;
            vz += (random.nextDouble() - 0.5) * 0.02;
            return new CustomWhiteAshNoGravityParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}