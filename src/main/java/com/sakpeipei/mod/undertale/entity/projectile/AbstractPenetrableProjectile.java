package com.sakpeipei.mod.undertale.entity.projectile;

import com.sakpeipei.mod.undertale.utils.ProjectileUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Sakqiongzi
 * @since 2025-09-26 21:05
 * 可跟随owner的弹射物
 * 与 AbstractHurtingProjectile的区别在于tick中的旋转更新为立刻更新，而不是类似模拟重物的平滑更新
 *
 */
public abstract class AbstractPenetrableProjectile extends Projectile {
    private static final Logger log = LoggerFactory.getLogger(AbstractPenetrableProjectile.class);
    protected Vec3 relativeDir;     // 相对于拥有者的位置向量
    public double accelerationPower;

    public AbstractPenetrableProjectile(EntityType<? extends AbstractPenetrableProjectile> type, Level level, double accelerationPower) {
        super(type, level);
        this.accelerationPower = accelerationPower;
    }

    protected AbstractPenetrableProjectile(EntityType<? extends AbstractPenetrableProjectile> type, Level level) {
        this(type, level,0.1);
    }

    @Override
    public void tick() {
        Entity entity = this.getOwner();
        if (this.level().isClientSide || (entity == null || !entity.isRemoved()) && this.level().isLoaded(this.blockPosition())) {
            super.tick();
            List<HitResult> hitResults = ProjectileUtils.getEntityHitResultsOnMoveVector(this, this::canHitEntity, this.getClipType());
            for (HitResult hitResult : hitResults) {
                if (hitResult.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hitResult)) {
                    ProjectileDeflection projectileDeflection = this.hitTargetOrDeflectSelf(hitResult);
                    if(projectileDeflection != ProjectileDeflection.NONE){
                        break;
                    }
                }
            }
            this.checkInsideBlocks();
            Vec3 vec3 = this.getDeltaMovement();
            // 有位移时才更新旋转，否则由子类决定没位移时的朝向
            if(vec3.lengthSqr() > 0) {
                this.updateRotation();
            }
            double d0 = this.getX() + vec3.x;
            double d1 = this.getY() + vec3.y;
            double d2 = this.getZ() + vec3.z;
            float f;
            if (!this.isInWater()) {
                f = this.getInertia();
            } else {
                for(int i = 0; i < 4; ++i) {
                    this.level().addParticle(ParticleTypes.BUBBLE, d0 - vec3.x * (double)0.25F, d1 - vec3.y * (double)0.25F, d2 - vec3.z * (double)0.25F, vec3.x, vec3.y, vec3.z);
                }
                f = this.getLiquidInertia();
            }
            this.setDeltaMovement(vec3.add(vec3.normalize().scale(this.accelerationPower)).scale(f));
            ParticleOptions particleoptions = this.getTrailParticle();
            if (particleoptions != null) {
                this.level().addParticle(particleoptions, d0, d1 + this.getBbHeight() / 2, d2, 0.0F, 0.0F, 0.0F);
            }
            this.setPos(d0, d1, d2);
        } else {
            this.discard();
        }

    }


    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("acceleration_power", this.accelerationPower);
        if(relativeDir != null){
            tag.putDouble("relative_dir_x", relativeDir.x);
            tag.putDouble("relative_dir_y", relativeDir.y);
            tag.putDouble("relative_dir_z", relativeDir.z);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("acceleration_power", 6)) {
            this.accelerationPower = tag.getDouble("acceleration_power");
        }
        if (tag.contains("relative_dir_x")) {
            this.relativeDir = new Vec3(tag.getDouble("relative_dir_x"),tag.getDouble("relative_dir_y"),tag.getDouble("relative_dir_z"));
        }
    }

    @Nullable
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.WHITE_ASH;
    }

    protected float getInertia() {
        return 0.95F;
    }

    protected float getLiquidInertia() {
        return 0.8F;
    }

    protected ClipContext.Block getClipType() {
        return ClipContext.Block.COLLIDER;
    }
    // 适度放大
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize() * 2.0F; // 2倍而不是4倍
        if (Double.isNaN(size)) {
            size = 2.0F;
        }
        size *= 64.0F;
        return distance < size * size;
    }
}
