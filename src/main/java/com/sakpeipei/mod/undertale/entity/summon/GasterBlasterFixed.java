package com.sakpeipei.mod.undertale.entity.summon;

import com.sakpeipei.mod.undertale.tags.damagetype.DamageTypes;
import com.sakpeipei.mod.undertale.registry.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 固定动画时间GB
 */
public class GasterBlasterFixed extends Entity implements IGasterBlaster, GeoEntity, IEntityWithComplexSpawn {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation WHOLE_ANIM = RawAnimation.begin().thenPlay("whole");

    public static final byte CHARGE_TICK = 18;
    public static final float DEFAULT_LENGTH = 16f;    // 默认长度
    // 射线当前长度
    protected static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(GasterBlasterFixed.class, EntityDataSerializers.FLOAT);

    protected float width = 1.0f;          // 宽度
    protected Vec3 end;             // 攻击终点
    protected float damage  = 2f;   // 攻击伤害
    protected UUID ownerUUID;       // 召唤者UUID
    protected LivingEntity owner;   // 召唤者，用于追踪伤害来源仇恨

    // 射线当前长度
    public GasterBlasterFixed(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }
    public GasterBlasterFixed(EntityType<? extends Entity> type, Level level, LivingEntity owner) {
        this(type, level,owner,1.0f);
    }
    public GasterBlasterFixed(EntityType<? extends Entity> type, Level level, LivingEntity owner, float width) {
        this(type,level);
        super.setNoGravity(true);
        setOwner(owner);
        this.width = width;
    }

    @Override
    public void tick(){
        super.tick();
        //只在服务端执行攻击逻辑
        if(!this.level().isClientSide){
            //蓄力，0.88秒 = 17.6T = 18T
            if(super.tickCount <= CHARGE_TICK) {
                return;
            }
            if(super.tickCount > 46) {
                super.discard();
                return;
            }
            //射击，27T
            checkHit();
        }
    }

    @Override
    public void checkHit(){
        Vec3 start = this.position();
        // 新的攻击终点
        Vec3 newEnd = start.add(this.getLookAngle().scale(DEFAULT_LENGTH));
        // 光束的射线检测，如果路径上被方块阻挡，则最终位置替换成该方块位置
        BlockHitResult clip = level().clip(new ClipContext(start, newEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        // 攻击终点若为null 或 碰撞的攻击终点位置和上一次的攻击终点位置发生变化，则进行更新
        if(end == null || end.distanceToSqr(clip.getLocation()) > 0.01 ){
            end = clip.getLocation();
            this.entityData.set(LENGTH, (float) start.distanceTo(end));
        }
        // 获取攻击方向向量（从目标指向炮台）
        Vec3 attackDirection = start.subtract(end).normalize();
        // 检测光束路径上的所有活体
        List<LivingEntity> livingEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(getWidth() * 0.5), this::canHitTarget)
                .stream().filter(target -> target.getBoundingBox().inflate(getWidth() * 0.5).clip(start, end).isPresent())
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(start))).toList();

        for (LivingEntity target : livingEntities) {
            if (isBlockingWithShield(target, attackDirection)) {
                end = target.position();
                this.entityData.set(LENGTH, (float) start.distanceTo(end));
                break;
            } else {
                applyDamage(target); // 对盾牌前的实体造成伤害
            }
        }
        level().addParticle(ParticleTypes.END_ROD,
                (start.x + end.x)/2,
                (start.y + end.y)/2,
                (start.z + end.z)/2,
                end.x - start.x,
                end.y - start.y,
                end.z - start.z);
    }
    private boolean isBlockingWithShield(LivingEntity target, Vec3 attackDirection) {
        // 1. 检查主手或副手是否举盾
        if (!(target.isUsingItem() && (target.getUseItem().getItem() instanceof ShieldItem))) {
            return false;
        }
        // 2. 检查盾牌是否面向攻击方向（角度阈值约100°）
        Vec3 shieldDirection = target.getViewVector(1.0F); // 实体面朝方向
        double dotProduct = shieldDirection.dot(attackDirection.normalize()); // 点积计算夹角

        // 点积小于 cos(50°) ≈ 0.6428 时判定为有效格挡（50°是半角）
        return dotProduct < -0.6428; // 负值表示攻击来自前方
    }
    void applyDamage(LivingEntity target) {
        Vec3 deltaMovement = target.getDeltaMovement();
        DamageSource source = damageSources().source( DamageTypes.GASTER_BLASTER_BEAM, this, getOwner() == null ? this : owner);
        target.hurt(source, damage);
        target.invulnerableTime = 0; // 破解无敌帧
        target.setDeltaMovement(deltaMovement); //不击退
        // 粒子效果（服务端发送给客户端）
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getEyeY(), target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.1
            );
        }
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller",  state -> {
            if(state.getController().getAnimationState() == AnimationController.State.STOPPED){
                state.getController().setAnimation(WHOLE_ANIM);
                level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_WHOLE.get(), SoundSource.NEUTRAL,1,1);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public LivingEntity getOwner() {
        if(owner != null && !owner.isRemoved()) {
            return owner;
        }
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            LivingEntity entity = (LivingEntity) serverLevel.getEntity(ownerUUID);
            if(entity != null) {
                owner =  entity;
                return entity;
            }
        }
        return null;
    }
    @Override
    public void setOwner(LivingEntity owner) {
        this.ownerUUID = owner.getUUID();
        this.owner = owner;
    }
    @Override
    public @Nullable UUID getOwnerUUID() {
        return ownerUUID;
    }
    @Override
    public float getLength() {return super.entityData.get(LENGTH);}
    @Override
    public float getWidth(){return width;}
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(LENGTH, 16f);
    }
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(LENGTH,tag.getFloat("Length"));
        ownerUUID = tag.hasUUID("OwnerUUID")?tag.getUUID("OwnerUUID"):null;
    }
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Length", getLength());
        tag.putFloat("Width", getWidth());
        tag.putUUID("OwnerUUID", ownerUUID);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(width);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        this.width = additionalData.readFloat();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
