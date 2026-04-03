package com.fanxing.fx_undertale.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fanxing.fx_undertale.particle.options.GrowTrackEntityOption;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;


public class BallGrowParticle extends TextureSheetParticle {
    private final float size;
    private final Entity entity;

    public BallGrowParticle(GrowTrackEntityOption options, ClientLevel level, double x, double y, double z, Entity entity) {
        super(level, x, y, z);
        this.entity = entity;
        this.size = options.size();
        super.lifetime = options.lifetime();
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        if(entity == null ||entity.isRemoved() ){
            remove();
            return;
        }
        // 更新位置到实体坐标+偏移量
        this.setPos(entity.getX(), entity.getY() + 1.5, entity.getZ());
        super.tick();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
//        this.quadSize = AnimationUtils.calculateProgress(super.age,super.lifetime,partialTick,AnimationUtils.EasingType.LINEAR) * size;
        super.render(buffer, camera, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<GrowTrackEntityOption> {
        private final SpriteSet sprite;
        public Provider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }
        @Override
        public Particle createParticle(@NotNull GrowTrackEntityOption options, @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            BallGrowParticle particle = new BallGrowParticle(options,level, x,y,z,level.getEntity(options.entityId()));
            particle.setSprite(sprite.get(0,1)); // 关键初始化！
            return particle;
        }
    }
}
