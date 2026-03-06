package com.sakpeipei.undertale.entity.boss.sans;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import com.sakpeipei.undertale.client.render.effect.WarningTip;
import com.sakpeipei.undertale.entity.AbstractUTMonster;
import com.sakpeipei.undertale.entity.mechanism.ColorAttack;
import com.sakpeipei.undertale.common.phys.LocalDirection;
import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.entity.ai.control.PatchedMoveControl;
import com.sakpeipei.undertale.entity.attachment.Gravity;
import com.sakpeipei.undertale.entity.attachment.KaramJudge;
import com.sakpeipei.undertale.entity.persistentData.PersistentDataDict;
import com.sakpeipei.undertale.entity.persistentData.SoulMode;
import com.sakpeipei.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GroundBone;
import com.sakpeipei.undertale.entity.summon.LateralBone;
import com.sakpeipei.undertale.entity.summon.MovingGroundBone;
import com.sakpeipei.undertale.net.packet.*;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import com.sakpeipei.undertale.registry.EntityTypes;
import com.sakpeipei.undertale.registry.MemoryModuleTypes;
import com.sakpeipei.undertale.registry.SoundEvnets;
import com.sakpeipei.undertale.utils.EntityUtils;
import com.sakpeipei.undertale.utils.GravityUtils;
import com.sakpeipei.undertale.utils.LevelUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.List;

public class Sans extends AbstractUTMonster implements GeoEntity, IAnimatable, IEntityWithComplexSpawn {
    private static final Logger log = LogManager.getLogger(Sans.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final byte OPENING_ATTACK = 0;
    public static final byte FIRST_PHASE = 1;
    public static final byte MERCY_PHASE = 2;
    public static final byte SECOND_PHASE = 3;
    public static final byte SPECIAL_ATTACK = 4;
    public static final byte END_PHASE = 5;

    private static final EntityDataAccessor<Byte> PHASE_ID = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> IS_EYE_BLINK = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> STAMINA = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.INT);


    protected static final List<SensorType<? extends Sensor<? super Sans>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES,SensorType.HURT_BY);
    protected static final List<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,

            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,

            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.ANGRY_AT,
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.ATTACK_COOLING_DOWN,

            MemoryModuleTypes.ATTACKING.get(),
            MemoryModuleTypes.COOLDOWN_1.get(),
            MemoryModuleTypes.COOLDOWN_2.get(),
            MemoryModuleTypes.COOLDOWN_3.get()
    );

    private float maxStamina;   // 最大体力/耐力
    private byte animId = -1;
    private Vec3 originPos;     // 存储生成的原点

    // 添加BOSS条相关字段
    private ServerBossEvent bossEvent;
    private final Set<ServerPlayer> trackingPlayers = new HashSet<>();

    public Sans(EntityType<? extends Monster> type, Level level) {
        super(type, level);
//        maxStamina = level.getDifficulty().getId() * 5;
        maxStamina = 20;
        setStamina(maxStamina);
        this.moveControl = new PatchedMoveControl(this);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(),BossEvent.BossBarColor.WHITE,BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setPlayBossMusic(true).setDarkenScreen(false);
    }

    @Override
    protected Brain.@NotNull Provider<Sans> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }
    @Override
    protected @NotNull Brain<?> makeBrain(@NotNull Dynamic<?> dynamic) {
        return SansAi.initBrain(this, this.brainProvider().makeBrain(dynamic));
    }
    public @NotNull Brain<Sans> getBrain() { return (Brain<Sans>) super.getBrain();}


    // 残影数据
    private Vec3 phantomStartPos;      // 残影起始位置（10 tick前的位置）
    private int phantomStartTick;       // 残影开始的游戏tick
    private boolean phantomActive = false;  // 残影是否激活
    public static final int PHANTOM_DURATION = 15; // 持续15 tick（可根据需要调整）
    @Override
    public void tick() {
        super.tick();
        // 客户端过期检查（可选，也可以在渲染器中判断）
        if (level().isClientSide() && phantomActive) {
            int elapsed = tickCount - phantomStartTick;
            if (elapsed >= PHANTOM_DURATION) {
                phantomActive = false;
                phantomStartPos = null;
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("sansBrain");
        Brain<Sans> brain = this.getBrain();
        brain.tick((ServerLevel) this.level(), this);
        this.level().getProfiler().pop();
        // 耐力恢复逻辑（仍需要）
        byte phase = getPhaseID();
        float stamina = getStamina();
        if (phase <= FIRST_PHASE && this.tickCount % 40 == 0) {
            stamina = Math.min(stamina + 1, maxStamina);
            setStamina(stamina);
        }
        this.bossEvent.setProgress(stamina / maxStamina);
        if(getTargetFromBrain() != null) {
            for (ServerPlayer p : trackingPlayers) {
                bossEvent.addPlayer(p);
            }
        }
        SansAi.updateActivity(this);
        super.customServerAiStep();
    }



    @Override
    public boolean hurt(@NotNull DamageSource source, float power) {
        Entity sourceEntity = source.getEntity();
        Entity directEntity = source.getDirectEntity();
        this.getBrain().setMemory(MemoryModuleType.HURT_BY, source);
        if (isInvulnerableTo(source) || source.is(Tags.DamageTypes.IS_ENVIRONMENT)) {
            return false;
        }
        float stamina = getStamina();
        if (stamina == 0) {
            return super.hurt(source, power);
        }
        byte phaseID = getPhaseID();
        boolean flag;
        // 直接免疫的
        if (phaseID == MERCY_PHASE) {
            this.entityData.set(PHASE_ID, SECOND_PHASE);
            flag = false;
        } else if (source.is(Tags.DamageTypes.IS_TECHNICAL)) {
            this.setStamina(power);
            return super.hurt(source, power);
        } else if (phaseID == SPECIAL_ATTACK) {
            flag = false;
        } else {
            power *= 0.5f;
            if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                power *= 0.8f;
            }
            if (source.is(Tags.DamageTypes.IS_MAGIC)) {
                power *= 0.9f;
            }
            if (source.is(Tags.DamageTypes.IS_PHYSICAL)) {
                power *= 0.6f;
            }
            flag = true;
        }
        if (flag) {
            if (phaseID == FIRST_PHASE) {
                stamina = Math.max(maxStamina*0.5f, stamina - power);
                this.setStamina(stamina);
                this.bossEvent.setProgress(stamina /maxStamina);
                if (stamina <= maxStamina*0.5f) {
                    this.entityData.set(PHASE_ID, MERCY_PHASE);
                }
            } else if (phaseID == SECOND_PHASE) {
                stamina = Math.max(0, stamina - power);
                this.setStamina(stamina);
                this.bossEvent.setProgress(stamina /maxStamina);
                if (stamina == 0) {
                    //todo 转为特殊阶段
                    this.entityData.set(PHASE_ID, END_PHASE);
                }
            }
        }
        if(sourceEntity != null) {
            if(source.isDirect()){
                log.info("攻击伤害来源实体和直接实体相等，触发近战传送，距离：{}",sourceEntity.distanceTo(this));
                meleeTeleport(sourceEntity);
            }else{
                if(sourceEntity.distanceToSqr(this) <= 25){
                    log.info("攻击伤害来源实体和直接实体不相等，触发近战传送，攻击来源距离：{}",sourceEntity.distanceTo(this));
                    meleeTeleport(sourceEntity);
                }else if(directEntity != null){
                    if(directEntity instanceof LivingEntity){
                        meleeTeleport(directEntity);
                        log.info("攻击伤害来源实体和直接实体不相等，且与攻击来源实体距离超出近战范围，且直接实体是活体，触发近战传送，攻击来源距离：{}",sourceEntity.distanceTo(this));
                    }else{
                        log.info("攻击伤害来源实体和直接实体不相等，且与攻击来源实体距离超出近战范围，且直接实体不是活体，触发范围传送，攻击来源距离：{}",sourceEntity.distanceTo(this));
                        rangedTeleport(directEntity);
                    }
                }
            }
        }else{
            if(directEntity != null){
                if (directEntity.distanceToSqr(this) <= 25) {
                    log.info("没有伤害来源实体，但是有直接实体，距离：{}，触发近身传送",directEntity.distanceTo(this));
                    meleeTeleport(directEntity);
                } else{
                    log.info("没有伤害来源实体，但是有直接实体，距离：{}，触发范围传送",directEntity.distanceTo(this));
                    rangedTeleport(directEntity);
                }
            }else{
                randomTeleport(this.getX() + (random.nextDouble() - 0.5) * 16,
                        this.getY() + (random.nextDouble() - 0.5) * 8,
                        this.getZ() + (random.nextDouble() - 0.5) * 16,true);
            }
        }
        if (sourceEntity instanceof LivingEntity livingEntity) {
            this.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            this.getBrain().setMemory(MemoryModuleType.ANGRY_AT, livingEntity.getUUID());
            this.getBrain().setMemory(MemoryModuleType.HURT_BY_ENTITY, livingEntity);
            this.setLastHurtByMob(livingEntity);
            if (sourceEntity instanceof Player player) {
                this.setLastHurtByPlayer(player);
            }
        }else if(directEntity instanceof LivingEntity livingEntity){
            this.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            this.getBrain().setMemory(MemoryModuleType.ANGRY_AT, livingEntity.getUUID());
            this.getBrain().setMemory(MemoryModuleType.HURT_BY_ENTITY, livingEntity);
            this.setLastHurtByMob(livingEntity);
        }
        return true;
    }


    @Override
    public int getDeathTime() {
        return 40;
    }

    @Override
    public void remove(@NotNull RemovalReason removalReason) {
        this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            SansAi.clearTargetTag(this,target);
        });
        super.remove(removalReason);
    }
    @Override
    public void onRemovedFromLevel() {
        this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            SansAi.clearTargetTag(this,target);
        });
        super.onRemovedFromLevel();
    }


    private void rangedTeleport(@NotNull Entity entity) {
        float baseYaw;
        double r = entity.getBbWidth() + this.getBbWidth() + 2 * getPickRadius();
        Vec3 movement = entity.getDeltaMovement();
        if (movement.lengthSqr() == 0f) {
            // 速度为0时，以指向实体的方向为基准
            baseYaw = RotUtils.yRotD(this.position().subtract(entity.position()));
        } else {
            // 有速度时，以速度方向为基准
            baseYaw = RotUtils.yRotD(movement);
        }
        for (int i = 0; i < 64; i++) {
            // 随机选择左或右（±90°），始终保持垂直
            boolean left = random.nextBoolean();
            double distance = r + 0.5 + random.nextDouble() * 0.5;
            Vec3 offset = RotUtils.getWorldPos(left ? distance : -distance, 0, 0, 0, baseYaw);
            Vec3 to = entity.getEyePosition().add(offset);
            // 检查视线
            Vec3 from = this.getEyePosition();
            if (level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                if (randomTeleport(to.x, to.y, to.z, true)) {
                    return;
                }
            }
        }
    }

    private void meleeTeleport(Entity entity) {
        RandomSource random = this.random;
        for (int i = 0; i < 8; i++) {
            float rand = random.nextFloat();
            float baseAngle = entity.getYHeadRot() - 90f;
            // 概率选择方向：前20%，左右各30%，后50%
            if (rand < 0.2f) {
                baseAngle += 0; // 前方
            } else if (rand < 0.5f) {
                baseAngle += random.nextBoolean() ? -90 : 90; // 左右
            } else {
                baseAngle += 180; // 后方
            }
            // 在选定方向尝试传送
            for (int j = 0; j < 8; j++) {
                float angle = baseAngle + (random.nextFloat() - 0.5f) * 90f; // 90度范围内随机
                double distance = getFollowRange()*SansAi.MID_RANGE_FACTOR - random.nextDouble() * 4.0;
                double targetX = entity.getX() + Mth.cos(angle * Mth.DEG_TO_RAD) * distance;
                double targetY = entity.getY() + random.nextDouble() * 16 - 8;
                double targetZ = entity.getZ() + Mth.sin(angle * Mth.DEG_TO_RAD) * distance;
                // 检查视线
                Vec3 from = new Vec3(targetX, targetY, targetZ);
                Vec3 to = entity.getEyePosition();
                if (level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                    if (randomTeleport(targetX, targetY, targetZ,true)) {
                        log.info("followRange：{},近战传送距离：{}",getFollowRange(),distance);
                        return;
                    }
                }
            }
        }
    }

    public void teleportTowards(Entity target) {
        Vec3 dir = new Vec3(target.getX() - Sans.this.getX(), target.getEyeY() - Sans.this.getEyeY(), target.getZ() - Sans.this.getZ());
        dir = dir.normalize().scale(getFollowRange() / 2);
        randomTeleport(
                this.getX() + dir.x + (random.nextDouble() - 0.5) * 4,
                this.getY() + dir.y + (random.nextDouble() - 0.5) * 16,
                this.getZ() + dir.z + (random.nextDouble() - 0.5) * 4 ,
                true
        );
    }


    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        LivingEntity target = this.getTargetFromBrain();
        if(target == player) {
            this.bossEvent.addPlayer(player);
            SansAi.applyTargetTag(this,player);
        }else{
            trackingPlayers.add(player);
        }
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        super.stopSeenByPlayer(player);
        SansAi.clearTargetTag(this,player);
        trackingPlayers.remove(player);
        this.bossEvent.removePlayer(player);
    }

    public float getMaxStamina() {
        return maxStamina;
    }

    public float getStamina() {
        return this.entityData.get(STAMINA);
    }

    public void setStamina(float stamina) {
        this.entityData.set(STAMINA, stamina);
    }
    public byte getPhaseID() {
        return this.entityData.get(PHASE_ID);
    }
    public void setPhaseID(byte phaseID) {
        this.entityData.set(PHASE_ID, phaseID);
    }
    public boolean getIsEyeBlink() {
        return this.entityData.get(IS_EYE_BLINK);
    }
    public void setIsEyeBlink(boolean blink) {
        this.entityData.set(IS_EYE_BLINK, blink);
    }

    public Vec3 getPhantomStartPos() { return phantomStartPos; }
    public int getPhantomStartTick() { return phantomStartTick; }
    public boolean isPhantomActive() { return phantomActive; }
    @Override
    public byte getAnimID() {
        return animId;
    }
    @Override
    public void setAnimID(byte id) {
        this.animId = id;
    }
    @Override
    public @Nullable LivingEntity getTarget() {
        if(this.level().isClientSide){
            return (LivingEntity) this.level().getEntity(getTargetId());
        }else{
            return getTargetFromBrain();
        }
    }
    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }
    public void setTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
    }

    public int getFactor(){
        return getPhaseID() == SECOND_PHASE?1:0;
    }
    public int getDifficulty(){
        return this.level().getDifficulty().getId();
    }
    public double getFollowRange(){
        return this.getAttributeValue(Attributes.FOLLOW_RANGE);
    }
    public float getAttackDamage(){
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAMINA, maxStamina);
        builder.define(PHASE_ID, OPENING_ATTACK);
        builder.define(IS_EYE_BLINK, true);
        builder.define(TARGET_ID, -1);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(maxStamina);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.maxStamina = buffer.readFloat();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("maxStamina", this.maxStamina);
        tag.putFloat("stamina", this.getStamina());
        tag.putByte("phaseId", this.getPhaseID());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("maxStamina")) {
            this.maxStamina = tag.getFloat("maxStamina");
        }
        if (tag.contains("stamina")) {
            setStamina(tag.getFloat("stamina"));
        }
        if (tag.contains("phaseId")) {
            this.entityData.set(PHASE_ID, tag.getByte("phaseId"));
        }
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 36f);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if(id == 46){
            phantomStartPos = position();
            phantomStartTick = tickCount;
            phantomActive = true;
        }else{
            super.handleEntityEvent(id);
        }
    }


    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        originPos = this.position();
    }

    class MercyGoal extends Goal {
        @Override
        public boolean canUse() {
            return getPhaseID() == MERCY_PHASE;
        }

    }

    /**
     * 瞄准目标随机骨头弹幕 - 持续射击目标
     *
     * @return 需要执行完攻击的动画CD
     */
    public int shootAimedBarrage(LivingEntity target) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int difficulty = this.level().getDifficulty().getId();
        int factor = getFactor();
        float speed = 1.0f + factor * 0.3f + difficulty * 0.5f;
        int delay = 15 - factor * 5;
        int count = 10 * (factor + difficulty);
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("Sans疲劳程度：%d，飞行骨速度：%f，数量：%d", factor, speed, count)), false);
        }
        // 三层高度：腿、身体、眼睛
        double[] heights = {target.getEyeY(), target.getY(0.5), target.getY(0.15),};
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            do {
                bone.setPos(this.position().add(RotUtils.getWorldPos(
                        (this.random.nextDouble() - 0.5) * (6 + 3 * (factor + difficulty)),
                        this.random.nextDouble() * (2 + difficulty + factor) + this.getBbHeight() * 0.5f,
                        this.random.nextDouble() * (3 + difficulty + factor),
                        this.getXRot(), this.getYHeadRot()
                )));
            } while (this.level().noBlockCollision(bone, bone.getBoundingBox()) && !this.level().getEntities(bone, bone.getBoundingBox()).isEmpty() && ++attempts < 16);
            bone.aimShoot();
            RotUtils.lookAtShoot(bone, target.getX(), heights[this.random.nextInt(heights.length)], target.getZ());
            level().addFreshEntity(bone);
            delay += 6 - difficulty - factor;
        }
        return delay;
    }

    /**
     * 射向前方随机骨头弹幕 - 以目标碰撞高度为随机范围高度，向前方范围随机射击
     */
    public int shootForwardBarrage(LivingEntity target) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int difficulty = this.level().getDifficulty().getId();
        int factor = getFactor();
        float speed = 0.8f + (factor + difficulty) * 0.1f;
        int delay = 15 - 5 * factor;
        int count = 10 * (factor + difficulty);
        float targetBbHeight = target.getBbHeight();
        // Sans的视线方向（固定射击方向）
        Vec3 eyeLookAngle = this.getViewVector(1.0f);
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            do {
                // 计算相对于视线方向的偏移（在局部坐标系）
                float offsetX = (float) this.random.nextGaussian() * 0.333333f * (1.0f + (factor + difficulty) * 0.5f);  // 左右
                float offsetY = Mth.clamp(((float) this.random.nextGaussian() * 0.1666667f + 0.5f) * targetBbHeight, 0, targetBbHeight);     // 上下
                bone.setPos(this.position().add(RotUtils.getWorldPos(offsetX, offsetY, 1f, this.getXRot(), this.getYHeadRot())));
                bone.followAngleShoot(new Vec3(offsetX, offsetY, 1f));
            } while (this.level().noBlockCollision(bone, bone.getBoundingBox()) && !this.level().getEntities(bone, bone.getBoundingBox()).isEmpty() && ++attempts < 16);
            RotUtils.lookVecShoot(bone, eyeLookAngle);
            this.level().addFreshEntity(bone);
            delay += 6 - difficulty - factor;
        }
        return delay;
    }


    /**
     * 骨环齐射 - 向目标射击
     */
    public void shootBoneRingVolley(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        double[] offsetXs = getPhaseID() == FIRST_PHASE ? new double[]{1.0} : new double[]{1.0, -1.0};
        float speed = 1.0f + (getFactor() + difficulty) * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay = 0;
        for (double x : offsetXs) {
            delay = 9;
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            bone.aimShoot();
            LevelUtils.addFreshProjectile(this.level(), bone, RotUtils.getWorldPos(x, 1.5f, 0, this.getXRot(), this.getYHeadRot())
                    .add(this.getX(), this.getY(0.5f), this.getZ()), target);
            for (int l = 0; l < 2; l++) {
                delay += 5 - difficulty - getFactor();
                int count = (l + 1) * (6 + difficulty);
                float radius = (l + 1) * 0.5f;
                float interval = 360f / count;
                float angle = interval; //起始位置偏移，每层错位分布
                if (!FMLEnvironment.production) {
                    Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("第%d圈数量：%d，间隔：%f，偏移角度：%f", l + 1, count, interval, angle)), false);
                }
                for (int i = 0; i < count; i++, angle += interval) {
                    bone = createFlyingBone(attackTypeUUID, speed, delay);
                    bone.aimShoot();
                    LevelUtils.addFreshProjectile(this.level(), bone, RotUtils.getWorldPos(
                            x + radius * Mth.cos(angle * Mth.DEG_TO_RAD),
                            1.5f + radius * Mth.sin(angle * Mth.DEG_TO_RAD),
                            0,
                            this.getXRot(), this.getYHeadRot()
                    ).add(this.getX(), this.getY(0.5f), this.getZ()), target);
                }
            }
        }
    }

    /**
     * 弧形横扫齐射 - 骨头在圆弧上朝外径向发射
     */
    public void shootArcSweepVolley() {
        int difficulty = this.level().getDifficulty().getId();
        int factor = getFactor();
        float speed = 1.0f + (difficulty + factor) * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = difficulty * 8 | 1; // 或1，确保是奇数
        float interval = 10f - difficulty - factor;
        float[] offsetAngles = getPhaseID() == 1 ? new float[]{0} : new float[]{-30, 30};
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("弧形横扫，数量：%d,角度间隔：%f", count, interval)), false);
        }
        // 圆心位置（实体位置）
        Vec3 center = new Vec3(this.getX(), this.getY(0.5f), this.getZ());
        for (float offsetAngle : offsetAngles) {
            int middleIndex = (count - 1) / 2;
            float angle = -middleIndex * interval;
            for (int i = 0; i < count; i++, angle += interval) {
                Vec3 worldOffsetPos = RotUtils.getWorldPos(Mth.sin(angle * Mth.DEG_TO_RAD), 0, 0.8f * Mth.cos(angle * Mth.DEG_TO_RAD), this.getXRot(), this.getYHeadRot()+offsetAngle);
                FlyingBone bone = new FlyingBone(EntityTypes.FLYING_BONE.get(), this.level(), this, 1f, speed, worldOffsetPos);
                bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
                LevelUtils.addFreshProjectileByVec3(this.level(), bone, center.add(worldOffsetPos), worldOffsetPos);
            }
        }
    }
    protected FlyingBone createFlyingBone(String attackTypeUUID, float speed, int delay) {
        FlyingBone bone = new FlyingBone(EntityTypes.FLYING_BONE.get(), this.level(), this, getAttackDamage(), speed, delay);
        bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
        return bone;
    }




    public void summonGroundBoneWallAroundTarget(LivingEntity target, ColorAttack colorAttack, float height) {
        summonGroundBoneWall(target, colorAttack, height, LocalDirection.FRONT, 5, 12.0);
        summonGroundBoneWall(target, colorAttack, height, LocalDirection.BACK, 5, 12.0);
        summonGroundBoneWall(target, colorAttack, height, LocalDirection.LEFT, 5, 12.0);
        summonGroundBoneWall(target, colorAttack, height, LocalDirection.RIGHT, 5, 12.0);
    }
    /**
     * 在指定方向召唤前进骨墙
     */
    public int summonGroundBoneWall(LivingEntity target, ColorAttack color, float growScale, LocalDirection direction, int delay, double distance) {
        Level level = this.level();
        int difficulty = level.getDifficulty().getId();
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = (7 + difficulty * 4) * (color == ColorAttack.AQUA ? 2 : 1);
        float speed = 0.25F;
        float spacing = 0.375f;
        float xOffset = -spacing * (count - 1) * 0.5f;
        // 获取施法者旋转角度
        float casterYRot = this.getYHeadRot();
        // 计算骨砖朝向角度
        float boneLookYRot = switch (direction) {
            case BACK -> casterYRot + 180;   // 后方
            case LEFT -> casterYRot - 90;    // 左侧
            case RIGHT -> casterYRot + 90;   // 右侧
            default -> casterYRot;           // 前方
        };
        // 计算生成中心位置
        Vec3 lookVector = calculateViewVector(0, boneLookYRot);
        Vec3 centerPos = target.position().add(lookVector.scale(-distance));
        centerPos = new Vec3(centerPos.x, this.getY(), centerPos.z);
        for (int i = 0; i < count; i++) {
            // 计算每个骨砖的位置
            Vec3 finalPos = centerPos.add(RotUtils.yRot(new Vec3(xOffset, 0, 0), boneLookYRot));
            MovingGroundBone bone = new MovingGroundBone(level, this, 1.0f, growScale, delay, speed,getAttackDamage(), color);
            bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
            // 设置位置和朝向
            bone.setPos(finalPos);
            RotUtils.lookVec(bone, lookVector);
            level.addFreshEntity(bone);
            xOffset += spacing;
        }
        return delay;
    }

    /**
     * 在指定方向召唤前进骨墙矩阵
     */
    public void summonGroundBoneMatrix(LivingEntity target, float growScale) {
        Level level = this.level();
        int difficulty = target.level().getDifficulty().getId();
        int cols = (7 + difficulty * 4) * 2;
        int rows = 14 + difficulty * 2;
        String attackTypeUUID = UUID.randomUUID().toString();
        float speed = 0.35F;
        float scale = 1.0f;
        float spacing = 0.375f * scale;
        float xOffset = -spacing * (cols - 1) * 0.5f;
        // 计算骨砖朝向角度
        float boneLookYRot = this.getYHeadRot();
        // 计算生成中心位置
        Vec3 lookVector = calculateViewVector(0, boneLookYRot);
        Vec3 centerPos = target.position().add(lookVector.scale(-12.0f));
        centerPos = new Vec3(centerPos.x, this.getY(), centerPos.z);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // 计算每个骨砖的位置
                MovingGroundBone bone = new MovingGroundBone(level, this, scale, growScale * (r > rows * 0.5f ? 4f : 1f), speed, lookVector,getAttackDamage(), ColorAttack.WHITE);
                bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
                // 设置位置和朝向
                bone.setPos(centerPos.add(RotUtils.yRot(new Vec3(xOffset + c * spacing, 0, -r * spacing), boneLookYRot)));
                RotUtils.lookVec(bone, lookVector);
                level.addFreshEntity(bone);
            }
        }
    }

    /**
     * 带相对前方计算的正弦波缺口骨头矩阵
     *
     * @param factor 影响矩阵难度的因子，可影响周期和振幅
     */
    public void summonLateralBoneMatrix(LivingEntity target, int factor) {
        Level level = this.level();
        int difficulty = level.getDifficulty().getId();
        String id = UUID.randomUUID().toString();
        float yRot = this.getYHeadRot();
        float baseWidth = target.getBbWidth() * 1.66666667f;
        float width = 10f * baseWidth;
        float speed = 0.3f + 0.05f * difficulty;
        float gap = 2.5f * baseWidth;
        int rows = 7 * (3 + difficulty + factor);
        int cols = (int) (target.getBbHeight() * 2);
        float spacing = 0.75f;
        // 缺口中心的最大偏移量（确保骨头宽度>=0）
        int i = this.random.nextBoolean() ? 1 : -1;
        for (int r = 0; r < rows; r++) {
            float t = r / (float) (rows - 1);
            float x = i * Mth.sin(t * Mth.PI * (6 + factor + factor) * 0.25f) * width * (2 + difficulty + factor + factor) * 0.1f - (i * width * 0.1f * (float) this.random.nextGaussian() * 0.033333f + 0.1f);
            float leftWidth = (width + x) - gap * 0.5f;
            float rightWidth = (width - x) - gap * 0.5f;
            for (int c = 0; c < cols; c++) {
                // 创建左侧骨头
                LateralBone leftBone = new LateralBone(level, this, 1.0f, leftWidth, speed, calculateViewVector(0, yRot), (float) getAttributeValue(Attributes.ATTACK_DAMAGE), ColorAttack.WHITE);
                leftBone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(id, (byte) 6));
                leftBone.setPos(RotUtils.yRot(new Vec3(-width + leftWidth * 0.5f, c * spacing, -r * spacing), yRot).add(this.position()));
                level.addFreshEntity(leftBone);
                // 创建右侧骨头
                LateralBone rightBone = new LateralBone(level, this, 1.0f, rightWidth, speed, calculateViewVector(0, yRot), (float) getAttributeValue(Attributes.ATTACK_DAMAGE), ColorAttack.WHITE);
                rightBone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(id, (byte) 6));
                rightBone.setPos(RotUtils.yRot(new Vec3(width - rightWidth * 0.5f, c * spacing, -r * spacing), yRot).add(this.position()));
                level.addFreshEntity(rightBone);
            }
        }
    }


    /**
     * 骨刺波动 - 以自身为中心发射，单击
     */
    public void summonGroundBoneSpineWaveAroundSelf(@NotNull LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        int factor = getFactor();
        int count = 3 + 2 * factor;
        double distance = this.distanceTo(target);
        float maxAngle = 25f;   // 距离0时的角度（可调）
        float slope = 1.05F;        // 每格减小的角度（可调）
        float interval = maxAngle - slope * (float) distance;
        interval = Mth.clamp(interval, 5f, maxAngle); // 限制在5°～maxAngle之间
        Vec3 centerPos = target.position().subtract(this.position()).scale(0.5f).add(this.position());
        this.level().playSound(null, centerPos.x, centerPos.y, centerPos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("骨刺波动，数量：%d,角度间隔：%f", count, interval)), false);
        }
        float yaw = getYHeadRot();
        Vec3 horLookAngle = calculateViewVector(0, yaw);
        int rows = (int) getFollowRange() * 2 + (difficulty + factor) * 2;
        int cols = 2 + difficulty + 2*factor;
        // 计算起始角度（让波动对称分布）
        float angle = interval * (1 - count) * 0.5f;
        // 矩阵跨度（标量）
        float length = (rows - 1) * 0.6f;          // 方向方向总距离（骨刺中心之间）
        float width = (cols - 1) * 0.375f;   // 垂直方向最大偏移（中心到最远骨刺）
        float growScale = 1.0f+(getMaxStamina() - getStamina()) /getMaxStamina()*3f;
        for (int i = 0; i < count; i++, angle += interval) {
            PacketDistributor.sendToPlayersTrackingEntity(this,new WarningTipPacket.Cube((float) getX(), (float) Math.min(this.getY(),target.getY()), (float) getZ(),length, width+0.1f, growScale,yaw+angle,20,WarningTip.RED));
            summonGroundBoneSpineWave(target, rows, cols, this.position(), horLookAngle.yRot(angle * Mth.DEG_TO_RAD), ColorAttack.WHITE, 10);
        }
    }

    /**
     * 公用方法：召唤定向骨刺波动
     */
    public void summonGroundBoneSpineWave(LivingEntity target, int rows, int cols, Vec3 startPos, Vec3 direction, ColorAttack color, int delay) {
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        float colSpacing = 0.375f;
        float rowSpacing = 0.6f;
        // 计算垂直方向
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x);
        float growScale = 1.0f+(getMaxStamina() - getStamina()) /getMaxStamina()*3.0f;
        // 生成 rows×cols 骨刺矩阵
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // 计算当前位置
                double xOffset = (col - (cols - 1) * 0.5) * colSpacing;
                double zOffset = row * rowSpacing;
                Vec3 bonePos = startPos.add(perpendicular.scale(xOffset)).add(direction.scale(zOffset));
                createGroundBone(attackTypeUUID, bonePos.x, bonePos.z, minY, maxY, 1f, growScale, 20, delay, color, false);
            }
            // 每3行增加1tick延迟，行越少，波浪越窄
            delay += (row % 3 == 0 ? 1 : 0);
        }
    }


    /**
     * 在自身位置召唤地面骨刺扩张（圆形）
     */
    public void summonGroundBoneSpineAtSelf() {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 pos = this.position();
        String attackTypeUUID = UUID.randomUUID().toString();
        int factor = getFactor();
        int layer = 6 + 2 * (factor + difficulty);
        int delay = 13 - factor - difficulty;
        float spacing = 0.7f;
        float growScale = 1.0f+(getMaxStamina() - getStamina()) /getMaxStamina()*3.0f;
        double groundY = createGroundBone(attackTypeUUID, pos.x, pos.z, pos.y, pos.y, 1.0f,growScale, 20, delay, ColorAttack.WHITE, true);
        this.level().playSound(null, pos.x, pos.y, pos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Cylinder((float) pos.x, (float) groundY, (float) pos.z, layer * spacing, growScale, 20, WarningTip.RED));
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = spacing * (i + 1);
            float angle = interval; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                createGroundBone(attackTypeUUID, pos.x + r * Mth.cos(angle * Mth.DEG_TO_RAD), pos.z + r * Mth.sin(angle * Mth.DEG_TO_RAD),
                        pos.y, pos.y, 1.0f, growScale, 20, delay, ColorAttack.WHITE, false);
            }
            delay += (i % 3 == 0 ? 1 : 0);
        }
    }


    /**
     * 以目标位置为中心的圆形骨刺
     *
     * @param target 目标
     * @param layer  圆环层数
     */
    public void summonCircleGroundBoneSpine(LivingEntity target, int layer, float growScale, int lifetime, int delay) {
        String attackTypeUUID = UUID.randomUUID().toString();
        float spacing = 0.7f;
        Vec3 centerPos = target.position();
        Gravity data = target.getData(AttachmentTypes.GRAVITY);
        Direction gravity = data.getGravity();
        createGroundBone(attackTypeUUID,centerPos, 1.0f, growScale, lifetime, delay, ColorAttack.WHITE, true,gravity);
        this.level().playSound(null, centerPos.x, getY(), centerPos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Cylinder((float) centerPos.x, (float) centerPos.y, (float) centerPos.z, layer * spacing, growScale, lifetime,WarningTip.RED,gravity));
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = spacing * (i + 1);
            float angle = interval; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                Vec3 pos = centerPos.add(data.localToWorld(r * Math.cos(angle * Math.PI / 180F), 0, r * Math.sin(angle * Math.PI / 180F)));
                pos = GravityUtils.findGround(this.level(),new Vec3(Math.round(pos.x * 1e6) / 1e6,Math.round(pos.y * 1e6) / 1e6,Math.round(pos.z * 1e6) / 1e6), gravity);
                createGroundBone(attackTypeUUID, pos,1.0f, growScale, lifetime, delay, ColorAttack.WHITE, false,gravity);
            }
        }
    }


    /**
     * 创建应用曲线的地面骨
     *
     * @param scale     地面骨缩放大小
     * @param growScale 地面骨生长缩放大小
     */
    protected double createGroundBone(String attackUUID,  double targetX, double targetZ, double minY, double maxY,float scale, float growScale, int lifetime, int delay, ColorAttack colorAttack, boolean isPlaySound) {
        Level level = this.level();
        double spawnY = EntityUtils.findGroundY(level, targetX, targetZ, minY, maxY);
        // 如果找到有效地面，生成骨刺
        if (spawnY != level.getMinBuildHeight()) {
            GroundBone bone = new GroundBone(level, this, scale, growScale,getAttackDamage(), lifetime, delay, colorAttack, isPlaySound, true);
            bone.setPos(targetX, spawnY, targetZ);
            bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackUUID, (byte) 6));
            // 设置旋转：骨刺指向圆心（目标位置）
            bone.setYRot(this.getYHeadRot());
            // 添加随机X旋转，看起来不规则
            bone.setXRot((float) this.random.nextGaussian() * 10);
            level.addFreshEntity(bone);
            level.gameEvent(GameEvent.ENTITY_PLACE, new Vec3(targetX, spawnY, targetZ), GameEvent.Context.of(this));
        }
        return spawnY;
    }
    protected void createGroundBone(String attackUUID, Vec3 pos, float scale, float growScale, int lifetime, int delay, ColorAttack colorAttack, boolean isPlaySound, Direction gravity){
        Level level = this.level();
        GroundBone bone = new GroundBone(level, this, scale, growScale, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), lifetime, delay, colorAttack, isPlaySound, true,gravity);
        bone.setPos(pos);
        bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackUUID, (byte) 6));
        // 设置旋转：骨刺指向圆心（目标位置）
//        bone.setYRot(this.getYHeadRot());
        // 添加随机X旋转，看起来不规则
//        bone.setXRot((float) this.random.nextGaussian() * 10);
        level.addFreshEntity(bone);
        level.gameEvent(GameEvent.ENTITY_PLACE,pos, GameEvent.Context.of(this));
    }


    /**
     * 在自身周围随机位置召唤GB
     */
    public void summonGBAroundSelf(LivingEntity target, int count, float size) {
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            GasterBlaster gb = createGasterBlaster(size, 17, 28);
            do {
                gb.setPos(this.getEyePosition().subtract(target.getEyePosition()).scale(0.5).add(this.getEyePosition()).add(RotUtils.getWorldPos(
                        this.random.nextDouble() * 16 - 8,
                        this.random.nextDouble() * 3 + 1,
                        this.random.nextDouble() * 4,
                        this.getXRot(), this.getYHeadRot()
                )));
            } while (this.level().noBlockCollision(gb, gb.getBoundingBox()) && !this.level().getEntities(gb, gb.getBoundingBox()).isEmpty() && ++attempts < 16);
            gb.aim(target);
            this.level().addFreshEntity(gb);
        }
    }

    /**
     * 用于单击，环绕目标周围
     */
    public void summonGBAroundTarget(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        // 简单2~4 普通2~5 困难2~6
        int count = 2 + this.random.nextInt(1 + difficulty + getFactor());
        this.summonGBAroundTarget(target, count, this.random.nextInt(count) * 180f / count);
    }
    /**
     * 以目标和自身长度为半径的圆环上召唤GB，固定360度范围，根据数量自动计算角度步长和大小
     */
    public void summonGBAroundTarget(LivingEntity target, int count, float offsetAngle) {
        int difficulty = this.level().getDifficulty().getId();
        summonGBAroundTarget(target, count, target.getBbWidth() * 16, offsetAngle, 360f / count, 1.0f + (2 + difficulty - count) * 0.5f, 17, 28);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB，在自身前方对称召唤GB，用于自定义的序列攻击，
     */
    public void summonGBFront(LivingEntity target, int count, float angleStep, int charge) {
        int difficulty = this.level().getDifficulty().getId();
        summonGBAroundTarget(target, count, this.distanceTo(target)*0.9f, (1 - count) * angleStep * 0.5f, angleStep, 1.0f + (1 + difficulty + getFactor() - count) * 0.25f, charge, 28);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB
     *
     * @param target      目标实体
     * @param count       GB数量
     * @param offsetAngle 初始偏移角度（度）
     * @param angleStep   角度步长（度）
     */
    public void summonGBAroundTarget(LivingEntity target, int count, float radius, float offsetAngle, float angleStep, float size, int charge, int shot) {
        Vec3 targetPos = new Vec3(target.getX(), target.getY(0.5f), target.getZ());
        float currentAngle = offsetAngle; // 从指定角度开始
        for (int i = 0; i < count; i++, currentAngle += angleStep) {
            GasterBlaster gb = createGasterBlaster(size, charge, shot);
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = -Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            gb.setPos(RotUtils.getWorldPos(xOffset, 0, zOffset, this.getXRot(), this.getYHeadRot()).add(targetPos));
            gb.aim(target);
            this.level().addFreshEntity(gb);
        }
    }
    public int controlGBAim(LivingEntity target) {
        int factor = getFactor();
        int difficulty = getDifficulty();
        float size = 0.5f+difficulty*0.3334f+factor*0.5f;
        GasterBlaster gb = createGasterBlaster(size, (20-factor*10), (int) (100*size)).follow(new Vec3(0, this.getBbHeight()*0.5f, 1))
                .aimSmoothSpeed(0.1f+difficulty*0.02f+factor*0.04f);
        gb.aim(target);
        this.level().addFreshEntity(gb);
        return gb.getDecayTick();
    }

    public GasterBlaster createGasterBlaster(float size, int charge, int shot) {
        GasterBlaster gb = new GasterBlaster(this.level(), this,getAttackDamage(), size, charge, shot);
        gb.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(UUID.randomUUID().toString(), (byte) 10));
        return gb;
    }

    public void gravitySlam(LivingEntity target, LocalDirection direction, float acceleration) {
        Gravity gravityData = Gravity.applyRelativeGravity(this, target, direction);
        target.addDeltaMovement(new Vec3(0,-acceleration,0));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new GravityPacket(target.getId(), gravityData.getGravity(),acceleration));
    }
    public void timeJumpTeleport(LivingEntity target, int duration) {
        PacketDistributor.sendToPlayersTrackingEntity(this, new TimeJumpTeleportPacket(target.getId(), duration));
        this.teleportTo(originPos.x, originPos.y, originPos.z);
        target.teleportTo(originPos.x, originPos.y, originPos.z+getFollowRange()*0.5f);
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvnets.SANS_TELEPORT_TIME_JUMP.get(), SoundSource.HOSTILE);
    }
    public void controlSoulMode(LivingEntity target, byte soulState){
        target.setData(AttachmentTypes.SOUL_MODE, soulState);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,new SoulModePacket(target.getId(), soulState));
    }
    public void applyKarma(LivingEntity target,boolean exist){
        target.setData(AttachmentTypes.KARMA_TAG,exist);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,new KaramTagPacket(target.getId(),exist));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static final RawAnimation ANIM_CAST = RawAnimation.begin().thenPlay("attack.cast");
    private static final RawAnimation ANIM_CAST_LEFT = RawAnimation.begin().thenPlay("attack.cast.left");
    private static final RawAnimation ANIM_CAST_CIRCLE = RawAnimation.begin().thenPlay("attack.cast.circle");
    private static final RawAnimation ANIM_CAST_CIRCLE_LEFT = RawAnimation.begin().thenPlay("attack.cast.circle.left");
    private static final RawAnimation ANIM_BONE_PROJECTILE = RawAnimation.begin().thenPlay("attack.bone.projectile");
    private static final RawAnimation ANIM_BONE_PROJECTILE_LEFT = RawAnimation.begin().thenPlay("attack.bone.projectile.left");
    private static final RawAnimation ANIM_BONE_SWEEP = RawAnimation.begin().thenPlay("attack.bone.sweep");
    private static final RawAnimation ANIM_BONE_SWEEP_LEFT = RawAnimation.begin().thenPlay("attack.bone.sweep.left");
    private static final RawAnimation ANIM_GB_FOLLOW = RawAnimation.begin().thenPlay("attack.gb.follow");
    private static final RawAnimation[] THROW_ANIMATIONS = new RawAnimation[]{
            RawAnimation.begin().thenPlay("attack.throw.up"),
            RawAnimation.begin().thenPlay("attack.throw.down"),
            RawAnimation.begin().thenPlay("attack.throw.left"),
            RawAnimation.begin().thenPlay("attack.throw.right"),
            RawAnimation.begin().thenPlay("attack.throw.front"),
            RawAnimation.begin().thenPlay("attack.throw.back")
    };
    private int lastTickCount = -1;
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Sans> attackController = new AnimationController<>(this, "attack", state -> {
            AnimationController<Sans> controller = state.getController();
            if(animId == -1){
                return PlayState.STOP;
            }
            if(state.isCurrentAnimation(ANIM_CAST_LEFT)||state.isCurrentAnimation(ANIM_CAST_CIRCLE_LEFT) ){
                if(tickCount != lastTickCount){
                    Map<String, BoneAnimationQueue> boneAnimationQueues = state.getController().getBoneAnimationQueues();
                    BoneAnimationQueue leftHand = boneAnimationQueues.get("left_hand");
                    Vector3d worldPosition = leftHand.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                    lastTickCount = tickCount;
                }
            }
            if(state.isCurrentAnimation(ANIM_CAST)||state.isCurrentAnimation(ANIM_CAST_CIRCLE)){
                if(tickCount != lastTickCount){
                    Map<String, BoneAnimationQueue> boneAnimationQueues = state.getController().getBoneAnimationQueues();
                    BoneAnimationQueue leftHand = boneAnimationQueues.get("left_hand");
                    Vector3d worldPosition = leftHand.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                    BoneAnimationQueue rightHand = boneAnimationQueues.get("right_hand");
                    worldPosition = rightHand.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                    lastTickCount = tickCount;
                }
            }

            if(!controller.hasAnimationFinished() && animId == -2){
                return PlayState.CONTINUE;
            }
            byte phaseID = getPhaseID();
            boolean isFirstPhase = phaseID == FIRST_PHASE;
            controller.setAnimationSpeed(1.0f);
            switch (animId) {
                case 0, 1, 2, 3, 4, 5 -> controller.setAnimation(THROW_ANIMATIONS[animId]);
                case 6 -> controller.setAnimation(isFirstPhase?ANIM_CAST_LEFT:ANIM_CAST);
                case 7 -> controller.setAnimation(isFirstPhase?ANIM_CAST_CIRCLE_LEFT:ANIM_CAST_CIRCLE);
                case 8 -> controller.setAnimation(isFirstPhase ? ANIM_BONE_PROJECTILE_LEFT : ANIM_BONE_PROJECTILE);
                case 9 -> controller.setAnimation(isFirstPhase ? ANIM_BONE_SWEEP_LEFT : ANIM_BONE_SWEEP);
                case 10 -> {
                    controller.setAnimation(ANIM_GB_FOLLOW);
                    controller.setAnimationSpeed(0.5*(1+getFactor()));
                }
            }
            log.info("执行动画ID：{}",animId);
            animId = -2;
            log.info("清理动画ID：{}",animId);
            controller.forceAnimationReset();
            return PlayState.CONTINUE;
        });
        controllers.add(
                DefaultAnimations.genericWalkIdleController(this),
                DefaultAnimations.genericLivingController(this),
                attackController
        );
    }

}

