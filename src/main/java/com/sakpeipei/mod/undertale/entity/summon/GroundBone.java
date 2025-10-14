package com.sakpeipei.mod.undertale.entity.summon;

import com.sakpeipei.mod.undertale.common.DamageTypes;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.mechanism.ColorAttack;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 18:44
 */
public class GroundBone extends Entity implements GeoEntity, GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ColorAttack colorAttack;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;
    private float damage;
    private float speed;
    private int delay = 0;
    private int lifetime = 20;

    public GroundBone(EntityType<? extends GroundBone> type, Level level) {
        super(type, level);
    }

    public GroundBone(Level level, LivingEntity owner, float damage,float speed,int delay,double x, double y, double z) {
        this(level, owner, damage, speed,delay,x,y,z, ColorAttack.WHITE);
    }

    public GroundBone(Level level, LivingEntity owner, float damage, float speed,int delay,double x, double y, double z, ColorAttack colorAttack) {
        this(EntityTypeRegistry.GROUND_BONE.get(), level);
        this.setNoGravity(true);
        setOwner(owner);
        this.damage = damage;
        this.speed = speed;
        this.delay = delay;
        setPos(x,y,z);
        this.colorAttack = colorAttack;
    }

    @Override
    public void tick() {
        super.tick();
        if(delay-- <= 0){
            if (tickCount > lifetime) {
                this.discard();
                return;
            }
            for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2, 0.0F, 0.2), this::canHitEntity)) {
                onHitEntity(target);
            }
            Vec3 deltaMovement = getDeltaMovement();
            setDeltaMovement(deltaMovement.add(0, this.tickCount * this.tickCount * 0.1f, 0));
        }
    }

    private void onHitBlick(BlockHitResult hitResult) {
        BlockState blockstate = this.level().getBlockState(hitResult.getBlockPos());

    }

    private boolean canHitEntity(Entity entity) {
        return entity.isAlive() && entity != getOwner() && colorAttack.canHitEntity(entity);
    }

    private void onHitEntity(Entity entity) {
        LivingEntity owner = getOwner();
        if (owner == null) {
            entity.hurt(damageSources().source(DamageTypes.FRAME, this), damage);
        } else {
            if (owner instanceof Sans) {
                entity.hurt(damageSources().source(DamageTypes.FRAME, owner, this), damage);
            } else {
                entity.hurt(damageSources().indirectMagic(owner, this), damage);
            }
        }
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }
    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel) this.level()).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity) entity;
            }
        }
        return this.owner;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.contains("color")) {
            this.colorAttack = ColorAttack.getInstance(tag.getInt("color"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putInt("color", this.colorAttack.getColor().getColor());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }


    @Nullable
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0f;
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
