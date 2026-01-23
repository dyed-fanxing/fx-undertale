package com.sakpeipei.undertale.entity.summon;

import com.mojang.logging.LogUtils;
import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.network.GasterBlasterBeamEndPacket;
import com.sakpeipei.undertale.registry.EntityTypeRegistry;
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
import net.minecraft.world.phys.shapes.VoxelShape;
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
import software.bernie.geckolib.animation.keyframe.event.builtin.AutoPlayingSoundKeyframeHandler;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 固定动画时间GB
 */
public class GasterBlaster extends Entity implements IGasterBlaster, IEntityWithComplexSpawn, GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
    private static final RawAnimation FIRE_ANIM = RawAnimation.begin().thenPlayAndHold("fire");
    private static final RawAnimation SHOT_ANIM = RawAnimation.begin().thenPlayAndHold("shot");
    private static final RawAnimation DECAY_ANIM = RawAnimation.begin().thenPlay("decay");
    public static final float DEFAULT_LENGTH = 16f;    // 默认长度

    protected float size = 1.0f;            // 大小，基础1.0f，以这个为基准，进行缩放
    protected float mouthHeight = 0.4f;     // 嘴部高度（炮口位置）
    protected float damage = 1f;            // 攻击伤害
    protected UUID ownerUUID;               // 召唤者UUID
    protected LivingEntity owner;           // 召唤者缓存，用于追踪伤害来源仇恨

    protected Vec3 end;                     // 攻击终点

    protected int fireTick = 17;        // 开火Tick点
    protected int shotTick = 19;        // 发射Tick点
    protected int decayTick = 47;       // 开始衰退Tick点
    public GasterBlaster(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }

    public GasterBlaster(Level level, LivingEntity owner) {
        this(level, owner, 1.0f, 17,  28);
    }
    public GasterBlaster(Level level, LivingEntity owner, float size) {
        this(level, owner, size,17,28);
    }
    public GasterBlaster(Level level, LivingEntity owner, float size,int shot) {
        this(level, owner, size,17,shot);
    }
    public GasterBlaster(Level level, LivingEntity owner, float size,int charge, int shot) {
        super(EntityTypeRegistry.GASTER_BLASTER.get(), level);
        super.setNoGravity(true);
        if (owner != null) {
            setOwner(owner);
        }
        this.size = size;
        this.mouthHeight = 0.4f * size;
        this.end = this.position().add(0,mouthHeight,0);
        this.fireTick = charge;
        this.shotTick = fireTick + 2;
        this.decayTick =  (fireTick + shot);
        refreshDimensions();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return this.getType().getDimensions().scale(size);
    }

    @Override
    public void tick() {
        super.tick();
        List<VoxelShape> entityCollisions = this.level().getEntityCollisions(this, this.getBoundingBox());

        if(!entityCollisions.isEmpty()){
            LogUtils.getLogger().warn("发生碰撞,大小{}，碰撞箱大小{}",size,this.getBoundingBox().getSize());
        }
        // 必须直接算出END，同步至客户端，因为背身判断是否渲染的光束条件是通过END判断的
        Vec3 start = this.getStart();
        // 新的攻击终点
        Vec3 newEnd = start.add(this.getLookAngle().scale(DEFAULT_LENGTH));
        // 光束的射线检测，如果路径上被方块阻挡，则最终位置替换成该方块位置
        BlockHitResult clip = level().clip(new ClipContext(start, newEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        // 攻击终点若为null 或 碰撞的攻击终点位置和上一次的攻击终点位置发生变化，则进行更新
        if (end == null || end.distanceToSqr(clip.getLocation()) > 0.001) {
            end = clip.getLocation();
        }
        if(tickCount < fireTick){
            return;
        }
        if(tickCount > decayTick){
            // 固定回收是3Tick时间
            if(tickCount > decayTick + 3){
                this.discard();
            }
            return;
        }
        //只在服务端执行攻击逻辑
        if (!this.level().isClientSide) {
            List<LivingEntity> livingEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(mouthHeight), this::canHitTarget)
                    .stream().filter(target -> CollisionDetectionUtils.capsuleIntersectsAABB(start, end, size * 0.5f, target.getBoundingBox()))
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(start))).toList();
            for (LivingEntity target : livingEntities) {
                target.hurt(damageSources().source(DamageTypes.FRAME, this, getOwner() == null ? this : owner), damage);
            }
        }
    }

    @Override
    public void checkHit() {

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
        return end;
    }

    public void setEnd(Vec3 end) {
        this.end = end;
    }

    public int getFireTick() {
        return fireTick;
    }
    public int getShotTick() {
        return shotTick;
    }
    public int getDecayTick() {
        return decayTick;
    }

    @Override
    public boolean isFire() {
        return this.tickCount > fireTick;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("ownerUUID")) {
            ownerUUID = tag.getUUID("ownerUUID");
        }
        if (tag.contains("size")) {
            size = tag.getFloat("size");
            mouthHeight = size * 0.4f;
            this.end = this.position().add(0,mouthHeight,0);
            refreshDimensions();
        }
        if (tag.contains("fireTick")) {
            fireTick = tag.getInt("fireTick");
        }
        if(tag.contains("shotTick")){
            shotTick = tag.getInt("shotTick");
        }
        if (tag.contains("decayTick")) {
            decayTick = tag.getInt("decayTick");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUUID != null) {
            tag.putUUID("ownerUUID", ownerUUID);
        }
        tag.putFloat("size", size);
        tag.putInt("fireTick", fireTick);
        tag.putInt("shotTick", shotTick);
        tag.putInt("decayTick", decayTick);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(size);
        buffer.writeInt(fireTick);
        buffer.writeInt(shotTick);
        buffer.writeInt(decayTick);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.size = buffer.readFloat();
        this.mouthHeight = size * 0.4f;
        this.end = this.position().add(0,mouthHeight,0);
        this.fireTick = buffer.readInt();
        this.shotTick = buffer.readInt();
        this.decayTick = buffer.readInt();
        this.refreshDimensions();  // 重要！
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack", state -> {
            AnimationController<GasterBlaster> controller = state.getController();
            if(this.tickCount < fireTick) {
                controller.setAnimation(CHARGE_ANIM);
                controller.setAnimationSpeed(20.0/fireTick);
                controller.setSoundKeyframeHandler(keyframe -> this.level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_CHARGE.get(), SoundSource.NEUTRAL, 1, 1));
            }else if (this.tickCount < shotTick) {
                controller.setAnimation(FIRE_ANIM);
                controller.setAnimationSpeed(20.0/(shotTick-fireTick));
            }else if (this.tickCount < decayTick) {
                controller.setAnimation(SHOT_ANIM);
                controller.setAnimationSpeed(20.0 / (decayTick - shotTick));
                controller.setSoundKeyframeHandler(keyframe -> this.level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_FIRE.get(), SoundSource.NEUTRAL, 1, 1));
            } else {
                controller.setAnimation(DECAY_ANIM);
                controller.setAnimationSpeed(6.666667);
            }
            return PlayState.CONTINUE;
        }));
    }
}
