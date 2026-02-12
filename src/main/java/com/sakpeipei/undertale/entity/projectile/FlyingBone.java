package com.sakpeipei.undertale.entity.projectile;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Targeting;
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
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final Logger log = LoggerFactory.getLogger(FlyingBone.class);
    protected Vec3 relativePos;     // 拥有者的相对位置

    protected Vec3 shotVec;         // 射击矢量
    private boolean isTrackTarget = false;  // 是否追踪目标
    private boolean isAim = false;          // 延迟阶段是否瞄准目标
    private boolean isFollow = false;       // 延迟阶段是否跟随拥有者
    private boolean isFollowAngle = false;  // 是否跟随拥有者视线
    private boolean isRoll = false;         // 是否绕自身z轴旋转


    private int delay = 10;
    private float damage = 1.0f;
    private float speed = 1.0f;


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
    /**
     * 延迟移动，无需传递移动向量
     */
    public FlyingBone(EntityType<? extends FlyingBone> type, Level level, LivingEntity owner, float damage, float speed, int delay) {
        this(type, level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
        this.speed = speed;
        this.delay = delay;
    }
    /**
     * 立即移动，需要传递移动向量
     * velocity 移动向量，单位化的
     */
    public FlyingBone(EntityType<? extends FlyingBone> type, Level level, LivingEntity owner, float damage, float speed,Vec3 velocity) {
        this(type, level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
        this.speed = speed;
        this.delay = 0;
        this.setDeltaMovement(velocity.scale(damage));
    }

    @Override
    public void tick() {
        delay--;
        if (delay > 0) {
            // 处理旋转插值
            if (this.lerpSteps > 0 && Vec3.ZERO.equals(this.getDeltaMovement())) {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
                this.lerpSteps--;
            }
            Entity owner = getOwner();
            if (owner != null) {
                if (isAim) {
                    if (owner instanceof Targeting targeting && targeting.getTarget() != null) {
                        LivingEntity target = targeting.getTarget();
                        if (target != null) {
                            RotUtils.lookAtShoot(this, target.getEyePosition());
                            shotVec = target.getEyePosition().subtract(this.getEyePosition());
                        }
                    }
                } else if (isFollowAngle) {
                    RotUtils.lookVecShoot(this, owner.getViewVector(1.0f));
                    shotVec = owner.getViewVector(1.0f);
                }
                if (isFollow) {
                    this.setPos(owner.position().add(RotUtils.getWorldPos(relativePos, owner.getXRot(), owner.getYHeadRot())));
                }
            }
            return;
        }
        if(delay < 0){
            super.tick();
        }
        if (delay == 0 && !this.level().isClientSide && shotVec != null) {
            this.shoot(shotVec.x, shotVec.y, shotVec.z, speed, 0);
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
                damageSource = damageSources().source(DamageTypes.FRAME, this, owner);
            } else {
                damageSource = this.damageSources().mobProjectile(this, (LivingEntity) owner);
            }
            if (!livingTarget.hurt(damageSource, damage)) {
                // TODO 如果是因为无敌导致的，则不应该执行，因为是穿透的，因为继续走，而不是被阻挡，待判定要不要用这个
                if (livingTarget.isBlocking()) {
                    this.setNoGravity(false);
                    this.deflect(ProjectileDeflection.MIRROR_DEFLECT, target, this.getOwner(), false);
                    if (!this.level().isClientSide) {
                        this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
                    }
                }
            }
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        this.discard();
    }

    public void aimShoot() {
        this.isAim = true;
    }

    public void vectorShoot(Vec3 vec3) {
        this.shotVec = vec3;
    }

    public void followAngleShoot(Vec3 relativePos) {
        this.isFollow = true;
        this.relativePos = relativePos;
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
        if (relativePos != null) {
            tag.put("relativePos", this.newDoubleList(relativePos.x, relativePos.y, relativePos.z));
        }

        tag.putInt("delay", delay);
        tag.putFloat("speed", speed);
        tag.putBoolean("isFollow", isFollow);
        tag.putBoolean("isFollowAngle", isFollowAngle);
        tag.putBoolean("isAim", isAim);
        tag.putBoolean("isRoll", isRoll);


        if (shotVec != null) {
            tag.put("shotVec", this.newDoubleList(shotVec.x, shotVec.y, shotVec.z));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setNoGravity(true);
        this.accelerationPower = 0.0f;
        if (tag.contains("delay")) {
            this.delay = tag.getInt("delay");
        }
        if (tag.contains("speed")) {
            this.speed = tag.getFloat("speed");
        }
        if (tag.contains("relativePos")) {
            ListTag list = tag.getList("relativePos", 6);
            this.relativePos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        this.isFollow = tag.getBoolean("isFollow");
        this.isFollowAngle = tag.getBoolean("isFollowAngle");
        this.isAim = tag.getBoolean("isAim");
        this.isRoll = tag.getBoolean("isRoll");

        if (tag.contains("shotVec")) {
            ListTag list = tag.getList("shotVec", 6);
            this.shotVec = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeInt(this.delay);
        buf.writeFloat(this.speed);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.delay = buf.readInt();
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
