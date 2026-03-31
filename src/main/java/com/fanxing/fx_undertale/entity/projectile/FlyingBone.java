package com.fanxing.fx_undertale.entity.projectile;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.common.phys.CollisionDeflection;
import com.fanxing.fx_undertale.common.phys.motion.PhysicsMotionModel;
import com.fanxing.fx_undertale.entity.capability.Scalable;
import com.fanxing.fx_undertale.entity.capability.SyncablePhysicsMotion;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
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
public class FlyingBone extends AbstractPenetrableProjectile implements Scalable, GeoEntity {

    private static final Logger log = LoggerFactory.getLogger(FlyingBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 基础属性 ==========
    protected float damage = 1.0f;
    protected float speed = 1.0f;
    protected int delay = 0;
    protected float scale = 1.0f;
    protected float growScale = 1.0f;

    protected Vec3 shotVec;                    // 射击矢量
    protected Vec3 relativePos;                // 相对于拥有者的位置


    // ========== 行为标志 ==========
    private boolean isAim = false;              // 延迟阶段是否瞄准目标
    private boolean isFollow = false;           // 延迟阶段是否跟随拥有者

    // ========== 构造函数 ==========

    public FlyingBone(EntityType<? extends FlyingBone> type, Level level) {
        super(type, level);
    }
    public FlyingBone(EntityType<? extends FlyingBone> type, Level level, LivingEntity owner,float damage,float scale,float growScale) {
        this(type, level);
        this.setNoGravity(true);
        this.setOwner(owner);
        this.damage = damage;
        this.scale = scale;
        this.growScale = growScale;
    }

    // ========== 行为配置方法 ==========
    public FlyingBone aimShoot(int delay,float speed) {
        this.delay = delay;
        this.speed = speed;
        this.isAim = true;
        return this;
    }
    public FlyingBone followShoot(int delay,float speed,Vec3 relativePos) {
        this.delay = delay;
        this.speed = speed;
        this.isFollow = true;
        this.relativePos = relativePos;
        return this;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return super.getDimensions(pose).scale(scale,scale*growScale);
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
                    this.setPos(owner.position().add(RotUtils.rotateYX(relativePos, owner.getYHeadRot(), owner.getXRot())));
                    Vec3 viewVector = owner.getViewVector(1.0f);
                    RotUtils.lookVecShoot(this, viewVector);
                    shotVec = viewVector;
                }
                // 瞄准目标
                if (isAim && owner instanceof Targeting targeting) {
                    LivingEntity target = targeting.getTarget();
                    if(target != null) {
                        Vec3 toTarget = target.getEyePosition().subtract(this.getEyePosition());
                        RotUtils.lookVecShoot(this, toTarget);
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
        super.tick();
    }

    @Override
    public void lerpTo(double p_19896_, double p_19897_, double p_19898_, float p_19899_, float p_19900_, int p_19901_) {
//        this.setPos(p_19896_, p_19897_, p_19898_);
//        this.setRot(p_19899_, p_19900_);
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
            } else if (owner instanceof LivingEntity living) {
                damageSource = this.damageSources().mobProjectile(this, living);
            } else {
                damageSource = this.damageSources().magic();
            }

            if (!livingTarget.hurt(damageSource, damage)) {
                // 格挡反弹
                if (livingTarget.isBlocking()) {
                    this.setNoGravity(false);
                    this.deflect(CollisionDeflection.MIRROR_DEFLECT, target, owner, false);
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.02F));
                }
            }
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        // 普通模式：直接销毁
        this.discard();
    }


    @Override
    protected float getInertia() {
        return 0.99f;
    }
    @Override
    protected float getLiquidInertia() {
        return 0.88888f;
    }


    @Override
    protected double getDefaultGravity() {
        return 0.05F;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
    @Override
    public boolean isAttackable() {
        return false;
    }


    // ========== 接口实现 ==========
    @Override
    public float getScale() {
        return scale;
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
        if (relativePos != null) {
            tag.put("relativePos", this.newDoubleList(relativePos.x, relativePos.y, relativePos.z));
        }
        if (shotVec != null) {
            tag.put("shotVec", this.newDoubleList(shotVec.x, shotVec.y, shotVec.z));
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

        if (tag.contains("relativePos")) {
            ListTag list = tag.getList("relativePos", 6);
            this.relativePos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        if (tag.contains("shotVec")) {
            ListTag list = tag.getList("shotVec", 6);
            this.shotVec = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        this.refreshDimensions();
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(damage);
        buf.writeFloat(speed);
        buf.writeInt(delay);
        buf.writeBoolean(isFollow);
        buf.writeBoolean(isAim);
        buf.writeBoolean(relativePos != null);
        if (relativePos != null) {
            buf.writeDouble(this.relativePos.x);
            buf.writeDouble(this.relativePos.y);
            buf.writeDouble(this.relativePos.z);
        }
    }


    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.damage = buf.readFloat();
        this.speed = buf.readFloat();
        this.delay = buf.readInt();
        this.isFollow = buf.readBoolean();
        this.isAim = buf.readBoolean();
        if(buf.readBoolean()) this.relativePos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.refreshDimensions();
    }




    // ========== GeoEntity ==========
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}