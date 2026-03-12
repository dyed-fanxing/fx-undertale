package com.fanxing.fx_undertale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.WhiteAshParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class CustomWhiteAshParticle extends WhiteAshParticle {

    protected CustomWhiteAshParticle(ClientLevel level, double x, double y, double z,double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, 1.0F, sprites); // 传递速度 0，后续用传入的速度覆盖
        this.xd = vx;                   // 使用传入速度
        this.yd = vy;
        this.zd = vz;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new CustomWhiteAshParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}