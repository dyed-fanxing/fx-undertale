package com.sakpeipei.undertale.entity.projectile;

import com.sakpeipei.undertale.utils.CollisionDetectionUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
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
//    public double accelerationPower;

    public AbstractPenetrableProjectile(EntityType<? extends AbstractPenetrableProjectile> type, Level level) {
        this(type, level,0.1);
    }
    public AbstractPenetrableProjectile(EntityType<? extends AbstractPenetrableProjectile> type, Level level,double accelerationPower) {
        super(type, level);
//        this.accelerationPower = accelerationPower;
    }


    @Override
    public void tick() {
        Entity entity = this.getOwner();
        if (this.level().isClientSide || (entity == null || !entity.isRemoved()) && this.level().isLoaded(this.blockPosition())) {
            super.tick();
            List<HitResult> hitResults = CollisionDetectionUtils.getHitResultsOnMoveVector(this,this::canHitEntity,getClipType(),isCollision());
//            log.info("攻击检测结果{}",hitResults);
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
            double speedSqr = vec3.lengthSqr();
            // 有位移时才更新旋转，否则由子类决定没位移时的朝向
            if(speedSqr > 2.5000003E-5F){
                this.updateRotation();
            }else if(speedSqr != 0f){
                this.discard();
            }
            double d0 = this.getX() + vec3.x;
            double d1 = this.getY() + vec3.y;
            double d2 = this.getZ() + vec3.z;
            float f;
            if (this.isInWater()) {
                for(int i = 0; i < 4; ++i) {
                    this.level().addParticle(ParticleTypes.BUBBLE, d0 - vec3.x * (double)0.25F, d1 - vec3.y * (double)0.25F, d2 - vec3.z * (double)0.25F, vec3.x, vec3.y, vec3.z);
                }
                f = this.getLiquidInertia();

            } else {
                f = this.getInertia();
            }
//            this.setDeltaMovement(vec3.add(vec3.normalize().scale(this.accelerationPower)).scale(f));
            this.setDeltaMovement(vec3.scale(f));
            if(!this.isNoGravity()){
                this.applyGravity();
            }
            ParticleOptions particleoptions = this.getTrailParticle();
            if (particleoptions != null) {
                this.level().addParticle(particleoptions, d0, d1 + this.getBbHeight() / 2, d2, 0.0F, 0.0F, 0.0F);
            }
            this.setPos(d0, d1, d2);
        } else {
            this.discard();
        }
    }

    /**
     * 用于决定碰撞判定的类型，碰撞判定，还是线段判定
     */
    protected boolean isCollision(){
        return false;
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return super.canHitEntity(entity) && !ownedBy(entity);
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float p_341906_) {
        return !this.isInvulnerableTo(damageSource);
    }



    @Override
    public boolean shouldRenderAtSqrDistance(double r) {
        double d0 = this.getBoundingBox().getSize() * (double)2.0F;
        if (Double.isNaN(d0)) {
            d0 = 4.0F;
        }

        d0 *= 64.0F;
        return r < d0 * d0;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
//        if (tag.contains("acceleration_power", 6)) {
//            this.accelerationPower = tag.getDouble("acceleration_power");
//        }
    }
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
//        tag.putDouble("acceleration_power", this.accelerationPower);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }
    protected float getInertia() {
        return 0.95F;
    }

    protected float getLiquidInertia() {
        return 0.8F;
    }

    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.WHITE_ASH;
    }

    protected ClipContext.@NotNull Block getClipType() {
        return ClipContext.Block.COLLIDER;
    }

}
