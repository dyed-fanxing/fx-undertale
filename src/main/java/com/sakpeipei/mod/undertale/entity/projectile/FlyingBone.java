package com.sakpeipei.mod.undertale.entity.projectile;

import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

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
        if (delay > 0) {
            delay--;
            if(target == null && targetPosition != null) {
                if(delay == 0){
                    this.shoot(targetPosition.x - this.getX(), targetPosition.y - this.getY(), targetPosition.z - this.getZ(), damage, 0F);
                }
            }else{
                this.lookAt(EntityAnchorArgument.Anchor.FEET,target.position());
                this.moveTo(getOwner().getEyePosition().add(relativePosition));
                if(delay == 0){
                    this.shoot(target.getX() - this.getX(), target.getY(0.5f) - this.getY(), target.getZ() - this.getZ(), damage, 0F);
                }
            }
            return;
        }
        if(this.moveDist > 64){
            this.discard();
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
