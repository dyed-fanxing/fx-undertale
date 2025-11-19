package com.sakpeipei.mod.undertale.entity.summon;

import com.sakpeipei.mod.undertale.common.DamageTypes;
import com.sakpeipei.mod.undertale.registry.ParticleRegistry;
import com.sakpeipei.mod.undertale.item.GasterBlasterProItem;
import com.sakpeipei.mod.undertale.registry.ItemRegistry;
import com.sakpeipei.mod.undertale.registry.SoundRegistry;
import com.sakpeipei.mod.undertale.utils.ParticleMoveUtils;
import com.sakpeipei.mod.undertale.utils.RotUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.UUID;


public class GasterBlasterPro extends LivingEntity implements IGasterBlaster, GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final float DEFAULT_LENGTH = 32f;
    public static final float DEFAULT_LENGTH_SQR = DEFAULT_LENGTH * DEFAULT_LENGTH;
    public static final float HOVER_RADIUS_SQR  = 16;
    private static final Logger log = LogManager.getLogger(GasterBlasterPro.class);

    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("charge");
    private static final RawAnimation GROW_ANIM = RawAnimation.begin().thenPlayAndHold("grow");
    private static final RawAnimation DECAY_ANIM = RawAnimation.begin().thenPlay("decay");

    public static final byte PHASE_IDLE = 0;        // 闲置阶段
    public static final byte PHASE_CHARGE = 1;      // 蓄力阶段（可被中断提前发射）
    public static final byte PHASE_GROW = 2;        // 成长阶段（张嘴）
    public static final byte PHASE_SHOT = 3;        // 发射阶段
    public static final byte PHASE_DECAY = 4;       // 消退阶段

    public static final short DEFAULT_CD = 20;    // 默认CDTick
    public static final byte MAX_CHARGE = 100;     // 最大蓄力Tick
    public static final short DEFAULT_SHOT = 50;   // 默认射击Tick
    private short cd = 0;                           // CD
    private short shot = DEFAULT_SHOT;                             // 默认射击时间 100Tick
    public short timer=0;                            // 计时器
    private final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(GasterBlasterPro.class, EntityDataSerializers.FLOAT);
    private final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(GasterBlasterPro.class, EntityDataSerializers.BYTE);

    private static final float width = 5.0f;
    protected Vec3 end;                 // 攻击终点
    protected float damage  = 0.1f;       // 攻击伤害
    protected UUID ownerUUID;           // 召唤者UUID
    protected LivingEntity owner;       // 召唤者，用于追踪伤害来源仇恨
    private UUID targetUUID;            // 攻击目标UUID
    private LivingEntity target;        // 攻击目标缓存

    public GasterBlasterPro(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }
    public GasterBlasterPro(EntityType<? extends LivingEntity> type, Level level, LivingEntity owner) {
        this(type,level);
        setOwner(owner);
        super.setNoGravity(true);
    }

    @Override
    public void tick(){
        super.tick();
    }

    @Override
    public void aiStep() {
        if(isFire()) {
            timer++;
            processPhase();
        }
        super.aiStep();
        if(cd > 0) cd--;

        if(!level().isClientSide){
            LivingEntity target = getTarget();
            LivingEntity owner = getOwner();
            Vec3 selfPos = this.position();
            float bbWidth = getBbWidth();
            // 有目标时
            if (target != null) {
                // 获取目标方向向量
                Vec3 toTargetDir = target.position().subtract(selfPos);
                double targetDisSqr = toTargetDir.lengthSqr();
                Vec3 dirNormalize = toTargetDir.normalize();
                // 如果在攻击范围内，则不移动，瞄准目标，发射，并延迟追踪
                double verSpeed = 0;
                if(Math.abs(toTargetDir.y) > 10) verSpeed = Mth.clamp(toTargetDir.y * 0.05, -0.3, 0.3);
                double horDisSqr =  DEFAULT_LENGTH_SQR - 100;
                double horSpeed = 0;
                if(toTargetDir.horizontalDistanceSqr() >  horDisSqr * 0.5) horSpeed = Mth.clamp(targetDisSqr * 0.01, 0.1, 0.5);
                setDeltaMovement(new Vec3(dirNormalize.x, 0, dirNormalize.z).normalize().scale(horSpeed).add(0, verSpeed, 0));
                if(targetDisSqr <= DEFAULT_LENGTH_SQR){
                    if(cd == 0) fire();
//                    this.setRot(
//                            Mth.rotLerp(0.1f, getYRot(), RotUtils.yRot(dirNormalize.z, dirNormalize.x)),
//                            Mth.rotLerp(0.1f, getXRot(), RotUtils.xRot(dirNormalize.y)));
                } else {
//                    this.setRot(RotUtils.yRot(dirNormalize.z, dirNormalize.x), 0);
                }
            }
            else if (!isFire() && owner != null) {
                Vec3 targetPos = owner.getEyePosition().add(0, bbWidth / 2, 0);
                Vec3 toOwnerDir = targetPos.subtract(selfPos);
                // 无目标时：根据距离和是否在玩家头顶上方或在玩家下方，决定是否悬浮或向玩家移动
                double horDistanceSqr = toOwnerDir.horizontalDistanceSqr();
                // 水平距离大于跟随距离，直接向目标移动，只转偏航角
                Vec3 horDir = new Vec3(toOwnerDir.x, 0, toOwnerDir.z).normalize();
                double horSpeed ;
                if(horDistanceSqr > HOVER_RADIUS_SQR){
                    Vec3 dir = toOwnerDir.normalize();
                    this.setYRot(RotUtils.yRotD(dir.z, dir.x));
                    horSpeed = Mth.clamp(Math.sqrt(horDistanceSqr) * 0.5, 0.5, 1);
                }else {
                    this.setYRot(owner.getYRot());
                    double ratio = horDistanceSqr / HOVER_RADIUS_SQR;
                    // 使用平方曲线加速减速过程
                    horSpeed = Mth.lerp(-ratio*ratio + horDistanceSqr, 0.005, 0.5);
                }
                double verSpeed = 0;
                if(Math.abs(toOwnerDir.y) > 0.005){
                    // 1. 基础速度：直接比例响应（系数越大，响应越快）
                    verSpeed = toOwnerDir.y * 0.8; // 0.8 是响应强度，可调
                    // 2. 动态速度限制（根据高度差调整最大值）
                    double speedLimit = Mth.lerp(
                            Mth.clamp(Math.abs(toOwnerDir.y), 0, 1), // 1.0 是最大高度差
                            0.2,   // 最小速度（防止近距离时完全停止）
                            1      // 最大速度
                    );
                    verSpeed = Mth.clamp(verSpeed, -speedLimit, speedLimit);
                }
                this.setDeltaMovement(horDir.scale(horSpeed).add(0, verSpeed, 0));
                this.setXRot(0);
            }
        }
    }

    /**
     * 阶段处理
     */
    private void processPhase(){
        switch (getPhase()){
            case PHASE_CHARGE ->  {
                if(timer >= MAX_CHARGE) chargeEnd();
            }
            case PHASE_SHOT -> {
                if(timer >= shot) setPhase(PHASE_DECAY);
                if(!level().isClientSide){
                    // 每2tick应用一次伤害
                    if(timer % 2 == 0) checkHit();
                }
            }
            case PHASE_DECAY -> {
                if(timer >= 2) cooldown();
            }
            case PHASE_GROW -> this.entityData.set(PHASE,PHASE_SHOT);
        }
    }
    /**
     * 蓄力终结
     */
    void chargeEnd(){
        shot = (short) (shot + timer);              // 每多蓄1s延长1s射击时间，最高5s
        damage += (float) (timer * 0.001);    // 每多蓄1s提升 0.5伤害，最高2.5
        setPhase(PHASE_GROW);
    }
    /**
     *  开火
     */
    public void fire(){
        if(!isFire()){
            this.entityData.set(PHASE,PHASE_CHARGE);
        }
    }
    /**
     * 停止开火，不进入冷却
     */
    public void stop(){
        setPhase(PHASE_IDLE);
        // 重置伤害
        shot = DEFAULT_SHOT;
        damage = 2;
    }
    /**
     * 停止开火，进入冷却
     */
    public void cooldown(){
        stop();
        if(owner instanceof Player player) player.getCooldowns().addCooldown(ItemRegistry.GASTER_BLASTER_PRO.get(),DEFAULT_CD);
        cd = DEFAULT_CD;
    }
    @Override
    public void checkHit() {
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
        // 检测光束路径上的所有活体
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(start, end).inflate(0.5 * getWidth()), // 适当扩大检测范围
                this::canHitTarget)) {
            if(target.getBoundingBox().inflate(0.5 * getWidth()).clip(start,end).isPresent()){
                applyDamage(target);
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
    void applyDamage(LivingEntity target) {
        Vec3 deltaMovement = target.getDeltaMovement();
        DamageSource source = damageSources().source(
                DamageTypes.GASTER_BLASTER_BEAM, this, owner
        );
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
    public boolean hurt(@NotNull DamageSource source, float damage) {
        if(getPhase() == PHASE_CHARGE ) chargeEnd();
        return super.hurt(source, damage);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if(reason.shouldDestroy()) {
            if(getOwner() instanceof Player player){
                player.getCooldowns().addCooldown(ItemRegistry.GASTER_BLASTER_PRO.get(),40);
                player.getPersistentData().remove(GasterBlasterProItem.GASTER_BLASTER_PRO_KEY);
            }
        }
        super.remove(reason);
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller",  state -> {
            AnimationController<GasterBlasterPro> controller = state.getController();
            int phase = getPhase();
            if(phase == PHASE_IDLE) {
                controller.forceAnimationReset();
                return PlayState.STOP;
            }
            // 直接根据阶段播放对应动画，不再检查动画状态
            switch (phase) {
                case PHASE_CHARGE -> {
                    controller.setAnimation(CHARGE_ANIM);
                    controller.setAnimationSpeed(20f / MAX_CHARGE);
                    controller.setSoundKeyframeHandler(event -> {
                        level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_CHARGE.get(),
                        SoundSource.NEUTRAL, 1, 1);
                    });
                    // 生成向中心移动的粒子
                    if(state.animationTick % 2 < 0.1) ParticleMoveUtils.ballIn(level(), 20, getWidth() + 1, ParticleRegistry.LIGHT_STREAK.get(), this.getX(), this.getY() + 1.5, this.getZ());
                }
                case PHASE_GROW -> {
                    controller.setAnimation(GROW_ANIM);
                    controller.setAnimationSpeed(20.0);
                    controller.setSoundKeyframeHandler(event -> {
                        level().playLocalSound(this, SoundRegistry.GASTER_BLASTER_SHOT.get(),
                                SoundSource.NEUTRAL,1,1);
                        ParticleMoveUtils.circularOut(level(), 100, getWidth() * 2, ParticleTypes.SOUL_FIRE_FLAME,
                                getX(), getY(), getZ(), getYRot(), getXRot(), 3.0f);
                    });
                }
                case PHASE_DECAY -> {
                    controller.setAnimation(DECAY_ANIM);
                    controller.setAnimationSpeed(10.0);
                }
            }
            return PlayState.CONTINUE;
        }));
    }


    @SubscribeEvent
    public void onEntityChangeDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() == target) { // 目标跨维度时清除缓存
            clearTarget();
        }
    }
    // 当目标被杀死时
    @SubscribeEvent
    public void onTargetDeath(LivingDeathEvent event) {
        log.info("死亡事件{}",event);
        if (event.getEntity().getUUID().equals(targetUUID)) {
            log.info("目标死亡，清除目标{}",event.getEntity());
            clearTarget();
        }
    }

    @Override
    public @Nullable UUID getOwnerUUID() {
        return null;
    }

    @Override
    public LivingEntity getOwner() {
        if(owner != null ) {
            return owner;
        }
        if (ownerUUID != null) {
            LivingEntity entity = (LivingEntity) ((ServerLevel) level()).getEntity(ownerUUID);
            if(entity != null) {
                owner =  entity;
                return owner;
            }
        }
        return null;
    }
    @Override
    public void setOwner(LivingEntity owner) {
        this.ownerUUID = owner.getUUID();
        this.owner = owner;
    }
    public LivingEntity getTarget() {
        if(target != null && target.isAlive()) return target;
        // 2. 客户端直接返回null（客户端不应解析实体）
        if (level().isClientSide) return null;
        if (targetUUID != null) {
            LivingEntity entity = (LivingEntity) ((ServerLevel) level()).getEntity(targetUUID);
            if(entity != null && entity.isAlive()) {
                target =  entity;
                return target;
            }
            else clearTarget();
        }
        return null;
    }
    public void setTarget(LivingEntity target) {
        if(!level().isClientSide) {
            this.targetUUID = target.getUUID();
            this.target = target;
        }
    }
    public void clearTarget(){
        targetUUID = null;
        target = null;
    }

    public byte getPhase() {
        return super.entityData.get(PHASE);
    }
    void setPhase(byte phase){
        this.entityData.set(PHASE,phase);
        timer = 0;
    }
    @Override
    public float getLength() {
        return super.entityData.get(LENGTH);
    }
    public float getWidth() {
        return width;
    }
    public boolean isFire(){
        return super.entityData.get(PHASE) != PHASE_IDLE;
    }

    // 禁用击退
    @Override
    public void knockback(double p_147241_, double p_147242_, double p_147243_) {
    }

    @Override
    public boolean canCollideWith(@NotNull Entity entity) {
        return false;  // 不与任何实体碰撞
    }
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected float getMaxHeadRotationRelativeToBody() {
        return 0.0f;
    }

    @Override
    public boolean isCustomNameVisible() {
        return false;
    }


    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }
    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }
    @Override
    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack itemStack) {
    }
    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    /**
     * 客户端初始化同步数据
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LENGTH, 32f);
        builder.define(PHASE, PHASE_IDLE);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // 基础属性
        timer = tag.getShort("Timer");
        shot = tag.getShort("ShotTime");
        this.entityData.set(PHASE, tag.getByte("Phase"));
        entityData.set(LENGTH,tag.getFloat("Length"));
        ownerUUID = tag.hasUUID("OwnerUUID")?tag.getUUID("OwnerUUID"):null;
        targetUUID = tag.hasUUID("TargetUUID")? tag.getUUID("TargetUUID") : null;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Length", getLength());
        tag.putShort("Timer", timer);
        tag.putShort("ShotTime", shot);
        tag.putByte("Phase", getPhase());
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        if(targetUUID != null) tag.putUUID("TargetUUID", targetUUID);
    }
}
