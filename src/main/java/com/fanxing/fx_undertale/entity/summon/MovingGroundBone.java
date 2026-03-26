package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.entity.capability.Growable;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.entity.ColoredAttacker;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 地面移动骨骼 - 作为环境危险实体而非弹射物
 *
 * @author FanXing
 * @since 2025-10-06 21:18
 */
public class MovingGroundBone extends AbstractMovingSummons implements Growable,ColoredAttacker,IEntityWithComplexSpawn, GeoEntity{
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float scale = 1.0f; // 整体缩放
    private float growScale = 1.0f; // 在基于整体缩放的基础上的高度缩放

    private int delay = 10;
    private float speed = 1.0f;

    private float damage = 1.0f;
    private ColorAttack colorAttack = ColorAttack.WHITE;


    public MovingGroundBone(EntityType<? extends MovingGroundBone> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }
    /**
     * 延迟移动，无需传递移动向量
     */
    public MovingGroundBone(Level level, LivingEntity owner,float scale, float growScale,  int delay,float speed, float damage, ColorAttack colorAttack) {
        super(EntityTypes.MOVING_GROUND_BONE.get(), level,owner);
        this.setNoGravity(true);
        this.scale = scale;
        this.growScale = growScale;
        this.delay = delay;
        this.damage = damage;
        this.speed = speed;
        this.colorAttack = colorAttack;
        refreshDimensions();
    }
    /**
     * 立即移动，需要传递移动向量，无需相对位置
     * velocity 移动向量，单位化的
     */
    public MovingGroundBone(Level level, LivingEntity owner,float scale, float growScale, float speed,Vec3 velocity, float damage, ColorAttack colorAttack) {
        super(EntityTypes.MOVING_GROUND_BONE.get(), level,owner);
        this.setNoGravity(true);
        this.scale = scale;
        this.growScale = growScale;
        this.delay = 0;
        this.setDeltaMovement(velocity.scale(speed));
        RotUtils.lookVec(this, velocity);
        this.damage = damage;
        this.speed = speed;
        this.colorAttack = colorAttack;
        refreshDimensions();
    }


    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        // 先整体，再高度缩放
        return super.getDimensions(pose).scale(scale).scale(1f, growScale);
    }


    @Override
    public void tick() {
        super.tick();
        delay--;
        if (delay < 0) {
            super.tick();
        }
        if (delay == 0 && !this.level().isClientSide) {
            this.setDeltaMovement(this.getLookAngle().scale(speed));
            this.hasImpulse = true;
        }
    }
    @Override
    public float getGrowProgress(float partialTick) {
        return 0;
    }
    /**
     * 判断是否可命中实体
     */
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity instanceof LivingEntity && entity.isAlive() && colorAttack.canHitEntity(entity);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {

    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
        DamageSource damageSource;
        if (owner instanceof Sans) {
            damageSource = this.damageSources().source(DamageTypes.FRAME, this, owner);
        } else {
            damageSource = this.damageSources().indirectMagic(this, owner);
        }
        boolean hurtSuccess = entity.hurt(damageSource, damage);
        // 可根据伤害结果执行额外逻辑
        if (hurtSuccess) {
            // 例如：播放命中音效、生成粒子等
            // 如果是非穿透攻击，可以在这里销毁自己
            // this.discard();
        }

    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putFloat("scale", scale);
        tag.putFloat("growScale", growScale);
        tag.putInt("delay", delay);
        tag.putFloat("speed", speed);
        tag.putInt("color", this.colorAttack.getColor());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.contains("delay")) {
            this.delay = tag.getInt("delay");
        }
        if (tag.contains("speed")) {
            this.speed = tag.getFloat("speed");
        }
        if (tag.contains("scale")) {
            this.scale = tag.getFloat("scale");
        }
        if (tag.contains("growScale")) {
            this.growScale = tag.getFloat("growScale");
        }
        if (tag.contains("color")) {
            this.colorAttack = ColorAttack.of(tag.getInt("color"));
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.speed);
        buf.writeInt(this.delay);
        buf.writeFloat(scale);
        buf.writeFloat(this.growScale);
        buf.writeInt(this.colorAttack.getColor());
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        this.speed = buf.readFloat();
        this.delay = buf.readInt();
        this.scale  = buf.readFloat();
        this.growScale = buf.readFloat();
        this.colorAttack = ColorAttack.of(buf.readInt());
        refreshDimensions();
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    public float getScale() {
        return scale;
    }
    public float getGrowScale() {
        return growScale;
    }

    @Override
    public int getColor() {
        return colorAttack.getColor();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


}