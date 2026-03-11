package com.sakpeipei.undertale.entity.summon;

import com.sakpeipei.undertale.entity.Mountable;
import com.sakpeipei.undertale.mixin.LivingEntityAccessor;
import com.sakpeipei.undertale.registry.EntityTypes;
import com.sakpeipei.undertale.registry.ItemTypes;
import com.sakpeipei.undertale.registry.SoundEvnets;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

public class GasterBlasterLiving extends LivingSummons implements Mountable,IGasterBlaster, IEntityWithComplexSpawn, GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(GasterBlasterLiving.class);


    protected Vec3 relativePos;         // 跟随拥有者的相对位置
    protected boolean isFollow;

    public static final float DEFAULT_LENGTH = 32f;
    public static final int DECAY = 2;
    private float maxLength = DEFAULT_LENGTH;
    private float length = DEFAULT_LENGTH;

    protected float size = 1.0f;            // 大小
    protected float damage = 1f;            // 攻击伤害
    protected float aimSmoothSpeed;         // 瞄准追踪的平滑移动速度

    protected int fireTick = 17;            // 开火Tick点
    protected int shotTick = 19;            // 发射Tick点
    protected int decayTick = 47;           // 开始衰退Tick点
    // 骑乘相关
    private static final EntityDataAccessor<Boolean> DATA_MOUNTABLE = SynchedEntityData.defineId(GasterBlasterLiving.class, EntityDataSerializers.BOOLEAN);
    private static final double HORIZONTAL_SPEED = 0.5;
    private static final double VERTICAL_SPEED = 0.5;


    public GasterBlasterLiving(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        super.setNoGravity(true);
    }

    public GasterBlasterLiving(EntityType<? extends LivingEntity> entityType, Level level, LivingEntity owner) {
        super(entityType, level, owner);
        super.setNoGravity(true);
    }

    public GasterBlasterLiving(Level level, LivingEntity owner) {
        this(level, owner,1.0f, 1.0f, 17,  28);
    }
    public GasterBlasterLiving(Level level, LivingEntity owner,float damage, float size) {
        this(level, owner,damage, size,17,28);
    }
    public GasterBlasterLiving(Level level, LivingEntity owner,float damage, float size,int shot) {
        this(level, owner,damage, size,17,shot);
    }
    public GasterBlasterLiving(Level level, LivingEntity owner,float damage, float size,int charge, int shot) {
        super(EntityTypes.GASTER_BLASTER_LIVING.get(), level,owner);
        super.setNoGravity(true);
        this.damage = damage;
        this.size = size;
        this.fireTick = charge;
        this.shotTick = fireTick + 2;
        this.decayTick =  (shotTick + shot);
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
    public GasterBlasterLiving follow(Vec3 relativePos) {
        this.isFollow = true;
        this.relativePos = relativePos;
        setPos(owner.position().add(RotUtils.getWorldPos(relativePos,owner.getViewXRot(1.0f), owner.getViewYRot(1.0f))));
        return this;
    }
    public GasterBlasterLiving aimSmoothSpeed(float speed){
        this.aimSmoothSpeed = speed;
        return this;
    }
    public GasterBlasterLiving mountable(){
        setMountable(true);
        return this;
    }
    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (isMountable() && owner == player) {
            ItemStack stack = player.getItemInHand(hand);
            if(player.isShiftKeyDown() && stack.isEmpty()){
                if (!level().isClientSide) {
                    this.discard();
                    this.spawnAtLocation(ItemTypes.GASTER_BLASTER.get());
                }
                return InteractionResult.CONSUME;
            }else{
                if (!level().isClientSide && getPassengers().isEmpty()) {
                    player.startRiding(this);
                    return InteractionResult.CONSUME;
                }
            }
        }
        return super.interact(player, hand);
    }

    protected void positionRider(@NotNull Entity entity, Entity.@NotNull MoveFunction function) {
        super.positionRider(entity, function);
        if (entity instanceof LivingEntity) {
            ((LivingEntity)entity).yBodyRot = this.getYRot();
        }

    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 vec3) {
        setDeltaMovement(vec3);
        setYRot(player.getYRot());
        setXRot(player.getXRot());
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 vec3) {
        double forward = vec3.z;
        double strafe = vec3.x;
        boolean jump = ((LivingEntityAccessor) player).isJumping();
        boolean shift = player.isShiftKeyDown();
        double ySpeed = 0;
        if (jump) ySpeed = ySpeed+VERTICAL_SPEED;
        if (shift) ySpeed = ySpeed-VERTICAL_SPEED;
        Vec3 moveVec = new Vec3(strafe, 0, forward).normalize();
        moveVec = moveVec.yRot(-player.getYRot() * Mth.DEG_TO_RAD);
        return new Vec3(moveVec.x, ySpeed, moveVec.z);
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
    public boolean isMountable() {
        return entityData.get(DATA_MOUNTABLE);
    }
    public void setMountable(boolean mountable) {
        entityData.set(DATA_MOUNTABLE, mountable);
    }

    @Override
    public boolean isFire() {
        return this.tickCount > fireTick;
    }
    public void startDecay() {
        this.decayTick = this.tickCount+DECAY;
    }

    public boolean isFollow(){
        return isFollow;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOUNTABLE,false);
    }



    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
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

        if (relativePos != null) {
            tag.put("relativePos", this.newDoubleList(relativePos.x, relativePos.y, relativePos.z));
        }
        tag.putBoolean("isFollow", isFollow);


        tag.putBoolean("mountable", isMountable());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
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

        if (tag.contains("relativePos")) {
            ListTag list = tag.getList("relativePos", 6);
            this.relativePos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        this.isFollow = tag.getBoolean("isFollow");

        if (tag.contains("mountable")) {
            setMountable(tag.getBoolean("mountable"));
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(size);
        buffer.writeInt(fireTick);
        buffer.writeInt(shotTick);
        buffer.writeInt(decayTick);
        buffer.writeFloat(maxLength);
        buffer.writeFloat(aimSmoothSpeed);
        buffer.writeBoolean(this.isFollow);
        if(relativePos != null) {
            buffer.writeBoolean(true);
            buffer.writeDouble(this.relativePos.x);
            buffer.writeDouble(this.relativePos.y);
            buffer.writeDouble(this.relativePos.z);
        }else{
            buffer.writeBoolean(false);
        }
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        this.size = buffer.readFloat();
        this.fireTick = buffer.readInt();
        this.shotTick = buffer.readInt();
        this.decayTick = buffer.readInt();
        this.maxLength = buffer.readFloat();
        this.aimSmoothSpeed = buffer.readFloat();
        this.isFollow = buffer.readBoolean();
        if(buffer.readBoolean()) {
            this.relativePos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        this.refreshDimensions();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32f);
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
        if(!this.isMountable()){
            controllers.add(new AnimationController<>(this, "attack", state -> {
                AnimationController<GasterBlasterLiving> controller = state.getController();
                if (this.tickCount < fireTick) {
                    controller.setAnimation(CHARGE_ANIM);
                    controller.setAnimationSpeed(20.0 / fireTick);
                    controller.setSoundKeyframeHandler(keyframe -> this.level().playLocalSound(this, SoundEvnets.GASTER_BLASTER_CHARGE.get(), SoundSource.NEUTRAL, 1, 1));
                } else if (this.tickCount < shotTick) {
                    controller.setAnimation(FIRE_ANIM);
                    controller.setAnimationSpeed(20.0 / (shotTick - fireTick));
                } else if (this.tickCount < decayTick) {
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
}
