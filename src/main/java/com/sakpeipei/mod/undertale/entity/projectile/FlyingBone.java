package com.sakpeipei.mod.undertale.entity.projectile;

import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 18:44
 */
public class FlyingBone extends AbstractPenetrableProjectile implements GeoEntity {

    private static final Logger log = LoggerFactory.getLogger(FlyingBone.class);
    private float damage;
    private float speed;
    private int delay;
    // 核心插值属性
    public int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    public FlyingBone(EntityType<? extends FlyingBone> type, Level level) {
        super(type, level);
    }

    public FlyingBone(EntityType<? extends FlyingBone> type,  Level level,LivingEntity owner,float damage,float speed) {
        this(type, level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
        this.speed = speed;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.lerpSteps > 0 && this.getDeltaMovement().lengthSqr() == 0.0f) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
        delay--;
        if (!this.level().isClientSide) {
            Entity owner = getOwner();
            LivingEntity target = null;
            if(owner instanceof Targeting targeting){
                target = targeting.getTarget();
            }
            if (delay > 0) {
                if (owner != null) {
                    this.moveTo(owner.getEyePosition().add(this.relativeDir.yRot(-owner.getYHeadRot() * Mth.DEG_TO_RAD).xRot(-owner.getXRot() * Mth.DEG_TO_RAD)));
                }
                if (target != null) {
                    RotUtils.lookAtByShoot(this,target);
                }
            }else if(delay == 0){
                if (target != null) {
                    this.shoot(target.getX() - this.getX(),target.getEyeY() - this.getY(),target.getZ() - this.getZ(),speed,0);
                }else{
                    if(owner != null){
                        Vec3 lookAngle = owner.getLookAngle();
                        this.shoot(lookAngle.x,lookAngle.y,lookAngle.z,speed,0);
                    }else{
                        this.discard();
                    }
                }
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        // 设置伤害逻辑
        if (target instanceof LivingEntity livingTarget) {
            DamageSource damageSource;
            if(owner instanceof Sans ){
                damageSource = damageSources().source(DamageTypes.FRAME,this,owner);
            }else{
                damageSource = this.damageSources().mobProjectile(this, (LivingEntity) owner);
            }
            livingTarget.hurt(damageSource, damage);
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        this.discard();
    }
    public void delayShoot(int delay, Vec3 relativeDir){
        this.relativeDir = relativeDir;
        this.delay = delay;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("delay",delay);
        tag.putFloat("speed",speed);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.delay = tag.getInt("delay");
        this.speed = tag.getFloat("speed");
    }

    @Override
    public void lerpMotion(double p_37279_, double p_37280_, double p_37281_) {
        this.setDeltaMovement(p_37279_, p_37280_, p_37281_);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }
    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }
    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }
//    @Override
//    public float lerpTargetXRot() {
//        return this.lerpSteps > 0 ? (float)this.lerpXRot : this.getXRot();
//    }
//    @Override
//    public float lerpTargetYRot() {
//        return this.lerpSteps > 0 ? (float)this.lerpYRot : this.getYRot();
//    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return GeckoLibUtil.createInstanceCache(this);
    }


}
