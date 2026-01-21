package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.network.GasterBlasterBeamEndPacket;
import com.sakpeipei.undertale.registry.SoundRegistry;
import com.sakpeipei.undertale.utils.CollisionDetectionUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
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
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.event.sound.SoundEvent;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
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
public class GasterBlaster extends Entity implements IGasterBlaster, IEntityWithComplexSpawn, GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
    private final RawAnimation ANTICIPATION_ANIM = RawAnimation.begin().thenPlayAndHold("anticipation");
    private final RawAnimation FIRE_ANIM = RawAnimation.begin().thenPlayAndHold("fire");
    private final RawAnimation DECAY_ANIM = RawAnimation.begin().thenPlay("decay");
    public static final float DEFAULT_LENGTH = 16f;    // 默认长度

    private static final EntityDataAccessor<Vector3f> END = SynchedEntityData.defineId(GasterBlaster.class, EntityDataSerializers.VECTOR3);

    // 光束宽度
    protected float size;           // 大小
    protected float mouthHeight;    // 嘴部高度（炮口位置）
    protected float damage = 2f;    // 攻击伤害
    protected UUID ownerUUID;       // 召唤者UUID
    protected LivingEntity owner;   // 召唤者缓存，用于追踪伤害来源仇恨
    protected Vec3 end;             // 攻击终点

    protected short fireTick;        // 蓄力转发射Tick点
    protected short decayTick;      // 开始衰退Tick点

    public GasterBlaster(EntityType<? extends Entity> type, Level level) {
        this(type, level, null, 1.0f);
    }

    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner) {
        this(type, level, owner, 1.0f, (short) 32);
    }

    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float size) {
        this(type, level, owner, size, (short) 32);
    }

    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float size, short shot) {
        this(type, level, owner, size, 0.4f, (short) 17, shot);
    }

    public GasterBlaster(EntityType<? extends Entity> type, Level level, LivingEntity owner, float size, float mouthHeightRatio, short charge, short shot) {
        super(type, level);
        super.setNoGravity(true);
        if (owner != null) {
            setOwner(owner);
        }
        this.size = size;
        this.mouthHeight = mouthHeightRatio * size;
        this.fireTick = (short) (charge + 1);
        this.decayTick = (short) (fireTick + shot);
    }

    @Override
    public void tick() {
        super.tick();
        //只在服务端执行攻击逻辑
        if (!this.level().isClientSide) {
            // 必须直接算出END，同步至客户端，因为背身判断是否渲染的光束条件是通过END判断的
            Vec3 start = this.getStart();
            // 新的攻击终点
            Vec3 newEnd = start.add(this.getLookAngle().scale(DEFAULT_LENGTH));
            // 光束的射线检测，如果路径上被方块阻挡，则最终位置替换成该方块位置
            BlockHitResult clip = level().clip(new ClipContext(start, newEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            // 攻击终点若为null 或 碰撞的攻击终点位置和上一次的攻击终点位置发生变化，则进行更新
            if (end == null || end.distanceToSqr(clip.getLocation()) > 0.001) {
                end = clip.getLocation();
                this.entityData.set(END, end.toVector3f());
            }
            if (tickCount > fireTick && tickCount < decayTick) {
                List<LivingEntity> livingEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(mouthHeight), this::canHitTarget)
                        .stream().filter(target -> CollisionDetectionUtils.capsuleIntersectsAABB(start, end, size * 0.5f, target.getBoundingBox()))
                        .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(start))).toList();
                for (LivingEntity target : livingEntities) {
                    applyDamage(target);
                }
            }
            //蓄力，0.88秒 = 17.6T = 18T
            if (tickCount > decayTick + 2) {
                this.discard();
            }
        }
    }

    @Override
    public void checkHit() {
//        Vec3 start = this.getStart();
//        // 新的攻击终点
//        Vec3 newEnd = start.add(this.getLookAngle().scale(DEFAULT_LENGTH));
//        // 光束的射线检测，如果路径上被方块阻挡，则最终位置替换成该方块位置
//        BlockHitResult clip = level().clip(new ClipContext(start, newEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
//        // 攻击终点若为null 或 碰撞的攻击终点位置和上一次的攻击终点位置发生变化，则进行更新
//        if(end == null || end.distanceToSqr(clip.getLocation()) > 0.001 ){
//            end = clip.getLocation();
//            this.entityData.set(END,end.toVector3f());
//        }
//        List<LivingEntity> livingEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(mouthHeight), this::canHitTarget)
//                .stream().filter(target -> CollisionDetectionUtils.capsuleIntersectsAABB( start, end, size * 0.5f, target.getBoundingBox()))
//                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(start))).toList();
//        for (LivingEntity target : livingEntities) {
//            applyDamage(target);
//        }
    }

    void applyDamage(LivingEntity target) {
        if (target.hurt(damageSources().source(DamageTypes.FRAME, this, getOwner() == null ? this : owner), damage)) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(), target.getEyeY(), target.getZ(),
                        10, 0.2, 0.2, 0.2, 0.1
                );
            }
        }
    }

    @Override
    public LivingEntity getOwner() {
        if (owner != null && !owner.isRemoved()) {
            return owner;
        }
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            LivingEntity entity = (LivingEntity) serverLevel.getEntity(ownerUUID);
            if (entity != null) {
                owner = entity;
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
    public float getSize() {
        return size;
    }

    @Override
    public Vec3 getEnd() {
        Vector3f vector3f = this.entityData.get(END);
        if (end == null || !this.end.toVector3f().equals(vector3f)) {
            this.end = new Vec3(vector3f);
        }
        return this.end;
    }

    @Override
    public void setEnd(Vector3f end) {
        this.end = new Vec3(end);
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(END, new Vector3f());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("Size")) {
            size = tag.getFloat("Size");
        }
        if (tag.contains("MouthHeight")) {
            mouthHeight = tag.getFloat("MouthHeight");
        }
        if (tag.contains("FireTick")) {
            fireTick = tag.getShort("FireTick");
        }
        if (tag.contains("DecayTick")) {
            decayTick = tag.getShort("DecayTick");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        tag.putFloat("Size", size);
        tag.putFloat("MouthHeight", mouthHeight);
        tag.putShort("FireTick", fireTick);
        tag.putShort("DecayTick", decayTick);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(size);
        buffer.writeFloat(mouthHeight);
        buffer.writeShort(fireTick);
        buffer.writeShort(decayTick);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.size = buffer.readFloat();
        this.mouthHeight = buffer.readFloat();
        this.fireTick = buffer.readShort();
        this.decayTick = buffer.readShort();

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack", state -> {
            AnimationController<GasterBlaster> controller = state.getController();
            GasterBlaster animatable = state.getAnimatable();
            short fireTick = this.getFireTick();
            short decayTick = this.getDecayTick();
            if (animatable.tickCount < fireTick) {
                controller.setAnimation(CHARGE_ANIM);
                controller.setAnimationSpeed(20.0 / fireTick);
                controller.setSoundKeyframeHandler(keyframe -> this.level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_CHARGE.get(), SoundSource.NEUTRAL, 1, 1));
            } else if (animatable.tickCount < fireTick + 1) {
                controller.setAnimation(ANTICIPATION_ANIM);
                controller.setAnimationSpeed(20.0);
            } else if (animatable.tickCount < decayTick) {
                controller.setAnimation(FIRE_ANIM);
                controller.setAnimationSpeed(20.0 / (decayTick - fireTick));
                controller.setSoundKeyframeHandler(keyframe -> this.level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_FIRE.get(), SoundSource.NEUTRAL, 1, 1));
            } else {
                controller.setAnimation(DECAY_ANIM);
                controller.setAnimationSpeed(10.0);
            }
            return PlayState.CONTINUE;
        }));
    }
}
