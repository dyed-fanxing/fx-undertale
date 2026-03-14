package com.fanxing.fx_undertale.entity.projectile;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.common.phys.CollisionDeflection;
import com.fanxing.fx_undertale.common.phys.motion.AbstractPhysicsMotionModel;
import com.fanxing.fx_undertale.common.phys.motion.SpringMotionModel;
import com.fanxing.fx_undertale.entity.ISyncablePhysicsMotion;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.registry.EntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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

/**
 * 简谐振荡弹射物（向心力模式）
 * 使用 OscillationMotion 模型，实现指向目标的往复振荡。
 */
public class RotationBone extends AbstractPenetrableProjectile implements ISyncablePhysicsMotion,GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(RotationBone.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private AbstractPhysicsMotionModel motion;

    private Vec3 targetPos;
    // 伤害
    private float damage = 1.0f;
    public float spinDir = 1f;
    public RotationBone(EntityType<? extends RotationBone> type, Level level) {
        super(type, level);
    }
    public RotationBone(Level level, LivingEntity owner, float damage) {
        this(EntityTypes.ROTATION_BONE.get(), level);
        setOwner(owner);
        this.damage = damage;
        this.setNoGravity(true);
    }
    public void startMotion(AbstractPhysicsMotionModel motionModel,Vec3 targetPos, float initSpeed,float spinDir) {
        this.motion = motionModel;
        this.accelerationPower = 0;
        this.targetPos = targetPos;
        Vec3 toTarget = targetPos.subtract(this.getX(),this.getY(0.5f),this.getZ());
        this.shoot(toTarget.x,toTarget.y,toTarget.z, initSpeed,0);
        this.spinDir = spinDir;
    }

    @Override
    public void tick() {
        // 先更新运动（注意修正 deltaTime，见后文）
        this.setDeltaMovement(motion.update(this.position(), this.getDeltaMovement(),targetPos, null, 0.05f));  // 0.05f = 固定 tick 时长
        super.tick();
        if (motion instanceof SpringMotionModel spring) {
            // 旋转速度与当前总能量成正比，系数可调
            this.setYRot(this.getYRot() + spring.getTotalEnergy() * 45F*spinDir);
        }
    }
    @Override
    protected void updateRotation() {
    }

    // ---------- 碰撞处理 ----------
    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        CollisionDeflection.slideDeflect(this,result.getDirection().getNormal(),this.level().random,0.6f,0f);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        Entity target = result.getEntity();
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
                if (livingTarget.isBlocking()) {
                    this.setNoGravity(false);
                    this.deflect(CollisionDeflection.MIRROR_DEFLECT, target, this.getOwner(), false);
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.9f));
                }
            }
        }
    }

    @Override
    protected float getInertia() {
        return 0.995f; // 速度不由惯性控制，由阻尼控制
    }

    @Override
    protected float getLiquidInertia() {
        return 0.88888F;
    }

    @Override
    public AbstractPhysicsMotionModel getMotionModel() {
        return motion;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        if (motion != null) {
            motion.addAdditionalSaveData(tag);
        }
        if(targetPos != null) {
            tag.put("targetPos",this.newDoubleList(targetPos.x,targetPos.y,targetPos.z));
        }
        tag.putFloat("spinDir", spinDir);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("damage");
        this.motion = AbstractPhysicsMotionModel.fromTag(tag);
        if(tag.contains("targetPos")) {
            ListTag list = tag.getList("targetPos", 6);
            this.targetPos = new Vec3(list.getDouble(0),list.getDouble(1),list.getDouble(2));
        }
        this.spinDir = tag.getFloat("spinDir");
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(this.damage);
        motion.writeSpawnData(buf);
        buf.writeBoolean(this.targetPos != null);
        if(this.targetPos != null) {
            buf.writeDouble(this.targetPos.x);
            buf.writeDouble(this.targetPos.y);
            buf.writeDouble(this.targetPos.z);
        }
        buf.writeFloat(this.spinDir);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.damage = buf.readFloat();
        this.motion = AbstractPhysicsMotionModel.fromBuf(buf);
        boolean hasTargetPos = buf.readBoolean();
        if(hasTargetPos) {
            this.targetPos = new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble());
        }
        this.spinDir = buf.readFloat();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}