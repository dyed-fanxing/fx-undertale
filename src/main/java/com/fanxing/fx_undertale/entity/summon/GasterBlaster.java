package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.client.render.component.SphereEffectEmitter;
import com.fanxing.fx_undertale.common.damagesource.DamageTypes;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.capability.Mountable;
import com.fanxing.fx_undertale.mixin.LivingEntityAccessor;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.registry.ItemTypes;
import com.fanxing.fx_undertale.registry.SoundEvnets;
import com.fanxing.fx_undertale.utils.ColorUtils;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.fanxing.fx_undertale.utils.collsion.CollisionDetectionUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import java.util.Objects;

public class GasterBlaster extends LivingSummons implements Mountable,IGasterBlaster, IEntityWithComplexSpawn, GeoEntity {
    private static final Logger log = LoggerFactory.getLogger(GasterBlaster.class);
    private static final int DIE_TICK = 400;

    protected Vec3 relativePos;         // 跟随拥有者的相对位置
    protected boolean isFollow;

    public static final float DEFAULT_LENGTH = 32f;
    public static final int DECAY = 2;
    private float maxLength = DEFAULT_LENGTH;
    private float length = DEFAULT_LENGTH;

    protected float size = 1.0f;            // 大小
    protected float aimSmoothSpeed;         // 瞄准追踪的平滑移动速度

    protected int fireTick = 17;            // 开火Tick点
    protected int shotTick = 19;            // 发射Tick点
    protected int decayTick = 47;           // 开始衰退Tick点

    protected float holdTimeScale;
    public static final float DIST = 8f;
    protected Entity target;

    // 动画插值位置
    protected Vec3 startPos;
    protected Vec3 endPos;

    // 骑乘相关
    private static final EntityDataAccessor<Boolean> DATA_MOUNTABLE = SynchedEntityData.defineId(GasterBlaster.class, EntityDataSerializers.BOOLEAN);

    // 光束颜色
    public int[][] color = Sans.ENERGY_AQUA;
    // 吸收光线
    public final SphereEffectEmitter sphereRayEmitter = new SphereEffectEmitter();

    public GasterBlaster(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setCustomNameVisible(false);
    }

    public GasterBlaster(EntityType<? extends LivingEntity> entityType, Level level, LivingEntity owner) {
        super(entityType, level, owner);
        super.setNoGravity(true);
        this.setCustomNameVisible(false);
    }

    public GasterBlaster(Level level, LivingEntity owner) {
        this(level, owner,1.0f, 1.0f, 17,  28,20);
    }
    public GasterBlaster(Level level, LivingEntity owner,float health) {
        this(level, owner,1.0f, 1.0f, 17,  28,health);
    }
    public GasterBlaster(Level level, LivingEntity owner, float damage, float size) {
        this(level, owner,damage, size,17,28,20);
    }
    public GasterBlaster(Level level, LivingEntity owner, float damage, float size, int shot) {
        this(level, owner,damage, size,17,shot,100);
    }
    public GasterBlaster(Level level, LivingEntity owner, float damage, float size, int charge, int shot,float health) {
        super(EntityTypes.GASTER_BLASTER.get(), level,owner);
        super.setNoGravity(true);
        this.setCustomNameVisible(false);
        Objects.requireNonNull(this.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(damage);
        this.size = size;
        this.fireTick = charge-1;
        this.shotTick = fireTick + 2;
        this.decayTick =  (shotTick + shot);
        // 设置最大生命值属性
        Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(health);
        this.setHealth(this.getMaxHealth());
    }
    public GasterBlaster charge(int charge){
        this.fireTick = charge-1;
        this.shotTick = fireTick + 2;
        return this;
    }
    public GasterBlaster shot(int shot){
        this.decayTick =  (shotTick + shot);
        return this;
    }

    public GasterBlaster follow(Vec3 relativePos) {
        this.isFollow = true;
        this.relativePos = relativePos;
        setPos(owner.position().add(RotUtils.rotateYX(relativePos, owner.getViewYRot(1.0f),owner.getViewXRot(1.0f))));
        return this;
    }
    public GasterBlaster aimSmoothSpeed(float speed){
        this.aimSmoothSpeed = speed;
        return this;
    }
    public GasterBlaster mountable(){
        setMountable(true);
        return this;
    }
    public GasterBlaster target(Entity target) {
        this.target = target;
        return this;
    }
    public GasterBlaster color(int[][] color){
        this.color = color;
        return this;
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

    public void restAnimPos(){
        endPos = this.position();
        startPos = this.position().add(this.getLookAngle().scale(-DIST));
        this.setPos(startPos);
    }
    public void restAnimPosClient(){
        startPos = this.position();
        endPos = this.position().add(this.getLookAngle().scale(DIST));
    }
    /**
     * 平滑瞄准目标，旋转速度由 speed 控制（0~1，值越小越慢）
     */
    protected void aimSmoothly(Entity target) {
        Vec3 dir = new Vec3(target.getX(), target.getY(0.5f), target.getZ()).subtract(this.getEyePosition());
        this.maxLength = (float) Math.max(dir.length()+DIST,DEFAULT_LENGTH);
        this.length = maxLength;
        this.setYRot(Mth.rotLerp(aimSmoothSpeed,this.getYRot(), RotUtils.yRotD(dir)));
        this.setXRot(Mth.rotLerp(aimSmoothSpeed,this.getXRot(), RotUtils.xRotD(dir)));
    }

    @Override
    public @NotNull EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
        return this.getType().getDimensions().scale(size);
    }

    /**
     * 瞄准发射时不保存，应该跟随SansAi重启会立刻发射一个新的
     */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }


    @Override
    public void tick() {
        super.tick();
        if(isMountable()) return;
        Entity owner = getOwner();
        if(isFollow){
            if(owner != null){
                float viewXRot = owner.getViewXRot(1.0f);
                float viewYRot = owner.getViewYRot(1.0f);
                setPos(owner.position().add(RotUtils.rotateYX(relativePos, viewYRot,viewXRot)));
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
                        if(!Minecraft.getInstance().options.keySprint.isDown()){
                            this.setXRot(viewXRot);
                        }else{
                            player.setXRot(0);
                            this.setXRot(0);
                        }
                        this.setYRot(viewYRot);
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
        }else{
            if(startPos != null && endPos != null){
                int discardTick = decayTick + DECAY;
                float t = (float) tickCount / discardTick;
                float rise = (float) shotTick / discardTick;
                float pv;
                if(t < rise){
                    pv = CurvesUtils.powRiseEaseOut(t/rise, 8F);
                }else{
                    float tt = (t-rise)/(1-rise);
                    pv = CurvesUtils.powerFallEaseIn(tt,4);
                }
                setPos(startPos.lerp(endPos, pv));
            }
            if(owner instanceof Player player && target != null && this.canHitEntity(target)){
                if(tickCount < shotTick) aim(target);
                else if(tickCount < decayTick) aimSmoothly(target);
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
        if(tickCount < shotTick){
            return;
        }
        if(!this.level().isClientSide){
            Vec3 finalEnd = end;
            List<LivingEntity> livingEntities = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(size), this::canHitEntity)
                    .stream().filter(target -> CollisionDetectionUtils.capsuleIntersectsAABB(start, finalEnd, size * 0.5f, target.getBoundingBox()))
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(start))).toList();
            for (LivingEntity target : livingEntities) {
                target.hurt(damageSources().source(DamageTypes.FRAME, this, getOwner() == null ? this : owner), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }
    }
    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 vec3) {
        double forward = player.zza;
        double strafe = player.xxa;
        boolean jump = ((LivingEntityAccessor) player).isJumping();
        boolean shift = player.isShiftKeyDown();
        double speed = this.getAttributeValue(Attributes.FLYING_SPEED);
        double ySpeed = 0;
        if (jump) ySpeed = ySpeed+speed;
        if (shift) ySpeed = ySpeed-speed;
        Vec3 moveVec = new Vec3(strafe, 0, forward).scale(speed);
        moveVec = moveVec.yRot(-player.getYRot() * Mth.DEG_TO_RAD);
        return new Vec3(moveVec.x, ySpeed, moveVec.z);
    }
    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 vec3) {
        setDeltaMovement(vec3);
        setYRot(player.getYRot());
        setXRot(player.getXRot());
        this.move(MoverType.SELF, this.getDeltaMovement());
    }
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        if(passenger instanceof Player player){
            return player;
        }else if(passenger instanceof Mob mob && mob.canControlVehicle()){
            return mob;
        }
        return super.getControllingPassenger();
    }

    protected void positionRider(@NotNull Entity entity, Entity.@NotNull MoveFunction function) {
        super.positionRider(entity, function);
        if (entity instanceof LivingEntity) {
            ((LivingEntity)entity).yBodyRot = this.getYRot();
        }
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (isMountable() && getOwner() == player) {
            ItemStack stack = player.getItemInHand(hand);
            if(player.isShiftKeyDown() && stack.isEmpty()){
                if (!level().isClientSide) {
                    this.discard();
                    ItemStack itemStack = new ItemStack(ItemTypes.GASTER_BLASTER.get());
                    if (!player.addItem(itemStack)) {
                        this.spawnAtLocation(itemStack);
                    }
                    player.getCooldowns().addCooldown(ItemTypes.GASTER_BLASTER.get(), (int)((1.0f - this.getHealth()/getMaxHealth()) *DIE_TICK));
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

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float damage) {
        if(damageSource.is(Tags.DamageTypes.IS_ENVIRONMENT)){
            return false;
        }
        return super.hurt(damageSource, damage);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (!this.level().isClientSide) {
            if(isMountable()){
                if(getOwner() instanceof Player player){
                    if(!player.addItem(new ItemStack(ItemTypes.GASTER_BLASTER.get()))){
                        this.spawnAtLocation(ItemTypes.GASTER_BLASTER.get());
                    }
                    player.getCooldowns().addCooldown(ItemTypes.GASTER_BLASTER.get(), DIE_TICK);
                }
            }
            this.level().broadcastEntityEvent(this, (byte)60);
            this.discard();
        }
    }
    @Override
    public void lerpTo(double p_20977_, double p_20978_, double p_20979_, float p_20980_, float p_20981_, int p_20982_) {
        super.lerpTo(p_20977_, p_20978_, p_20979_, p_20980_, p_20981_, p_20982_);
        lerpSteps = 0;
    }

    @Override
    public void knockback(double p_147241_, double p_147242_, double p_147243_) {
    }

    @Override
    public boolean addEffect(@NotNull MobEffectInstance effectInstance, @Nullable Entity entity) {
        return false; // 拒绝所有效果
    }
    @Override
    public boolean isPushable() { return false;}

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        this.yBodyRot = yRot;
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

        tag.putFloat("holdTimeScale", holdTimeScale);
        tag.putIntArray("color",ColorUtils.rgbaArrayToInt(color));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.hasUUID("ownerUUID")) ownerUUID = tag.getUUID("ownerUUID");
        if(tag.contains("size")) size = tag.getFloat("size");
        if(tag.contains("fireTick")) fireTick = tag.getInt("fireTick");
        if(tag.contains("shotTick")) shotTick = tag.getInt("shotTick");
        if(tag.contains("decayTick")) decayTick = tag.getInt("decayTick");
        if(tag.contains("maxLength")) maxLength = tag.getFloat("maxLength");
        if(tag.contains("aimSmoothSpeed")) aimSmoothSpeed = tag.getFloat("aimSmoothSpeed");

        if(tag.contains("relativePos")) {
            ListTag list = tag.getList("relativePos", 6);
            this.relativePos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        this.isFollow = tag.getBoolean("isFollow");

        if(tag.contains("mountable")) setMountable(tag.getBoolean("mountable"));
        if(tag.contains("holdTimeScale"))  this.holdTimeScale = tag.getFloat("holdTimeScale");
        if(tag.contains("color")) tag.getIntArray("color");
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
        buffer.writeBoolean(this.isMountable());
        if(relativePos != null) {
            buffer.writeBoolean(true);
            buffer.writeDouble(this.relativePos.x);
            buffer.writeDouble(this.relativePos.y);
            buffer.writeDouble(this.relativePos.z);
        }else{
            buffer.writeBoolean(false);
        }
        buffer.writeFloat(holdTimeScale);
        buffer.writeBoolean(target != null);
        if(target != null) buffer.writeInt(target.getId());
        buffer.writeInt(ColorUtils.rgbaArrayToInt(color[0]));
        buffer.writeInt(ColorUtils.rgbaArrayToInt(color[1]));
        buffer.writeInt(ColorUtils.rgbaArrayToInt(color[2]));
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
        setMountable(buffer.readBoolean());
        if(buffer.readBoolean()) {
            this.relativePos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }
        this.holdTimeScale = buffer.readFloat();
        if(buffer.readBoolean()) this.target = this.level().getEntity(buffer.readInt());
        if(!isFollow && !isMountable()) restAnimPosClient();
        this.color = ColorUtils.intToRGBArrays(buffer.readInt(),buffer.readInt(),buffer.readInt());
        this.refreshDimensions();
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        sphereRayEmitter.setSeed(packet.getUUID());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32f);
    }





    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation FADE_IN_ANIM = RawAnimation.begin().thenPlay("fadeIn");
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
    private static final RawAnimation FIRE_ANIM = RawAnimation.begin().thenPlayAndHold("fire");
    private static final RawAnimation SHOT_ANIM = RawAnimation.begin().thenPlayAndHold("shot");
    private static final RawAnimation DECAY_ANIM = RawAnimation.begin().thenPlay("decay");
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if(!this.isMountable()){
            controllers.add(new AnimationController<>(this, "attack", state -> {
                AnimationController<GasterBlaster> controller = state.getController();
                if(this.tickCount < 1){
                    controller.setAnimation(FADE_IN_ANIM);
                }else if(this.tickCount < fireTick) {
                    controller.setAnimation(CHARGE_ANIM);
                    controller.setAnimationSpeed(20.0 / (fireTick - 1));
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
