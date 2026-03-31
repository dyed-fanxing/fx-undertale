package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.common.phys.motion.PhysicsMotionModel;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.capability.OBBRotationCollider;
import com.fanxing.fx_undertale.entity.capability.QuaternionRotatable;
import com.fanxing.fx_undertale.entity.capability.SyncablePhysicsMotion;
import com.fanxing.fx_undertale.net.packet.QuaternionSyncPacket;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.fanxing.fx_undertale.utils.collsion.EntityHitResultTimed;
import com.fanxing.fx_undertale.utils.collsion.OBBCCDUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class RotationBone extends AbstractBone<RotationBone> implements QuaternionRotatable, OBBRotationCollider, SyncablePhysicsMotion, GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(RotationBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private PhysicsMotionModel motion = new PhysicsMotionModel();
    private Vec3 targetPos;

    // 内部四元数，用于存储姿态（避免万向节死锁）
    private Quaternionf orientation = new Quaternionf();
    public Quaternionf previousOrientation = new Quaternionf(); // 上一帧的四元数，用于插值

    private static final EntityDataAccessor<Vector3f> ANGULAR_VELOCITY_VECTOR = SynchedEntityData.defineId(RotationBone.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Quaternionf> ORIENTATION_DATA = SynchedEntityData.defineId(RotationBone.class, EntityDataSerializers.QUATERNION);

    public RotationBone(EntityType<? extends AbstractBone<RotationBone>> type, Level level) {
        super(type, level);
    }

    public RotationBone(Level level, LivingEntity owner, float scale, float growScale, int lifetime, float damage) {
        super(EntityTypes.ROTATION_BONE.get(), level, owner, scale, growScale, lifetime, damage);
    }

    public RotationBone initOrientation(float yaw, float pitch, float roll) {
        orientation.rotateYXZ(yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD, roll * Mth.DEG_TO_RAD);  // 创建副本
        this.previousOrientation.set(this.orientation);
        return this;
    }

    public RotationBone initOrientation(Quaternionf orientation) {
        this.orientation.set(orientation);  // 创建副本
        this.previousOrientation.set(this.orientation);
        return this;
    }

    @Override
    public RotationBone gravity(Direction gravity) {
        Quaternionf gravityRotation = Gravity.getRotation(gravity);
        orientation = gravityRotation.mul(orientation, new Quaternionf());
        Vector3f currentAngularVel = new Vector3f(getAngularVelocity());  // 创建副本
        Vector3f temp = new Vector3f(currentAngularVel);
        temp.rotate(gravityRotation);
        setAngularVelocity(temp);
        syncEulerAnglesFromQuaternion();
        return this;
    }

    /**
     * 基本弧度
     */
    public RotationBone angularVelocity(Vector3f angularVelocity) {
        this.entityData.set(ANGULAR_VELOCITY_VECTOR, angularVelocity);
        return this;
    }

    /**
     * 便捷方法：通过角度（度）和轴向量设置角速度
     *
     * @param angleDegrees 旋转角度（度）
     * @param axis         旋转轴方向向量
     * @return this
     */
    public RotationBone angularVelocity(float angleDegrees, Vec3 axis) {
        return angularVelocity(axis.normalize().scale(angleDegrees * Mth.DEG_TO_RAD).toVector3f());
    }


    public RotationBone motion(PhysicsMotionModel motionModel, Vec3 targetPos) {
        this.motion = motionModel;
        this.targetPos = targetPos;
        return this;
    }

    public void shoot(Vec3 velocity) {
        this.setDeltaMovement(velocity);
        this.hasImpulse = true;
    }


    @Override
    public float getGrowProgress(float partialTick) {
        if (tickCount <= lifetime) {
            return CurvesUtils.parametricHeight((tickCount + partialTick) / lifetime, holdTimeScale, 0.8f);
        }
        return 1f;
    }

    @Override
    public void tick() {
        if (tickCount >= lifetime) {
            this.discard();
        }
        // 保存上一帧的四元数（用于插值）
        previousOrientation.set(orientation);

        this.setDeltaMovement(motion.update(this.position(), this.getDeltaMovement(), targetPos, null, 1f));
        // 1. 碰撞检测 ，检测到了，速度取反
        super.tick();

        // 使用四元数增量更新姿态（绕任意轴旋转，使用 JOML integrate 方法）
        // 注意：angularVelocity 向量必须使用弧度制
        Vector3f angularVelVector = getAngularVelocity();
        if (angularVelVector.lengthSquared() > 0) {
            // 左乘：绕世界轴旋转
            orientation = new Quaternionf().integrate(1.0f, angularVelVector.x, angularVelVector.y, angularVelVector.z).mul(orientation);
        }
        this.entityData.set(ORIENTATION_DATA, new Quaternionf(orientation));
        // 同步欧拉角给渲染器
        syncEulerAnglesFromQuaternion();
        refreshDimensions();  // 会调用 updateOBB()，通过 getUpVector() 拦截器使用四元数
    }

    /**
     * 从内部四元数同步欧拉角给渲染器
     * 只在需要时转换，避免万向节死锁影响
     */
    public void syncEulerAnglesFromQuaternion() {
        Vector3f euler = orientation.getEulerAnglesYXZ(new Vector3f());
        float newYaw = -euler.y() * Mth.RAD_TO_DEG;
        float newPitch = euler.x() * Mth.RAD_TO_DEG;
        setYRot(newYaw);
        setXRot(newPitch);
    }

    @Override
    public Quaternionf getLerpOrientation(float partialTick) {
        return previousOrientation.slerp(orientation, partialTick, new Quaternionf());
    }

    /**
     * 获取当前姿态的四元数
     */
    @Override
    public Quaternionf getOrientation() {
        return orientation;
    }

    @Override
    protected List<EntityHitResult> getEntityHitResults(Vec3 to) {
        if (getAngularVelocity().lengthSquared() > 0) {
            boolean noMove = getDeltaMovement().lengthSqr() == 0;
            Vector3f angularVelVector = getAngularVelocity();
            if (noMove)
                return new ArrayList<>(OBBCCDUtils.getEntityHitResultsOnlyOnRotation(obb, angularVelVector, this.position(), this.level(), this, this::canHitEntity));
            else
                return new ArrayList<>(OBBCCDUtils.getEntityHitResults(obb, this.getDeltaMovement(), angularVelVector, this.position(), this.level(), this, this::canHitEntity));
        } else return OBBCCDUtils.getEntityHitResultsOnlyOnMove(obb, this.getDeltaMovement(), this, this::canHitEntity);
    }

    @Override
    void updateRotation(Vec3 velocity) {
    }

    @Override
    protected BlockHitResult getBlockHitResult() {
        return BlockHitResult.miss(obb.getCenter().add(this.getDeltaMovement()), null, null);
    }

    // ---------- 碰撞处理 ----------
    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();
        DamageSource damageSource;
        // 设置伤害逻辑
        if (target instanceof LivingEntity livingTarget) {
            if (owner instanceof Sans) {
                damageSource = new DamageSource(damageSources().damageTypes.getHolderOrThrow(DamageTypes.KARMA_BLOCKABLE), this, owner, hitResult.getLocation());
            } else {
                damageSource = new DamageSource(damageSources().damageTypes.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.MOB_PROJECTILE), this, owner, hitResult.getLocation());
            }
            // KEY 这里的造成伤害仅仅只是判断的伤害值是否小于0，而不是判断目标是否被受伤，但没有掉血
            boolean hurt = livingTarget.hurt(damageSource, damage) && livingTarget.isDamageSourceBlocked(damageSource);
            // 只有是盾牌碰撞检测成功的且线速度和目标方向不同，则判定可以反弹
            Vec3 location = hitResult.getLocation();
            if (getAngularVelocity().lengthSquared() > 0) {
                boolean isInvulnerableTo = livingTarget.isInvulnerableTo(damageSource);
                boolean isBlocked = livingTarget.isDamageSourceBlocked(damageSource);
                // 格挡成功，判断是否反弹
                boolean shouldBounce = false;
                if (!hurt && isBlocked) {
                    // 计算碰撞点处的线速度
                    Vec3 rotationCenter = this.position();
                    Vec3 radiusVector = location.subtract(rotationCenter);
                    Vector3f angularVel = getAngularVelocity();
                    Vector3f linearVel = new Vector3f();
                    angularVel.cross(new Vector3f((float) radiusVector.x, (float) radiusVector.y, (float) radiusVector.z), linearVel);
                    if (linearVel.length() > 0.001f) {
                        Vec3 linearVelDir = new Vec3(linearVel.x, linearVel.y, linearVel.z).normalize();
                        Vec3 viewVector = livingTarget.getViewVector(1.0F);
                        float dotProduct = (float) (linearVelDir.x * viewVector.x + linearVelDir.y * viewVector.y + linearVelDir.z * viewVector.z);

                        shouldBounce = dotProduct < 0;
                    }
                }
                log.debug("反弹判断总结：!hurt={}, !isInvulnerableTo={}, isBlocked={}, shouldBounce={}", !hurt, !isInvulnerableTo, isBlocked, shouldBounce);
                if (!hurt && livingTarget.isDamageSourceBlocked(damageSource) && shouldBounce) {
                    // 格挡成功，反弹
                    if (hitResult instanceof EntityHitResultTimed hitResultTimed) {
                        if (!this.level().isClientSide) {
                            Vector3f currentVel = getAngularVelocity();
                            Vector3f newVel = new Vector3f(currentVel).negate();
                            setAngularVelocity(newVel);
                            // 发送强制同步数据包，确保客户端立即收到更新
                            QuaternionSyncPacket packet = new QuaternionSyncPacket(getId(), orientation.x, orientation.y, orientation.z, orientation.w, newVel.x, newVel.y, newVel.z);
                            PacketDistributor.sendToPlayersTrackingEntity(this, packet);
                        }
                    }
                }
            }
        }
    }


    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    public Vector3f getAngularVelocity() {
        return this.entityData.get(ANGULAR_VELOCITY_VECTOR);
    }

    public void setAngularVelocity(Vector3f angularVelocity) {
        this.entityData.set(ANGULAR_VELOCITY_VECTOR, angularVelocity);
    }

    @Override
    public PhysicsMotionModel getMotionModel() {
        return motion;
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGULAR_VELOCITY_VECTOR, new Vector3f());
        builder.define(ORIENTATION_DATA, new Quaternionf(0, 0, 0, 1));
    }

    public void setOrientation(Quaternionf orientation) {
        this.orientation = orientation;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        motion.addAdditionalSaveData(tag);
        if (targetPos != null) tag.put("targetPos", this.newDoubleList(targetPos.x, targetPos.y, targetPos.z));
        // 保存角速度向量
        Vector3f angularVel = getAngularVelocity();
        if (angularVel != null) {
            tag.put("angularVelocity", this.newFloatList(angularVel.x, angularVel.y, angularVel.z));
        }
        if (orientation != null) {
            tag.put("orientation", this.newFloatList(orientation.x, orientation.y, orientation.z, orientation.w));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        log.info("readAdditionalSaveData 开始：{}", orientation);
        super.readAdditionalSaveData(tag);
        this.motion = PhysicsMotionModel.fromTag(tag);
        if (tag.contains("targetPos")) {
            ListTag list = tag.getList("targetPos", 6);
            this.targetPos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        if (tag.contains("angularVelocity")) {
            ListTag list = tag.getList("angularVelocity", 5);
            this.setAngularVelocity(new Vector3f(list.getFloat(0), list.getFloat(1), list.getFloat(2)));
        }
        if (tag.contains("orientation")) {
            ListTag list = tag.getList("orientation", 5);
            this.orientation.set(list.getFloat(0), list.getFloat(1), list.getFloat(2), list.getFloat(3));
        } else {
            orientation.rotationYXZ(-getYRot() * Mth.DEG_TO_RAD, getXRot() * Mth.DEG_TO_RAD, (tag.contains("roll") ? tag.getFloat("roll") : 0f) * Mth.DEG_TO_RAD);
        }
        // 同步 previousOrientation，避免 NullPointerException
        this.previousOrientation.set(this.orientation);
        syncEulerAnglesFromQuaternion();
        refreshDimensions();
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        motion.writeSpawnData(buf);
        buf.writeBoolean(this.targetPos != null);
        if (this.targetPos != null) {
            buf.writeDouble(this.targetPos.x);
            buf.writeDouble(this.targetPos.y);
            buf.writeDouble(this.targetPos.z);
        }
        // 发送角速度向量
        Vector3f angularVel = getAngularVelocity();
        buf.writeFloat(angularVel.x);
        buf.writeFloat(angularVel.y);
        buf.writeFloat(angularVel.z);
        buf.writeFloat(orientation.x);
        buf.writeFloat(orientation.y);
        buf.writeFloat(orientation.z);
        buf.writeFloat(orientation.w);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.motion = PhysicsMotionModel.fromBuf(buf);
        boolean hasTargetPos = buf.readBoolean();
        if (hasTargetPos) {
            this.targetPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
        setAngularVelocity(new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()));
        orientation.set(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        // 同步 previousOrientation，避免 NullPointerException
        this.previousOrientation.set(this.orientation);
        // 加载后立即同步 OBB 和欧拉角
        syncEulerAnglesFromQuaternion();
        refreshDimensions();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}