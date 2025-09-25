package com.sakpeipei.mod.undertale.entity.projectile;

import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 18:44
 */
public class FlyingBone extends AbstractHurtingProjectile implements GeoEntity, GeoAnimatable, Targeting {

    private float damage;
    private int delay = 0;
    private LivingEntity target;
    private Vec3 relativePosition;
    private Vec3 targetPosition;

    public FlyingBone(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public FlyingBone(EntityType<? extends AbstractHurtingProjectile> type,  Level level,LivingEntity owner,float damage) {
        this(type, level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
    }


    @Override
    public void tick() {
//        if (delay > 0) {
//            delay--;
//            if(target == null && targetPosition != null) {
//                if(delay == 0){
//                    this.shoot(targetPosition.x - this.getX(), targetPosition.y - this.getY(), targetPosition.z - this.getZ(), damage, 0F);
//                }
//            }else{
//                RotUtils.setLookAtByShootRot(this,target);
//                this.moveTo(getOwner().getEyePosition().add(relativePosition));
//            }
//            return;
//        }
        if(!this.level().isClientSide){
            Entity owner = getOwner();
            this.moveTo(owner.getEyePosition().add(relativePosition),owner.getYRot(),owner.getXRot());
            RotUtils.setLookAtByShootRot(this,target);
            if(this.moveDist > 64){
                this.discard();
            }
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        // 设置伤害逻辑
        if (target instanceof LivingEntity livingTarget) {
            DamageSource damageSource;
            if(owner instanceof Sans ){
                damageSource = damageSources().source(DamageTypes.FRAME,this,owner);
            }else{
                damageSource = this.damageSources().mobProjectile(this, (LivingEntity) owner);
            }
            livingTarget.hurt(damageSource, damage);
        }
    }

    public void delayShoot(int delay,LivingEntity target,Vec3 relativePosition){
        this.target = target;
        this.relativePosition = relativePosition;
        this.delay = delay;
    }
    public void delayShoot(int delay,Vec3 targetPosition){
        this.targetPosition = targetPosition;
        this.delay = delay;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if(target != null){
            tag.putUUID("targetUUID", target.getUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        UUID targetUUID = tag.getUUID("targetUUID");
        if (this.level() instanceof ServerLevel level) {
            this.target = (LivingEntity) level.getEntity(targetUUID);
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return GeckoLibUtil.createInstanceCache(this);
    }

    @Override
    public @Nullable LivingEntity getTarget() {
        return target;
    }
}
