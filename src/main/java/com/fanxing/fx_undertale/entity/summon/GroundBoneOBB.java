package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.capability.QuaternionRotatable;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.fanxing.fx_undertale.utils.GravityUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 地面骨头 - 支持 OBB 碰撞检测
 * @author FanXing
 * @since 2025-08-18
 */
public class GroundBoneOBB extends AbstractBone<GroundBoneOBB> implements QuaternionRotatable {

    private static final Logger log = LoggerFactory.getLogger(GroundBoneOBB.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int delay = 20;

    private Quaternionf orientation = new Quaternionf();
    public GroundBoneOBB(EntityType<? extends GroundBoneOBB> type, Level level) {
        super(type, level);
    }
    public GroundBoneOBB(Level level, LivingEntity owner, float scale, float growScale, float damage, int lifetime, int delay) {
        super(EntityTypes.GROUND_BONE_OBB.get(), level, owner,scale,growScale,lifetime,damage);
        this.delay = delay;
        refreshDimensions();
    }
    public GroundBoneOBB orientation(float yaw, float pitch, float roll) {
        // 取负数为了对齐MC的坐标系
        orientation.rotateYXZ(-yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD, roll * Mth.DEG_TO_RAD);  // 创建副本
        return this;
    }

    public GroundBoneOBB orientation(Quaternionf orientation) {
        this.orientation.set(orientation);
        return this;
    }
    @Override
    public GroundBoneOBB gravity(Direction gravity) {
        Quaternionf gravityRotation = GravityUtils.getLocalToWorldF(gravity);
        orientation = gravityRotation.mul(orientation, new Quaternionf());
        return this;
    }

    @Override
    public float getGrowProgress(float partialTick) {
        if (delay >= -lifetime && delay < 0) {
            if (holdTimeScale == -1f) { // 特殊值-1f使用sin曲线，这个比较符合重力猛摔后的骨刺刺出并返回的效果
                return Mth.sin(((-delay + partialTick)/ lifetime)*Mth.PI);
            } else {
                return CurvesUtils.riseHoldFallBezier((-delay + partialTick) / lifetime, holdTimeScale, 0.8f);
            }
        }
        return 1f;
    }

    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        updateOBB();
    }

    @Override
    public void setPos(double p_20210_, double p_20211_, double p_20212_) {
        super.setPos(p_20210_, p_20211_, p_20212_);
    }

    // ========== 核心逻辑 ==========
    @Override
    public void tick() {
        super.tick();
        delay--;
        // 生命周期内（从延迟结束到消失）
        if (delay >= -lifetime && delay <= 0) {
            this.refreshDimensions();
            if (!this.level().isClientSide) {
                for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class,this.obb.getBoundingAABB(), this::canHitEntity)) {
                    onHitEntity(new EntityHitResult(target));
                }
            }
        }
        // 生命周期结束
        else if (delay < -lifetime) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && this.obb.intersects(entity.getBoundingBox());
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
        Entity owner = getOwner();
        DamageSource damageSource;
        if (owner instanceof Sans) {
            damageSource = damageSources().source(DamageTypes.FRAME, this, owner);
        } else if (owner != null) {
            damageSource = damageSources().indirectMagic(this, owner);
        } else {
            damageSource = damageSources().magic();
        }
        entity.hurt(damageSource, damage);
    }

    @Override
    public Quaternionf getLerpOrientation(float partialTick) {
        return orientation;
    }

    @Override
    public Quaternionf getOrientation() {
        return orientation;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return 0f;
    }
    public int getDelay() {
        return delay;
    }


    // ========== 数据同步 ==========
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("delay", delay);
        if (orientation != null) {
            tag.put("orientation", this.newFloatList(orientation.x, orientation.y, orientation.z, orientation.w));
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("delay")) this.delay = tag.getInt("delay");
        if (tag.contains("orientation")) {
            ListTag list = tag.getList("orientation", 5);
            this.orientation.set(list.getFloat(0), list.getFloat(1), list.getFloat(2), list.getFloat(3));
        } else {
            orientation.rotationYXZ(-getYRot() * Mth.DEG_TO_RAD, getXRot() * Mth.DEG_TO_RAD, (tag.contains("roll") ? tag.getFloat("roll") : 0f) * Mth.DEG_TO_RAD);
        }
        refreshDimensions();
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeInt(this.delay);
        buf.writeFloat(orientation.x);
        buf.writeFloat(orientation.y);
        buf.writeFloat(orientation.z);
        buf.writeFloat(orientation.w);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.delay = buf.readInt();
        orientation.set(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
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