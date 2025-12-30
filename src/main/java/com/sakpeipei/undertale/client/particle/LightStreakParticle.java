package com.sakpeipei.undertale.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightStreakParticle extends NoRandomDirParticle {
    private float size;
    private float lifetime;

    public LightStreakParticle(ClientLevel level, double x, double y, double z,double xd,double yd,double zd , SpriteSet sprite) {
        super(level, x, y, z,xd, yd, zd);
        setSprite(sprite.get(0,1));
        this.lifetime = 20;
    }


    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        this.quadSize = Mth.lerp(partialTick, 1, size);
        super.render(buffer, camera, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;
        public Provider(SpriteSet sprite) {this.sprite = sprite;}
        @Override
        public @Nullable Particle createParticle(@NotNull SimpleParticleType particleType, @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new LightStreakParticle(level, x, y, z,xd,yd,zd,sprite);
        }
    }


}
