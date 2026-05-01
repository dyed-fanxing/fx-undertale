package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.entity.ColoredAttacker;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.lib.entity.capability.Growable;
import com.fanxing.lib.util.CurvesUtils;
import com.fanxing.lib.util.RotUtils;
import com.fanxing.lib.util.collsion.RayCCDUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * 地面移动骨骼 - 作为环境危险实体而非弹射物
 *
 * @author FanXing
 * @since 2025-10-06 21:18
 */
public class GroundBone extends AbstractBone<GroundBone> implements Growable, ColoredAttacker, IEntityWithComplexSpawn, GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int delay;          // 整体延迟
    private int shotDelay;      // 射击延迟，在整体延迟结束后开始
    private float speed;

    public GroundBone(EntityType<? extends GroundBone> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public GroundBone(Level level, LivingEntity owner, float scale, float growScale, int lifetime, float damage) {
        super(EntityTypes.GROUND_BONE.get(), level, owner, scale, growScale, lifetime, damage);
        this.refreshDimensions();
    }
    public GroundBone delay(int delay) {
        this.delay = delay;
        return this;
    }
    public GroundBone delayShoot(int shotDelay, float speed) {
        this.shotDelay = shotDelay;
        this.speed = speed;
        return this;
    }

    public void shoot(Vec3 velocity) {
        this.setDeltaMovement(velocity);
        RotUtils.lookVec(this, velocity);
    }


    @Override
    public float getGrowProgress(float partialTick) {
        if (delay >= -lifetime && delay < 0) {
            if(holdTimeScale == -3f) return CurvesUtils.powerFallEaseOut(partialTick,10);
            if(holdTimeScale == -2f) return (Mth.sin((-delay+partialTick)*0.2F)+1)/2;
            else if (holdTimeScale == -1f) return Mth.sin(((-delay + partialTick) / lifetime) * Mth.PI);
            else return CurvesUtils.riseHoldFallBezier((-delay + partialTick) / lifetime, holdTimeScale, 0.8f);
        }else return 0f;
    }


    @Override
    public void tick() {
        delay--;
        if (delay <= 0 ) {
            shotDelay--;
            this.refreshDimensions();
            super.tick();
            if (shotDelay == 0 && !this.level().isClientSide) {
                this.setDeltaMovement(this.getLookAngle().scale(speed));
                this.hasImpulse = true;
            }
            if (delay < -lifetime) {
                this.discard();
            }
        }
    }


    @Override
    protected List<EntityHitResult> getEntityHitResults(Vec3 to) {
        if (this.speed == 0 && this.getDeltaMovement().lengthSqr() == 0) return RayCCDUtils.getHitResultsOnStill(this.level(),LivingEntity.class,this.getBoundingBox(),this::canHitEntity);
        else return super.getEntityHitResults(to);
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
        entity.hurt(damageSource, damage);
    }

    // 不需要持久化，攻击物不需要
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("delay", delay);
        tag.putFloat("speed", speed);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("delay")) {
            this.delay = tag.getInt("delay");
        }
        if (tag.contains("speed")) {
            this.speed = tag.getFloat("speed");
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.writeSpawnData(buf);
        buf.writeFloat(this.speed);
        buf.writeInt(this.delay);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buf) {
        super.readSpawnData(buf);
        this.speed = buf.readFloat();
        this.delay = buf.readInt();
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