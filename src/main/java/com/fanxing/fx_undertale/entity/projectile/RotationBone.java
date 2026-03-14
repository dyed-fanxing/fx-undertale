package com.fanxing.fx_undertale.entity.projectile;

import com.fanxing.fx_undertale.common.DamageTypes;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RotationBone extends AbstractPenetrableProjectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected Vec3 shotVec;         // 射击矢量
    private boolean isFollow = false;       // 延迟阶段是否跟随拥有者
    private boolean isFollowAngle = false;  // 是否跟随拥有者视线


    private float damage = 1.0f;
    private float speed = 1.0f;


    // 核心插值属性
    public int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;


    public RotationBone(EntityType<? extends RotationBone> type, Level level) {
        super(type, level);
    }

    /**
     * 延迟移动，无需传递移动向量
     */
    public RotationBone(EntityType<? extends RotationBone> type, Level level, LivingEntity owner, float damage, float speed, int delay) {
        this(EntityTypes.ROTATION_BONE.get(), level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
        this.speed = speed;
    }

    /**
     * 立即移动，需要传递移动向量
     * velocity 移动向量，单位化的
     */
    public RotationBone(Level level, LivingEntity owner, float damage, float speed, Vec3 velocity) {
        this(EntityTypes.ROTATION_BONE.get(), level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
        this.speed = speed;
        this.shoot(velocity.x, velocity.y, velocity.z, speed, 0);
    }

    @Override
    public void tick() {
        // 处理旋转插值
        if (this.level().isClientSide && this.lerpSteps > 0 && Vec3.ZERO.equals(this.getDeltaMovement())) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        // 设置伤害逻辑
        if (target instanceof LivingEntity livingTarget) {
            DamageSource damageSource;
            if (owner instanceof Sans) {
                damageSource = damageSources().source(DamageTypes.KARMA_BLOCKABLE, this, owner);
            } else {
                damageSource = this.damageSources().mobProjectile(this, (LivingEntity) owner);
            }
            if (!livingTarget.hurt(damageSource, damage)) {
                if (livingTarget.isBlocking()) {
                    this.setNoGravity(false);
                    this.deflect(ProjectileDeflection.MIRROR_DEFLECT, target, this.getOwner(), false);
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
                }
            }
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        this.discard();
    }

    public void vectorShoot(Vec3 vec3) {
        this.shotVec = vec3;
        this.shoot(shotVec.x, shotVec.y, shotVec.z, speed, 0);
    }

    public void followAngleShoot(Vec3 relativePos) {
        this.isFollow = true;
        this.isFollowAngle = true;
    }

    @Override
    protected float getInertia() {
        return 0.99f;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05f;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("speed", speed);
        tag.putBoolean("isFollow", isFollow);
        tag.putBoolean("isFollowAngle", isFollowAngle);
        if (shotVec != null) {
            tag.put("shotVec", this.newDoubleList(shotVec.x, shotVec.y, shotVec.z));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setNoGravity(true);
        this.accelerationPower = 0.0f;
        if (tag.contains("speed")) {
            this.speed = tag.getFloat("speed");
        }
        this.isFollow = tag.getBoolean("isFollow");
        this.isFollowAngle = tag.getBoolean("isFollowAngle");

        if (tag.contains("shotVec")) {
            ListTag list = tag.getList("shotVec", 6);
            this.shotVec = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(this.speed);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.speed = buf.readFloat();
    }


    @Override
    public void lerpMotion(double p_37279_, double p_37280_, double p_37281_) {
        this.setDeltaMovement(p_37279_, p_37280_, p_37281_);
        if (this.xRotO == 0 && this.yRotO == 0) {
            this.xRotO = getXRot();
            this.yRotO = getYRot();
        }
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

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? (float) this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float) this.lerpYRot : this.getYRot();
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
