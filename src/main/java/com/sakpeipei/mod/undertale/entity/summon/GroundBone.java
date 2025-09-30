package com.sakpeipei.mod.undertale.entity.summon;

import com.sakpeipei.mod.undertale.data.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.mechanism.ColorAttack;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 18:44
 */
public class GroundBone extends Entity implements Targeting,GeoEntity, GeoAnimatable {
    private ColorAttack colorAttack;

    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;
    private float damage;
    private boolean isMove; //标记是否为移动地面骨头
    private int delay = 0;
    private LivingEntity target;
    private int lifetime = 20;

    public GroundBone(EntityType<? extends GroundBone> type, Level level) {
        super(type, level);
    }
    public GroundBone(EntityType<? extends GroundBone> type, Level level, LivingEntity owner, float damage,boolean isMove) {
        this(type, level,owner,damage,isMove,ColorAttack.WHITE);
    }
    public GroundBone(EntityType<? extends GroundBone> type, Level level, LivingEntity owner, float damage,boolean isMove,ColorAttack colorAttack) {
        this(type, level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
        this.isMove = isMove;
        this.colorAttack = colorAttack;
    }

    @Override
    public void tick() {
        super.tick();
        if(isMove){
            Entity entity = this.getOwner();
            if (this.level().isClientSide || (entity == null || !entity.isRemoved()) && !this.level().getChunkAt(this.blockPosition()).isEmpty()) {
                super.tick();
                HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity, ClipContext.Block.COLLIDER);
                if(hitresult.getType() == HitResult.Type.BLOCK) {
                    this.onHitBlick((BlockHitResult)hitresult);
                }else if(hitresult.getType() == HitResult.Type.ENTITY){
                    onHitEntity(((EntityHitResult)hitresult).getEntity());
                }
                this.checkInsideBlocks();
                Vec3 vec3 = this.getDeltaMovement();
                double d0 = this.getX() + vec3.x;
                double d1 = this.getY() + vec3.y;
                double d2 = this.getZ() + vec3.z;
                ProjectileUtil.rotateTowardsMovement(this, 0.2F);
                float f;
                if (!this.isInWater()) {
                    f = this.getInertia();
                } else {
                    for(int i = 0; i < 4; ++i) {
                        float f1 = 0.25F;
                        this.level().addParticle(ParticleTypes.BUBBLE, d0 - vec3.x * (double)0.25F, d1 - vec3.y * (double)0.25F, d2 - vec3.z * (double)0.25F, vec3.x, vec3.y, vec3.z);
                    }

                    f = this.getLiquidInertia();
                }

                this.setDeltaMovement(vec3.scale(f));
                ParticleOptions particleoptions = this.getTrailParticle();
                if (particleoptions != null) {
                    this.level().addParticle(particleoptions, d0, d1 + (double)0.5F, d2, (double)0.0F, (double)0.0F, (double)0.0F);
                }
                this.setPos(d0, d1, d2);
            } else {
                this.discard();
            }
        }else{
            if(tickCount > lifetime) {
                this.discard();
                return;
            }
            Vec3 deltaMovement = getDeltaMovement();
            setDeltaMovement(deltaMovement.add(0,this.tickCount * this.tickCount * 0.1f,0));
            for(LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2, 0.0F, 0.2), this::canHitEntity)) {
                onHitEntity(target);
            }
        }
    }

    private void onHitBlick(BlockHitResult hitResult) {
        BlockState blockstate = this.level().getBlockState(hitResult.getBlockPos());

    }

    private boolean canHitEntity(Entity entity){
        return entity.isAlive() && entity != getOwner() && colorAttack.canHitEntity(entity);
    }
    private void onHitEntity(Entity entity) {
        LivingEntity owner = getOwner();
        if(owner == null){
            entity.hurt(damageSources().source(DamageTypes.FRAME,this),damage);
        }else{
            if(owner instanceof Sans){
                entity.hurt(damageSources().source(DamageTypes.FRAME,owner,this),damage);
            }else{
                entity.hurt(damageSources().indirectMagic(owner,this),damage);
            }
        }
    }
    public void delayShoot(int delay, @NotNull LivingEntity target, Vec3 relation) {
        this.delay = delay;
        this.target = target;

    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel)this.level()).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            }
        }
        return this.owner;
    }
    @Override
    public @Nullable LivingEntity getTarget() {
        return target;
    }
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }
    protected float getInertia() {
        return 0.95F;
    }

    protected float getLiquidInertia() {
        return 0.8F;
    }
    @Nullable
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }
    @Override
    protected double getDefaultGravity() {
        return 0f;
    }






    private final RawAnimation UP = RawAnimation.begin().thenPlay("move.up");
    private final RawAnimation IDLE = RawAnimation.begin().thenPlay("move.idle");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"ground_bone",state -> {
            state.setAnimation(UP);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return GeckoLibUtil.createInstanceCache(this);
    }

}
