package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.common.DamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 固定动画时间GB
 */
public class GasterBlaster extends Entity implements IGasterBlaster,IEntityWithComplexSpawn,GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation WHOLE_ANIM = RawAnimation.begin().thenPlay("whole");
    private final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
    private final RawAnimation ANTICIPATION_ANIM = RawAnimation.begin().thenPlayAndHold("anticipation");
    private final RawAnimation FIRE_ANIM = RawAnimation.begin().thenPlayAndHold("fire");
    private final RawAnimation DECAY_ANIM = RawAnimation.begin().thenPlay("decay");

    private byte charge;    // 蓄力转发射tick点
    private short discard;   // 发射完毕Tick点

    public static final float DEFAULT_LENGTH = 16f;    // 默认长度
    // 射线当前长度
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(GasterBlaster.class, EntityDataSerializers.FLOAT);

    protected float width;          // 宽度
    protected Vec3 end;             // 攻击终点
    protected float damage = 2f;   // 攻击伤害
    protected UUID ownerUUID;       // 召唤者UUID
    protected LivingEntity owner;   // 召唤者缓存，用于追踪伤害来源仇恨


    // 射线当前长度
    public GasterBlaster(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }
    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float width) {
        this(type, level,owner,width,(byte) 18,(short) 46);
    }
    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float width,short discard) {
        this(type, level,owner,width,(byte) 18,discard);
    }
    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float width, byte charge, short discard) {
        this(type,level);
        super.setNoGravity(true);
        setOwner(owner);
        this.width = width;
        this.charge = charge;
        this.discard = discard;
    }

    @Override
    public void tick(){
        super.tick();
        //只在服务端执行攻击逻辑
        if(!this.level().isClientSide){
            //蓄力，0.88秒 = 17.6T = 18T
            if(super.tickCount <= charge) {
                return;
            }
            // 46
            if(super.tickCount > discard) {
                this.discard();
                return;
            }
            //射击，27T
            checkHit();
        }
    }
    @Override
    public void checkHit(){
        Vec3 start = this.position().add(0,0.4f * width,0);
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
        Vec3 attackDirection = end.subtract(start).normalize();
        // 检测光束路径上的所有活体
        for (int i = 0; i <= 16; i++) {
            Vec3 scale = attackDirection.scale(i).add(start);
            if(this.level() instanceof  ServerLevel level){
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, scale.x,scale.y,scale.z,1,0.0,0.0,0.0,0.0);
            }
        }
        List<LivingEntity> livingEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(getWidth() * 0.5), this::canHitTarget)
                .stream().filter(target -> target.getBoundingBox().clip(start, end).isPresent())
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(start))).toList();
        for (LivingEntity target : livingEntities) {
            applyDamage(target);
        }
    }

    void applyDamage(LivingEntity target) {
        Vec3 deltaMovement = target.getDeltaMovement();
        DamageSource source = damageSources().source( DamageTypes.FRAME, this, getOwner() == null ? this : owner);
        if(target.hurt(source, damage)){
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(), target.getEyeY(), target.getZ(),
                        10, 0.2, 0.2, 0.2, 0.1
                );
            }
            target.setDeltaMovement(deltaMovement); //不击退
        }
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack",  state -> {
            AnimationController<GasterBlaster> controller = state.getController();
            if(controller.getAnimationState() == AnimationController.State.STOPPED){
                GasterBlaster animatable = state.getAnimatable();
                byte charge = this.getCharge();
                short discard = this.getDiscard();
                if(animatable.tickCount < charge){
                    controller.setAnimation(CHARGE_ANIM);
                    controller.setAnimationSpeed(20.0/charge);
                }else if(animatable.tickCount == charge){
                    controller.setAnimation(ANTICIPATION_ANIM);
                }else if (animatable.tickCount <= discard){
                    controller.setAnimation(FIRE_ANIM);
                    controller.setAnimationSpeed(20.0/(discard - charge));
                }else{
                    controller.setAnimation(DECAY_ANIM);
                }
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
    public byte getCharge() {
        return charge;
    }
    public short getDiscard() {
        return discard;
    }

    @Override
    public boolean isFire() {
        return this.tickCount > charge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(LENGTH, 16f);
    }
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerUUID = tag.hasUUID("OwnerUUID")?tag.getUUID("OwnerUUID"):null;
        width = tag.getFloat("Width");
        charge = tag.getByte("Charge");
        discard = tag.getShort("Discard");
    }
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("OwnerUUID", ownerUUID);
        tag.putFloat("Width", width);
        tag.putByte("Charge", charge);
        tag.putShort("Discard", discard);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(width);
        buffer.writeByte(charge);
        buffer.writeShort(discard);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        this.width = additionalData.readFloat();
        this.charge = additionalData.readByte();
        this.discard = additionalData.readShort();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}
