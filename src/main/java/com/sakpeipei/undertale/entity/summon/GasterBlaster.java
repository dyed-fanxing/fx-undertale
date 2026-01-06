package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.network.GasterBlasterBeamEndPacket;
import com.sakpeipei.undertale.utils.CollisionDetectionUtils;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
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
    private final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("fireTick");
    private final RawAnimation ANTICIPATION_ANIM = RawAnimation.begin().thenPlayAndHold("anticipation");
    private final RawAnimation FIRE_ANIM = RawAnimation.begin().thenPlayAndHold("fire");
    private final RawAnimation DECAY_ANIM = RawAnimation.begin().thenPlay("decay");
    public static final float DEFAULT_LENGTH = 16f;    // 默认长度

    // 光束宽度
    protected float size;           // 大小
    protected float mouthHeight;    // 嘴部高度（炮口位置）
    protected float damage = 2f;    // 攻击伤害
    protected UUID ownerUUID;       // 召唤者UUID
    protected LivingEntity owner;   // 召唤者缓存，用于追踪伤害来源仇恨
    protected Vec3 end;             // 攻击终点


    protected short fireTick;        // 蓄力转发射Tick点
    protected short decayTick;      // 开始衰退Tick点
    protected short discardTick;    // 完毕Tick点

    public GasterBlaster(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }
    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float size) {
        this(type, level,owner,size,(short) 46);
    }
    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float size,short decayTick) {
        this(type, level,owner,0.4f,size,(short) 18,decayTick);
    }
    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float size,float mouthHeightRatio,short fireTick,short decayTick) {
        this(type,level);
        super.setNoGravity(true);
        setOwner(owner);
        this.size = size;
        this.mouthHeight = mouthHeightRatio * size;
        this.fireTick = fireTick;
        this.decayTick = decayTick;
    }

    @Override
    public void tick(){
        super.tick();
        //只在服务端执行攻击逻辑
        if(!this.level().isClientSide){
            //蓄力，0.88秒 = 17.6T = 18T
            if(super.tickCount <= fireTick) {
                return;
            }
            if(super.tickCount > discardTick + 2) {
                this.discard();
                return;
            }
            checkHit();
        }
    }

    @Override
    public void checkHit(){
        Vec3 start = this.getStart();
        // 新的攻击终点
        Vec3 newEnd = start.add(this.getLookAngle().scale(DEFAULT_LENGTH));
        // 光束的射线检测，如果路径上被方块阻挡，则最终位置替换成该方块位置
        BlockHitResult clip = level().clip(new ClipContext(start, newEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        // 攻击终点若为null 或 碰撞的攻击终点位置和上一次的攻击终点位置发生变化，则进行更新
        if(end == null || end.distanceToSqr(clip.getLocation()) > 0.01 ){
            end = clip.getLocation();
            PacketDistributor.sendToPlayersTrackingEntity(this,new GasterBlasterBeamEndPacket(this.getId(),end));
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
        List<LivingEntity> livingEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(mouthHeight), this::canHitTarget)
                .stream().filter(target -> CollisionDetectionUtils.capsuleIntersectsAABB( start, end, size*0.5f,target.getBoundingBox()))
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
    public float getMonthHeight() {
        return mouthHeight;
    }
    @Override
    public float getSize(){
        return size;
    }
    @Override
    public Vec3 getEnd() {
        return end;
    }

    @Override
    public void setEnd(Vec3 end) {
        this.end = end;
    }

    public short getFireTick() {
        return fireTick;
    }
    public short getDecayTick() {
        return decayTick;
    }

    @Override
    public boolean isFire() {
        return this.tickCount > fireTick;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_326003_) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerUUID = tag.hasUUID("OwnerUUID")?tag.getUUID("OwnerUUID"):null;
        size = tag.getFloat("Size");
        mouthHeight = tag.getFloat("MouthHeight");
        fireTick = tag.getShort("FireTick");
        decayTick = tag.getShort("DecayTick");
    }
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("OwnerUUID", ownerUUID);
        tag.putFloat("Size", size);
        tag.putFloat("MouthHeight",mouthHeight);
        tag.putShort("FireTick", fireTick);
        tag.putShort("DecayTick", decayTick);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(size);
        buffer.writeFloat(mouthHeight);
        buffer.writeShort(fireTick);
        buffer.writeShort(discardTick);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.size = buffer.readFloat();
        this.mouthHeight = buffer.readFloat();
        this.fireTick = buffer.readByte();
        this.discardTick = buffer.readShort();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack",  state -> {
            AnimationController<GasterBlaster> controller = state.getController();
            if(controller.getAnimationState() == AnimationController.State.STOPPED){
                GasterBlaster animatable = state.getAnimatable();
                short fireTick = this.getFireTick();
                short decayTick = this.getDecayTick();
                if(animatable.tickCount < fireTick){
                    controller.setAnimation(CHARGE_ANIM);
                    controller.setAnimationSpeed(20.0/fireTick);
                }else if(animatable.tickCount == fireTick){
                    controller.setAnimation(ANTICIPATION_ANIM);
                }else if (animatable.tickCount < decayTick){
                    controller.setAnimation(FIRE_ANIM);
                    controller.setAnimationSpeed(20.0/(decayTick - fireTick));
                }else{
                    controller.setAnimation(DECAY_ANIM);
                }
            }
            return PlayState.CONTINUE;
        }));
    }
}
