package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.common.phys.motion.PhysicsMotionModel;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.capability.Rollable;
import com.fanxing.fx_undertale.entity.capability.SyncablePhysicsMotion;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.fanxing.fx_undertale.utils.collsion.OBBCCDUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.ArrayList;
import java.util.List;

public class RotationBone extends AbstractBone<RotationBone> implements Rollable, SyncablePhysicsMotion, GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(RotationBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float roll;
    private Direction.Axis rotateAxis = Direction.Axis.Y;
    private float angularVelocity = 0f;
    private PhysicsMotionModel motion = new PhysicsMotionModel();
    private Vec3 targetPos;

    public float rollO;

    public RotationBone(EntityType<? extends AbstractBone<RotationBone>> type, Level level) {
        super(type, level);
    }
    public RotationBone(Level level, LivingEntity owner, float scale, float growScale, int lifetime, float damage) {
        super(EntityTypes.ROTATION_BONE.get(), level,owner,scale,growScale,lifetime,damage);
    }

    public RotationBone roll(float roll) {
        this.roll = roll;
        return this;
    }
    public RotationBone angularVelocity(float angularVelocity){
        this.angularVelocity = angularVelocity;
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
        refreshDimensions();
        super.tick();
        rollO = roll;
        switch (rotateAxis){
            case X -> this.setXRot(getXRot() + angularVelocity);
            case Y -> this.setYRot(getYRot() + angularVelocity);
            case Z -> this.roll += angularVelocity;
        }
    }


    @Override
    protected List<EntityHitResult> getEntityHitResults(Vec3 to) {
        if (obb == null) {
            return super.getEntityHitResults(to);
        } else {
            boolean noMove = getDeltaMovement().lengthSqr() == 0;
            Vec3 rotateAxis = switch (this.rotateAxis){
                case X -> new Vec3(1,0,0);
                case Y -> new Vec3(0,1,0);
                case Z -> new Vec3(0,0,1);
            };
            if (noMove) return new ArrayList<>(OBBCCDUtils.getEntityHitResultsOnlyOnRotation(obb, angularVelocity * Mth.DEG_TO_RAD, rotateAxis, this.position(), this.level(), this, this::canHitEntity));
            else return new ArrayList<>(OBBCCDUtils.getEntityHitResults(obb, this.getDeltaMovement(), angularVelocity * Mth.DEG_TO_RAD, rotateAxis, this.position(),this.level(), this, this::canHitEntity));
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
    public PhysicsMotionModel getMotionModel() {
        return motion;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        motion.addAdditionalSaveData(tag);
        if (targetPos != null) tag.put("targetPos", this.newDoubleList(targetPos.x, targetPos.y, targetPos.z));
        tag.putFloat("angularVelocity", angularVelocity);
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
        this.angularVelocity = tag.getFloat("angularVelocity");
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
        buf.writeFloat(this.angularVelocity);
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
        this.angularVelocity = buf.readFloat();
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