package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.common.phys.CollisionDeflection;
import com.fanxing.fx_undertale.common.phys.motion.PhysicsMotionModel;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.capability.OBBRotationCollider;
import com.fanxing.fx_undertale.entity.capability.Rollable;
import com.fanxing.fx_undertale.entity.capability.SyncablePhysicsMotion;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.fanxing.fx_undertale.utils.collsion.EntityHitResultTimed;
import com.fanxing.fx_undertale.utils.collsion.OBBCCDUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
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

public class RotationBone extends AbstractBone<RotationBone> implements Rollable, OBBRotationCollider, SyncablePhysicsMotion, GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(RotationBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float roll;
    public Direction.Axis rotateAxis = Direction.Axis.Y;

    private PhysicsMotionModel motion = new PhysicsMotionModel();
    private Vec3 targetPos;
    public float rollO;

    private static final EntityDataAccessor<Float> ANGULAR_VELOCITY = SynchedEntityData.defineId(RotationBone.class,EntityDataSerializers.FLOAT);
    public RotationBone(EntityType<? extends AbstractBone<RotationBone>> type, Level level) {
        super(type, level);
    }
    public RotationBone(Level level, LivingEntity owner, float scale, float growScale, int lifetime, float damage) {
        super(EntityTypes.ROTATION_BONE.get(), level,owner,scale,growScale,lifetime,damage);
    }

    @Override
    public RotationBone gravity(Direction gravity) {
        // 1. 当前姿态的四元数（YXZ 顺序）
        Quaternionf qCurrent = new Quaternionf().rotationYXZ(this.getYRot()*Mth.DEG_TO_RAD, this.getXRot()*Mth.DEG_TO_RAD, roll*Mth.DEG_TO_RAD);
        // 3. 新姿态 = qGravity * qCurrent （左乘：先应用当前姿态，再整体旋转到新重力坐标系），转回欧拉角
        Vector3f euler = Gravity.getRotation(gravity).mul(qCurrent).getEulerAnglesYXZ(new Vector3f());
        setYRot(euler.y * Mth.RAD_TO_DEG);
        setXRot(euler.x * Mth.RAD_TO_DEG);
        roll = euler.z * Mth.RAD_TO_DEG;
        rotateAxis = Gravity.getRotateAxis(gravity);
        return this;
    }

    public RotationBone roll(float roll) {
        this.roll = roll;
        return this;
    }
    public RotationBone angularVelocity(float angularVelocity){
        this.entityData.set(ANGULAR_VELOCITY,angularVelocity);
        return this;
    }
    public RotationBone rotateAxis( Direction.Axis rotateAxis){
        this.rotateAxis = rotateAxis;
        return this;
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
        if(tickCount >= lifetime) {
            this.discard();
        }
        this.setDeltaMovement(motion.update(this.position(), this.getDeltaMovement(), targetPos, null, 0.05f));
        // 1. 碰撞检测 ，检测到了，速度取反
        super.tick();
        rollO = roll;
        //key 在以欧拉角 Y X Z的顺序下这样执行， 四元数左乘为绕世界轴旋转，右乘为绕局部轴旋转，
//        下方注释的方法理论应该是对的，但是在pitch=90/-90，提取欧拉角时，导致roll丢失自由度，无法旋转roll
//        Quaternionf q = new Quaternionf().rotationYXZ(getYRot() * Mth.DEG_TO_RAD,getXRot() * Mth.DEG_TO_RAD,roll * Mth.DEG_TO_RAD).rotateZ(angularVelocity * Mth.DEG_TO_RAD);
//        Vector3f euler = q.getEulerAnglesYXZ(new Vector3f());
//        log.info("提取的四元数欧拉角：{}",euler);
//        setYRot(euler.y * Mth.RAD_TO_DEG);
//        setXRot(euler.x * Mth.RAD_TO_DEG);
//        roll = euler.z * Mth.RAD_TO_DEG;
//        log.info("roll before: {}, after: {}", rollO, roll);


        // 绕世界轴旋转的增量四元数
//        Quaternionf qDelta = new Quaternionf().rotationAxis(angularVelocity * Mth.DEG_TO_RAD, Gravity.getRotateAxis(rotateAxis).toVector3f());
//        Quaternionf q = qDelta.mul(new Quaternionf().rotationYXZ(getYRot() * Mth.DEG_TO_RAD, getXRot() * Mth.DEG_TO_RAD, roll * Mth.DEG_TO_RAD));
//        // 转回欧拉角并设置
//        Vector3f euler = q.getEulerAnglesYXZ(new Vector3f());
//        setYRot(euler.y() * Mth.RAD_TO_DEG);
//        setXRot(euler.x() * Mth.RAD_TO_DEG);
//        roll = euler.z() * Mth.RAD_TO_DEG;
        //2. 使用取反后的角速度应用旋转
        switch (rotateAxis){
            case Y -> this.setYRot(getYRot() + getAngularVelocity());
            case X -> {
                Quaternionf q = new Quaternionf().rotationYXZ(getYRot() * Mth.DEG_TO_RAD, getXRot() * Mth.DEG_TO_RAD, roll * Mth.DEG_TO_RAD);
                Quaternionf qDelta = new Quaternionf().rotationX(getAngularVelocity() * Mth.DEG_TO_RAD);
                q = qDelta.mul(q); // 左乘：绕世界 X 轴
                Vector3f euler = q.getEulerAnglesYXZ(new Vector3f());
                setYRot(euler.y() * Mth.RAD_TO_DEG);
                setXRot(euler.x() * Mth.RAD_TO_DEG);
                roll = euler.z() * Mth.RAD_TO_DEG;
            }
            case Z -> {
                Quaternionf q = new Quaternionf().rotationYXZ(getYRot() * Mth.DEG_TO_RAD, getXRot() * Mth.DEG_TO_RAD, roll * Mth.DEG_TO_RAD);
                q = new Quaternionf().rotationZ(getAngularVelocity() * Mth.DEG_TO_RAD).mul(q); // 关键修改：世界 Z 轴旋转左乘
                Vector3f euler = q.getEulerAnglesYXZ(new Vector3f());
                setYRot(euler.y() * Mth.RAD_TO_DEG);
                setXRot(euler.x() * Mth.RAD_TO_DEG);
                roll = euler.z() * Mth.RAD_TO_DEG;
            }
        }
        refreshDimensions();
    }


    @Override
    protected List<EntityHitResult> getEntityHitResults(Vec3 to) {
        if (obb == null) {
            return super.getEntityHitResults(to);
        } else {
            boolean noMove = getDeltaMovement().lengthSqr() == 0;
            Vec3 rotateAxis = getRotateAxis();
            if (noMove) return new ArrayList<>(OBBCCDUtils.getEntityHitResultsOnlyOnRotation(obb, getAngularVelocity() * Mth.DEG_TO_RAD, rotateAxis, this.position(), this.level(), this, this::canHitEntity));
            else return new ArrayList<>(OBBCCDUtils.getEntityHitResults(obb, this.getDeltaMovement(), getAngularVelocity() * Mth.DEG_TO_RAD, rotateAxis, this.position(),this.level(), this, this::canHitEntity));
        }
    }

    @Override
    void updateRotation(Vec3 velocity) {
    }

    @Override
    protected BlockHitResult getBlockHitResult() {
        return BlockHitResult.miss(null,null,null);
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
                damageSource = damageSources().source(DamageTypes.KARMA_BLOCKABLE, this, owner);
            } else {
                damageSource = this.damageSources().mobProjectile(this, (LivingEntity) owner);
            }
            if (!livingTarget.hurt(damageSource, damage)) {
                // 格挡反弹
                if (livingTarget.isBlocking()) {
                    if(hitResult instanceof EntityHitResultTimed hitResultTimed){
                        if(!this.level().isClientSide){
                            setAngularVelocity(getAngularVelocity()*-1);
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

    @Override
    public float getRoll() {
        return roll;
    }
    @Override
    public float getRollO() {
        return rollO;
    }
    public float getAngularVelocity(){
        return this.entityData.get(ANGULAR_VELOCITY);
    }
    public void setAngularVelocity(float angularVelocity){
        this.entityData.set(ANGULAR_VELOCITY, angularVelocity);
    }

    @Override
    public Vec3 getRotateAxis() {
        return Gravity.getRotateAxis(this.rotateAxis);
    }

    @Override
    public PhysicsMotionModel getMotionModel() {
        return motion;
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGULAR_VELOCITY, 0f);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        motion.addAdditionalSaveData(tag);
        if (targetPos != null) tag.put("targetPos", this.newDoubleList(targetPos.x, targetPos.y, targetPos.z));
        tag.putFloat("angularVelocity", getAngularVelocity());
        tag.putFloat("roll", roll);
        tag.putString("rotateAxis", rotateAxis.getName());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.motion = PhysicsMotionModel.fromTag(tag);
        if (tag.contains("targetPos")) {
            ListTag list = tag.getList("targetPos", 6);
            this.targetPos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        setAngularVelocity(tag.getFloat("angularVelocity"));
//        this.angularVelocity = tag.getFloat("angularVelocity");
        if (tag.contains("roll")) this.roll = tag.getFloat("roll");
        if (tag.contains("rotateAxis")) this.rotateAxis = Direction.Axis.byName(tag.getString("rotateAxis"));
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
//        buf.writeFloat(this.angularVelocity);
        buf.writeFloat(getAngularVelocity());
        buf.writeFloat(this.roll);
        buf.writeEnum(this.rotateAxis);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.motion = PhysicsMotionModel.fromBuf(buf);
        boolean hasTargetPos = buf.readBoolean();
        if (hasTargetPos) {
            this.targetPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
//        this.angularVelocity = buf.readFloat();
        setAngularVelocity(buf.readFloat());
        this.roll = buf.readFloat();
        this.rotateAxis = buf.readEnum(Direction.Axis.class);
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