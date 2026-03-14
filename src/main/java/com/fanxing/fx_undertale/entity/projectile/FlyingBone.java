package com.fanxing.fx_undertale.entity.projectile;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.common.phys.CollisionDeflection;
import com.fanxing.fx_undertale.common.phys.motion.AbstractPhysicsMotionModel;
import com.fanxing.fx_undertale.common.phys.motion.SpringMotionModel;
import com.fanxing.fx_undertale.entity.ISyncablePhysicsMotion;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
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
 * 飞行骨头统一类 - 支持普通射击、延迟射击、振荡模式、旋转模式
 */
public class FlyingBone extends AbstractPenetrableProjectile implements ISyncablePhysicsMotion, GeoEntity {

    private static final Logger log = LoggerFactory.getLogger(FlyingBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 基础属性 ==========
    protected float damage = 1.0f;
    protected float speed = 1.0f;
    protected int delay = 0;
    protected Vec3 shotVec;                    // 射击矢量
    protected Vec3 relativePos;                // 相对于拥有者的位置
    protected Vec3 targetPos;                  // 目标位置（用于振荡模式）

    // ========== 运动模型 ==========
    protected AbstractPhysicsMotionModel motion;

    // ========== 行为标志 ==========
    private boolean isAim = false;              // 延迟阶段是否瞄准目标
    private boolean isFollow = false;           // 延迟阶段是否跟随拥有者
    private boolean isRoll = false;             // 是否绕自身z轴旋转
    private float spinDir = 1f;                 // 旋转方向

    // ========== 构造函数 ==========

    public FlyingBone(EntityType<? extends FlyingBone> type, Level level) {
        super(type, level);
    }

    /**
     * 延迟移动模式 - 停留后按方向移动
     */
    public FlyingBone(EntityType<? extends FlyingBone> type, Level level, LivingEntity owner,
                      float damage, float speed, int delay) {
        this(type, level);
        this.setNoGravity(true);
        this.setOwner(owner);
        this.damage = damage;
        this.speed = speed;
        this.delay = delay;
    }

    /**
     * 立即移动模式 - 直接按矢量移动
     */
    public FlyingBone(EntityType<? extends FlyingBone> type, Level level, LivingEntity owner,
                      float damage, float speed, Vec3 velocity) {
        this(type, level);
        this.setNoGravity(true);
        this.setOwner(owner);
        this.damage = damage;
        this.speed = speed;
        this.delay = 0;
        this.setDeltaMovement(velocity.scale(speed));
    }

    // ========== 行为配置方法 ==========

    public FlyingBone aimShoot() {
        this.isAim = true;
        return this;
    }

    public FlyingBone vectorShoot(Vec3 vec3) {
        this.shotVec = vec3;
        return this;
    }
    public FlyingBone followAngleShoot(Vec3 relativePos) {
        this.isFollow = true;
        this.relativePos = relativePos;
        return this;
    }
    public FlyingBone followPosShoot(Vec3 relativePos) {
        this.isFollow = true;
        this.relativePos = relativePos;
        return this;
    }
    public FlyingBone setRoll(boolean roll) {
        this.isRoll = roll;
        return this;
    }
    public FlyingBone setSpinDir(float dir) {
        this.spinDir = dir;
        return this;
    }
    /**
     * 启动振荡/运动模型模式
     */
    public FlyingBone startMotion(AbstractPhysicsMotionModel motionModel, Vec3 targetPos, float initSpeed) {
        this.motion = motionModel;
        this.targetPos = targetPos;
        this.accelerationPower = 0;
        Vec3 toTarget = targetPos.subtract(this.getX(), this.getY(0.5f), this.getZ());
        this.shoot(toTarget.x, toTarget.y, toTarget.z, initSpeed, 0);
        return this;
    }

    // ========== 核心逻辑 ==========
    @Override
    public void tick() {
        // 延迟阶段
        if (delay > 0) {
            delay--;
            Entity owner = getOwner();
            if (owner != null) {
                // 跟随拥有者
                if (isFollow && relativePos != null) {
                    this.setPos(owner.position().add(RotUtils.getWorldVec3(relativePos, owner.getXRot(), owner.getYHeadRot())));
                    Vec3 viewVector = owner.getViewVector(1.0f);
                    RotUtils.lookVecShoot(this, viewVector);
                    shotVec = viewVector;
                }
                // 瞄准目标
                if (isAim && owner instanceof Targeting targeting) {
                    LivingEntity target = targeting.getTarget();
                    if(target != null) {
                        Vec3 toTarget = target.getEyePosition().subtract(this.getEyePosition());
                        RotUtils.lookAtEyeShoot(this, toTarget);
                        shotVec = toTarget;
                    }
                }
            }
            // 延迟结束，发射
            if (delay == 0 && !this.level().isClientSide && shotVec != null) {
                this.shoot(shotVec.x, shotVec.y, shotVec.z, speed, 0);
            }
            return;
        }

        // 运动模型模式
        if (motion != null) {
            this.setDeltaMovement(motion.update(this.position(), this.getDeltaMovement(), targetPos, null, 0.05f));
        }
        super.tick();
        // 旋转效果
        if (motion instanceof SpringMotionModel spring) {
            this.setYRot(this.getYRot() + spring.getTotalEnergy() * 45F * spinDir);
        }
    }



    // ========== 基类重载 ==========
    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        if (target instanceof LivingEntity livingTarget) {
            DamageSource damageSource;
            if (owner instanceof Sans) {
                damageSource = damageSources().source(DamageTypes.KARMA_BLOCKABLE, this, owner);
            } else if (owner instanceof LivingEntity) {
                damageSource = this.damageSources().mobProjectile(this, (LivingEntity) owner);
            } else {
                damageSource = this.damageSources().magic();
            }

            if (!livingTarget.hurt(damageSource, damage)) {
                // 格挡反弹
                if (livingTarget.isBlocking()) {
                    this.setNoGravity(false);
                    this.deflect(CollisionDeflection.MIRROR_DEFLECT, target, owner, false);
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
                }
            }
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        if (motion != null) {
            // 运动模型模式：滑动反弹
            CollisionDeflection.slideDeflect(this, result.getDirection().getNormal(),
                    this.level().random, 0.6f, 0f);
        } else {
            // 普通模式：直接销毁
            this.discard();
        }
    }


    @Override
    protected void updateRotation() {
        if (motion == null) {
            super.updateRotation();
        }
    }
    @Override
    protected float getInertia() {
        return 0.995f;
    }
    @Override
    protected float getLiquidInertia() {
        return 0.88888f;
    }
    @Override
    protected double getDefaultGravity() {
        return 0f;
    }
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }


    // ========== 接口实现 ==========
    @Override
    public AbstractPhysicsMotionModel getMotionModel() {
        return motion;
    }

    // ========== 数据同步 ==========
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putFloat("speed", speed);
        tag.putInt("delay", delay);
        tag.putBoolean("isFollow", isFollow);
        tag.putBoolean("isAim", isAim);
        tag.putBoolean("isRoll", isRoll);
        tag.putFloat("spinDir", spinDir);

        if (relativePos != null) {
            tag.put("relativePos", this.newDoubleList(relativePos.x, relativePos.y, relativePos.z));
        }
        if (shotVec != null) {
            tag.put("shotVec", this.newDoubleList(shotVec.x, shotVec.y, shotVec.z));
        }
        if (targetPos != null) {
            tag.put("targetPos", this.newDoubleList(targetPos.x, targetPos.y, targetPos.z));
        }
        if (motion != null) {
            motion.addAdditionalSaveData(tag);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("damage")) this.damage = tag.getFloat("damage");
        if (tag.contains("speed")) this.speed = tag.getFloat("speed");
        if (tag.contains("delay")) this.delay = tag.getInt("delay");
        if (tag.contains("isFollow")) this.isFollow = tag.getBoolean("isFollow");
        if (tag.contains("isAim")) this.isAim = tag.getBoolean("isAim");
        if (tag.contains("isRoll")) this.isRoll = tag.getBoolean("isRoll");
        if (tag.contains("spinDir")) this.spinDir = tag.getFloat("spinDir");

        if (tag.contains("relativePos")) {
            ListTag list = tag.getList("relativePos", 6);
            this.relativePos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        if (tag.contains("shotVec")) {
            ListTag list = tag.getList("shotVec", 6);
            this.shotVec = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        if (tag.contains("targetPos")) {
            ListTag list = tag.getList("targetPos", 6);
            this.targetPos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }

        this.motion = AbstractPhysicsMotionModel.fromTag(tag);
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(damage);
        buf.writeFloat(speed);
        buf.writeInt(delay);
        buf.writeFloat(spinDir);

        if (motion != null) {
            buf.writeBoolean(true);
            motion.writeSpawnData(buf);
        } else {
            buf.writeBoolean(false);
        }

        buf.writeBoolean(targetPos != null);
        if (targetPos != null) {
            buf.writeDouble(targetPos.x);
            buf.writeDouble(targetPos.y);
            buf.writeDouble(targetPos.z);
        }
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.damage = buf.readFloat();
        this.speed = buf.readFloat();
        this.delay = buf.readInt();
        this.spinDir = buf.readFloat();

        boolean hasMotion = buf.readBoolean();
        if (hasMotion) {
            this.motion = AbstractPhysicsMotionModel.fromBuf(buf);
        }

        boolean hasTargetPos = buf.readBoolean();
        if (hasTargetPos) {
            this.targetPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
    }




    // ========== GeoEntity ==========
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}