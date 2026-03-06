package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.common.DamageTypes;
import com.sakpeipei.undertale.entity.boss.sans.Sans;
import com.sakpeipei.undertale.registry.EntityTypes;
import com.sakpeipei.undertale.registry.ItemTypes;
import com.sakpeipei.undertale.registry.SoundEvnets;
import com.sakpeipei.undertale.utils.CollisionDetectionUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.client.gui.screens.worldselection.OptimizeWorldScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GasterBlaster extends FollowableSummons implements IGasterBlaster,GeoEntity {
    public static final float DEFAULT_LENGTH = 32f;
    public static final int DECAY = 2;
    private static final Logger log = LoggerFactory.getLogger(GasterBlaster.class);
    private float maxLength = DEFAULT_LENGTH;
    private float length = DEFAULT_LENGTH;

    protected float size = 1.0f;            // 大小
    protected float damage = 1f;            // 攻击伤害
    protected float aimSmoothSpeed;         // 瞄准追踪的平滑移动速度

    protected int fireTick = 17;            // 开火Tick点
    protected int shotTick = 19;            // 发射Tick点
    protected int decayTick = 47;           // 开始衰退Tick点

    public GasterBlaster(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }

    public GasterBlaster(EntityType<?> entityType, Level level, Entity owner) {
        super(entityType, level, owner);
    }

    public GasterBlaster(Level level, LivingEntity owner) {
        this(level, owner,1.0f, 1.0f, 17,  28);
    }
    public GasterBlaster(Level level, LivingEntity owner,float damage, float size) {
        this(level, owner,damage, size,17,28);
    }
    public GasterBlaster(Level level, LivingEntity owner,float damage, float size,int shot) {
        this(level, owner,damage, size,17,shot);
    }
    public GasterBlaster(Level level, LivingEntity owner,float damage, float size,int charge, int shot) {
        super(EntityTypes.GASTER_BLASTER.get(), level,owner);
        super.setNoGravity(true);
        this.damage = damage;
        this.size = size;
        this.fireTick = charge;
        this.shotTick = fireTick + 2;
        this.decayTick =  (fireTick + shot);
        refreshDimensions();
    }

    public void aim(Entity target) {
        aim(new Vec3(target.getX(), target.getY(0.5f), target.getZ()));
    }
    public void aim(Vec3 targetPos) {
        Vec3 dir = targetPos.subtract(this.getEyePosition());
        RotUtils.lookVec(this,dir);
        this.maxLength = (float) Math.max(dir.length()+5,DEFAULT_LENGTH);
        this.length = maxLength;
    }
    public GasterBlaster follow(Vec3 relativePos) {
        this.isFollow = true;
        this.relativePos = relativePos;
        setPos(owner.position().add(RotUtils.getWorldPos(relativePos,owner.getViewXRot(1.0f), owner.getViewYRot(1.0f))));
        return this;
    }
    public GasterBlaster aimSmoothSpeed(float speed){
        this.aimSmoothSpeed = speed;
        return this;
    }
    /**
     * 平滑瞄准目标，旋转速度由 speed 控制（0~1，值越小越慢）
     */
    protected void aimSmoothly(Entity target) {
        Vec3 dir = new Vec3(target.getX(), target.getY(0.5f), target.getZ()).subtract(this.getEyePosition());
        this.maxLength = (float) Math.max(dir.length()+5,DEFAULT_LENGTH);
        this.length = maxLength;
        this.setYRot(Mth.rotLerp(aimSmoothSpeed,this.getYRot(), RotUtils.yRotD(dir)));
        this.setXRot(Mth.rotLerp(aimSmoothSpeed,this.getXRot(), RotUtils.xRotD(dir)));
    }
    /**
     * 平滑瞄准目标，旋转速度由 speed 控制（0~1，值越小越慢）
     */
    protected void aimSmoothlyByPlayer(Player player) {
        this.length = maxLength;
        this.setYRot(owner.getViewYRot(1.0f));
        this.setXRot(owner.getViewXRot(1.0f));
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return this.getType().getDimensions().scale(size);
    }

    /**
     * 瞄准发射时不保存，应该跟随SansAi重启会立刻发射一个新的
     */
    @Override
    public boolean shouldBeSaved() {
        return !isFollow && super.shouldBeSaved();
    }

    @Override
    public void tick() {
        super.tick();
        if(isFollow){
            if(this.level().isClientSide && owner == null && ownerId != -1){
                Entity owner = this.level().getEntity(ownerId);
                if(owner != null){
                    setOwner(owner);
                }
            }
            Entity owner = getOwner();
            if(owner != null){
                float viewXRot = owner.getViewXRot(1.0f);
                float viewYRot = owner.getViewYRot(1.0f);
                setPos(owner.position().add(RotUtils.getWorldPos(relativePos,viewXRot, viewYRot)));
                if(owner instanceof Targeting targeting){
                    LivingEntity target = targeting.getTarget();
                    if(target != null){
                        aimSmoothly(target);
                    }else if(!this.level().isClientSide){
                        this.discard();
                        return;
                    }
                }else if(owner instanceof Player player){
                    if(player.isUsingItem() && player.getUseItem().getItem() == ItemTypes.GASTER_BLASTER.get()){
                        log.info("正在使用GB");
                        log.info("设置前视角：（{},{}）",this.getXRot(),this.getYRot());
                        aimSmoothlyByPlayer(player);
                        log.info("设置后视角：（{},{}）",this.getXRot(),this.getYRot());
                    }
                } else if(!this.level().isClientSide){
                    // 必须服务端，在重新进入游戏时，服务端同步targetId，客户端接受有延迟
                    this.discard();
                    return;
                }
            }else if(!this.level().isClientSide){
                this.discard();
                return;
            }
        }
        if(tickCount > decayTick){
            if(tickCount >= decayTick + DECAY){
                this.discard();
            }
            return;
        }
        Vec3 start = this.getEyePosition();
        Vec3 end = start.add(this.getLookAngle().scale(maxLength));
        // 光束的射线检测，如果路径上被方块阻挡，则最终位置替换成该方块位置
        BlockHitResult clip = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        // 攻击终点若为null 或 碰撞的攻击终点位置和上一次的攻击终点位置发生变化，则进行更新
        double disSqr = start.distanceToSqr(clip.getLocation());
        if (maxLength*maxLength - disSqr >= Mth.EPSILON) {
            end = clip.getLocation();
            length = (float) Math.sqrt(disSqr);
        }
        if(tickCount < fireTick){
            return;
        }
        if(!this.level().isClientSide){
            Vec3 finalEnd = end;
            List<LivingEntity> livingEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(size), this::canHitEntity)
                    .stream().filter(target -> CollisionDetectionUtils.capsuleIntersectsAABB(start, finalEnd, size * 0.5f, target.getBoundingBox()))
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(start))).toList();
            for (LivingEntity target : livingEntities) {
                target.hurt(damageSources().source(DamageTypes.FRAME, this, getOwner() == null ? this : owner), damage);
            }
        }

    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
    }

    @Override
    protected void onHitEntity(Entity entity, Vec3 location) {
    }

    @Override
    public float getSize() {
        return size;
    }

    public float getLength() {
        return length;
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
    public boolean isFollow(){
        return isFollow;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) {
            tag.putUUID("ownerUUID", ownerUUID);
        }
        tag.putFloat("size", size);
        tag.putInt("fireTick", fireTick);
        tag.putInt("shotTick", shotTick);
        tag.putInt("decayTick", decayTick);
        tag.putFloat("maxLength", maxLength);
        tag.putFloat("aimSmoothSpeed", aimSmoothSpeed);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("ownerUUID")) {
            ownerUUID = tag.getUUID("ownerUUID");
        }
        if (tag.contains("size")) {
            size = tag.getFloat("size");
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
        if(tag.contains("maxLength")){
            maxLength = tag.getFloat("maxLength");
        }
        if(tag.contains("aimSmoothSpeed")){
            aimSmoothSpeed = tag.getFloat("aimSmoothSpeed");
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        super.writeSpawnData(buffer);
        buffer.writeFloat(size);
        buffer.writeInt(fireTick);
        buffer.writeInt(shotTick);
        buffer.writeInt(decayTick);
        buffer.writeFloat(maxLength);
        buffer.writeFloat(aimSmoothSpeed);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        super.readSpawnData(buffer);
        this.size = buffer.readFloat();
        this.fireTick = buffer.readInt();
        this.shotTick = buffer.readInt();
        this.decayTick = buffer.readInt();
        this.maxLength = buffer.readFloat();
        this.aimSmoothSpeed = buffer.readFloat();
        this.refreshDimensions();
    }





    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
    private static final RawAnimation FIRE_ANIM = RawAnimation.begin().thenPlayAndHold("fire");
    private static final RawAnimation SHOT_ANIM = RawAnimation.begin().thenPlayAndHold("shot");
    private static final RawAnimation DECAY_ANIM = RawAnimation.begin().thenPlay("decay");
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack", state -> {
            AnimationController<GasterBlaster> controller = state.getController();
            if(this.tickCount < fireTick) {
                controller.setAnimation(CHARGE_ANIM);
                controller.setAnimationSpeed(20.0/fireTick);
                controller.setSoundKeyframeHandler(keyframe -> this.level().playLocalSound(this, SoundEvnets.GASTER_BLASTER_CHARGE.get(), SoundSource.NEUTRAL, 1, 1));
            }else if (this.tickCount < shotTick) {
                controller.setAnimation(FIRE_ANIM);
                controller.setAnimationSpeed(20.0/(shotTick-fireTick));
            }else if (this.tickCount < decayTick) {
                controller.setAnimation(SHOT_ANIM);
                controller.setAnimationSpeed(20.0 / (decayTick - shotTick));
                controller.setSoundKeyframeHandler(keyframe -> this.level().playLocalSound(this, SoundEvnets.GASTER_BLASTER_FIRE.get(), SoundSource.NEUTRAL, 1, 1));
            } else {
                controller.setAnimation(DECAY_ANIM);
                controller.setAnimationSpeed(6.666667);
            }
            return PlayState.CONTINUE;
        }));
    }
}
