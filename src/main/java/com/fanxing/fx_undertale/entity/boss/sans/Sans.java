package com.fanxing.fx_undertale.entity.boss.sans;

import com.fanxing.fx_undertale.client.effect.WarningTip;
import com.fanxing.fx_undertale.common.phys.LocalDirection;
import com.fanxing.fx_undertale.common.phys.motion.SpringMotionModel;
import com.fanxing.fx_undertale.entity.AbstractUTMonster;
import com.fanxing.fx_undertale.entity.IAnimatable;
import com.fanxing.fx_undertale.entity.ai.control.PatchedMoveControl;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.entity.attachment.KaramJudge;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.entity.projectile.FlyingBone;
import com.fanxing.fx_undertale.entity.projectile.RotationBone;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.fx_undertale.entity.summon.GroundBone;
import com.fanxing.fx_undertale.entity.summon.ObbBone;
import com.fanxing.fx_undertale.entity.summon.MovingGroundBone;
import com.fanxing.fx_undertale.net.packet.*;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.registry.MemoryModuleTypes;
import com.fanxing.fx_undertale.registry.SoundEvnets;
import com.fanxing.fx_undertale.utils.*;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
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
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import java.util.function.Function;

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


    protected static final List<SensorType<? extends Sensor<? super Sans>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY);
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

    private GasterBlaster controllerAimGB = null;
    public boolean shotA;

    public Sans(EntityType<? extends Monster> type, Level level) {
        super(type, level);
//        maxStamina = level.getDifficulty().getId() * 5;
        maxStamina = 20;
        setStamina(maxStamina);
        this.moveControl = new PatchedMoveControl(this);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
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

    public @NotNull Brain<Sans> getBrain() {
        return (Brain<Sans>) super.getBrain();
    }


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
        if (this.getIsEyeBlink() && this.level().isClientSide && this.tickCount % 2 == 0) {
            Vec3 vec3 = RotUtils.getWorldVec3(this.getAttachments().get(EntityAttachment.WARDEN_CHEST, 0, 0), this.getXRot(), this.getYHeadRot()).add(this.position());
            long time = this.tickCount;
            float cycle = (Mth.sin(time * 1.0f) + 1.0f) / 2.0f; // 0.1f 控制变化速度
            int colorA = 0xC061E5DF;// 原本是 0xFF61E5DF
            int colorB = 0xC0F6FD29;// 原本是 0xFFF6FD29
            this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.lerp(cycle, colorA, colorB)), vec3.x, vec3.y, vec3.z, 0, 0, 0);
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
        if (getTargetFromBrain() != null) {
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
                stamina = Math.max(maxStamina * 0.5f, stamina - power);
                this.setStamina(stamina);
                this.bossEvent.setProgress(stamina / maxStamina);
                if (stamina <= maxStamina * 0.5f) {
                    this.entityData.set(PHASE_ID, MERCY_PHASE);
                }
            } else if (phaseID == SECOND_PHASE) {
                stamina = Math.max(0, stamina - power);
                this.setStamina(stamina);
                this.bossEvent.setProgress(stamina / maxStamina);
                if (stamina == 0) {
                    //todo 转为特殊阶段
                    this.entityData.set(PHASE_ID, END_PHASE);
                }
            }
        }
        if (sourceEntity != null) {
            if (source.isDirect()) {
                log.info("攻击伤害来源实体和直接实体相等，触发近战传送，距离：{}", sourceEntity.distanceTo(this));
                meleeTeleport(sourceEntity);
            } else {
                if (sourceEntity.distanceToSqr(this) <= 25) {
                    log.info("攻击伤害来源实体和直接实体不相等，触发近战传送，攻击来源距离：{}", sourceEntity.distanceTo(this));
                    meleeTeleport(sourceEntity);
                } else if (directEntity != null) {
                    if (directEntity instanceof LivingEntity) {
                        meleeTeleport(directEntity);
                        log.info("攻击伤害来源实体和直接实体不相等，且与攻击来源实体距离超出近战范围，且直接实体是活体，触发近战传送，攻击来源距离：{}", sourceEntity.distanceTo(this));
                    } else {
                        log.info("攻击伤害来源实体和直接实体不相等，且与攻击来源实体距离超出近战范围，且直接实体不是活体，触发范围传送，攻击来源距离：{}", sourceEntity.distanceTo(this));
                        rangedTeleport(directEntity);
                    }
                }
            }
        } else {
            if (directEntity != null) {
                if (directEntity.distanceToSqr(this) <= 25) {
                    log.info("没有伤害来源实体，但是有直接实体，距离：{}，触发近身传送", directEntity.distanceTo(this));
                    meleeTeleport(directEntity);
                } else {
                    log.info("没有伤害来源实体，但是有直接实体，距离：{}，触发范围传送", directEntity.distanceTo(this));
                    rangedTeleport(directEntity);
                }
            } else {
                randomTeleport(this.getX() + (random.nextDouble() - 0.5) * 16,
                        this.getY() + (random.nextDouble() - 0.5) * 8,
                        this.getZ() + (random.nextDouble() - 0.5) * 16, true);
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
        } else if (directEntity instanceof LivingEntity livingEntity) {
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
            SansAi.clearTargetTag(this, target);
        });
        super.remove(removalReason);
    }

    @Override
    public void onRemovedFromLevel() {
        this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            SansAi.clearTargetTag(this, target);
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
            Vec3 offset = RotUtils.getWorldVec3(left ? distance : -distance, 0, 0, 0, baseYaw);
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
        double followRangeBaseValue = this.getAttributeBaseValue(Attributes.FOLLOW_RANGE);
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
                double distance = followRangeBaseValue * SansAi.MID_RANGE_FACTOR - random.nextDouble() * 4.0;
                double targetX = entity.getX() + Mth.cos(angle * Mth.DEG_TO_RAD) * distance;
                double targetY = entity.getY() + random.nextDouble() * 16 - 8;
                double targetZ = entity.getZ() + Mth.sin(angle * Mth.DEG_TO_RAD) * distance;
                // 检查视线
                Vec3 from = new Vec3(targetX, targetY, targetZ);
                Vec3 to = entity.getEyePosition();
                if (level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                    if (randomTeleport(targetX, targetY, targetZ, true)) {
                        log.info("followRangeBaseValue：{},近战传送距离：{}", followRangeBaseValue, distance);
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
                this.getZ() + dir.z + (random.nextDouble() - 0.5) * 4,
                true
        );
    }


    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        LivingEntity target = this.getTargetFromBrain();
        if (target == player) {
            this.bossEvent.addPlayer(player);
            SansAi.applyTargetTag(this, player);
        } else {
            trackingPlayers.add(player);
        }
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        super.stopSeenByPlayer(player);
        SansAi.clearTargetTag(this, player);
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


    public Vec3 getPhantomStartPos() {
        return phantomStartPos;
    }

    public int getPhantomStartTick() {
        return phantomStartTick;
    }

    public boolean isPhantomActive() {
        return phantomActive;
    }


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
        if (this.level().isClientSide) {
            return (LivingEntity) this.level().getEntity(getTargetId());
        } else {
            return getTargetFromBrain();
        }
    }

    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public void setTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
    }


    public int getPhaseFactor() {
        return getPhaseID() >= SECOND_PHASE ? 1 : 0;
    }

    public int getStaminaFactor() {
        float maxStamina = getMaxStamina();
        float stamina = getStamina();
        return getPhaseID() <= FIRST_PHASE ? (stamina > maxStamina * 0.75 ? 0 : 1) : (stamina < maxStamina * 0.25 ? 1 : 0);
    }

    public int getDifficulty() {
        return this.level().getDifficulty().getId();
    }

    public double getFollowRange() {
        return this.getAttributeValue(Attributes.FOLLOW_RANGE);
    }

    public float getAttackDamage() {
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public void setControllerAimGB(GasterBlaster gb) {
        this.controllerAimGB = gb;
    }

    public GasterBlaster getControllerAimGB() {
        return controllerAimGB;
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
        if (id == 46) {
            phantomStartPos = position();
            phantomStartTick = tickCount;
            phantomActive = true;
        } else {
            super.handleEntityEvent(id);
        }
    }


    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        originPos = this.position();
    }


    /**
     * 瞄准目标随机骨头弹幕 - 持续射击目标
     *
     * @return 需要执行完攻击的动画CD
     */
    public int shootAimedBarrage(LivingEntity target) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int difficulty = this.level().getDifficulty().getId();
        int factor = getPhaseFactor();
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
                bone.setPos(this.position().add(RotUtils.getWorldVec3(
                        (this.random.nextDouble() - 0.5) * (6 + 3 * (factor + difficulty)),
                        this.random.nextDouble() * (2 + difficulty + factor) + this.getBbHeight() * 0.5f,
                        this.random.nextDouble() * (3 + difficulty + factor),
                        this.getXRot(), this.getYHeadRot()
                )));
            } while (this.level().noBlockCollision(bone, bone.getBoundingBox()) && !this.level().getEntities(bone, bone.getBoundingBox()).isEmpty() && ++attempts < 16);
            bone.aimShoot();
            RotUtils.lookAtEyeShoot(bone, target.getX(), heights[this.random.nextInt(heights.length)], target.getZ());
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
        int factor = getPhaseFactor();
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
                bone.setPos(this.position().add(RotUtils.getWorldVec3(offsetX, offsetY, 1f, this.getXRot(), this.getYHeadRot())));
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
        float speed = 1.0f + (getPhaseFactor() + difficulty) * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay;
        for (double x : offsetXs) {
            delay = 9;
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            bone.aimShoot();
            LevelUtils.addFreshProjectile(this.level(), bone, RotUtils.getWorldVec3(x, 1.5f, 0, this.getXRot(), this.getYHeadRot())
                    .add(this.getX(), this.getY(0.5f), this.getZ()), target);
            for (int l = 0; l < 2; l++) {
                delay += 5 - difficulty - getPhaseFactor();
                int count = (l + 1) * (6 + difficulty);
                float radius = (l + 1) * 0.5f;
                float interval = 360f / count;
                float angle = interval; //起始位置偏移，每层错位分布
                for (int i = 0; i < count; i++, angle += interval) {
                    bone = createFlyingBone(attackTypeUUID, speed, delay);
                    bone.aimShoot();
                    LevelUtils.addFreshProjectile(this.level(), bone, RotUtils.getWorldVec3(
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
        int factor = getPhaseFactor();
        float speed = 1.0f + (difficulty + factor) * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        float interval = 10f - difficulty - factor;
        // 圆心位置（实体位置）
        Vec3 center = new Vec3(this.getX(), this.getY(0.5f), this.getZ());
        int pitchLayer = 3 + factor * 2;
        int yawLayer = 1 + factor;
        for (int k = 0; k < yawLayer; k++) {
            int count = difficulty * 8 + 1 + k; // 或1，确保是奇数
            float centerOffset = (count - 1) * 0.5f;
            float pitchLayerAngle = -pitchLayer * 0.5f;
            for (int j = 0; j < pitchLayer; j++, pitchLayerAngle += 4) {
                float angle = -centerOffset * interval;
                for (int i = 0; i < count; i++, angle += interval) {
                    Vec3 worldOffsetPos = RotUtils.getWorldVec3(Mth.sin(angle * Mth.DEG_TO_RAD), 0, 0.8f * Mth.cos(angle * Mth.DEG_TO_RAD), this.getXRot() + pitchLayerAngle, this.getYHeadRot());
                    FlyingBone bone = new FlyingBone(EntityTypes.FLYING_BONE.get(), this.level(), this, 1f, speed, worldOffsetPos);
                    bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
                    LevelUtils.addFreshProjectileByVec3(this.level(), bone, center.add(worldOffsetPos), worldOffsetPos);
                }
            }
        }
    }

    protected FlyingBone createFlyingBone(String attackTypeUUID, float speed, int delay) {
        FlyingBone bone = new FlyingBone(EntityTypes.FLYING_BONE.get(), this.level(), this, getAttackDamage(), speed, delay);
        bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
        return bone;
    }

    public void shootRotationBone(LivingEntity target, float frequency, float initSpeed, float isRightHand) {
        RotationBone bone = new RotationBone(level(), this, getAttackDamage());
        double targetBbHeight = target.getBbHeight() * 0.5f;
        Vec3 spawnOffset = RotUtils.getWorldVec3(1.0 * isRightHand, targetBbHeight, 2.0F, getXRot(), getYHeadRot());
        bone.setPos(this.position().add(spawnOffset));
        // 计算径向向量（从骨骼指向目标）
        Vec3 targetPos = new Vec3(target.getX(), target.getY() + targetBbHeight, target.getZ());
        bone.startMotion(new SpringMotionModel(frequency), targetPos, initSpeed, -isRightHand);
        level().addFreshEntity(bone);
    }

    public void summonGroundBoneWallAroundTarget(LivingEntity target, ColorAttack colorAttack, float growScale) {
        summonGroundBoneWall(target, colorAttack, 2.0F, growScale, LocalDirection.FRONT, 5, 12.0);
        summonGroundBoneWall(target, colorAttack, 2.0F, growScale, LocalDirection.BACK, 5, 12.0);
        summonGroundBoneWall(target, colorAttack, 2.0F, growScale, LocalDirection.LEFT, 5, 12.0);
        summonGroundBoneWall(target, colorAttack, 2.0F, growScale, LocalDirection.RIGHT, 5, 12.0);
    }

    /**
     * 在指定方向召唤前进骨墙
     */
    public int summonGroundBoneWall(LivingEntity target, ColorAttack color, float scale, float growScale, LocalDirection direction, int delay, double distance) {
        Level level = this.level();
        int difficulty = level.getDifficulty().getId();
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = (7 + difficulty * 4) * (color == ColorAttack.AQUA ? 2 : 1);
        float speed = 0.25F;
        float spacing = 0.375f * scale;
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
            MovingGroundBone bone = new MovingGroundBone(level, this, scale, growScale, delay, speed, getAttackDamage(), color);
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
                MovingGroundBone bone = new MovingGroundBone(level, this, scale, growScale * (r > rows * 0.5f ? 4f : 1f), speed, lookVector, getAttackDamage(), ColorAttack.WHITE);
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
        Vec3 vel = calculateViewVector(0, yRot).scale(speed);
        for (int r = 0; r < rows; r++) {
            float t = r / (float) (rows - 1);
            float x = i * Mth.sin(t * Mth.PI * (6 + factor + factor) * 0.25f) * width * (2 + difficulty + factor + factor) * 0.1f - (i * width * 0.1f * (float) this.random.nextGaussian() * 0.033333f + 0.1f);
            float leftWidth = (width + x) - gap * 0.5f;
            float rightWidth = (width - x) - gap * 0.5f;
            for (int c = 0; c < cols; c++) {
                // 创建左侧骨头
                ObbBone leftBone = new ObbBone(level, this, 1.0f, leftWidth, getAttackDamage()).shoot(vel);
                leftBone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(id, (byte) 6));
                leftBone.setPos(RotUtils.yRot(new Vec3(-width + leftWidth * 0.5f, c * spacing, -r * spacing), yRot).add(this.position()));
                level.addFreshEntity(leftBone);
                // 创建右侧骨头
                ObbBone rightBone = new ObbBone(level, this, 1.0f, rightWidth, getAttackDamage()).shoot(vel);
                rightBone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(id, (byte) 6));
                rightBone.setPos(RotUtils.yRot(new Vec3(width - rightWidth * 0.5f, c * spacing, -r * spacing), yRot).add(this.position()));
                level.addFreshEntity(rightBone);
            }
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
        createGroundBone(attackTypeUUID, centerPos.x, centerPos.z, centerPos.y, centerPos.y, 1.0f, growScale, lifetime, delay, 0f, 0f).gravity(gravity);
        this.level().playSound(null, centerPos.x, getY(), centerPos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Cylinder((float) centerPos.x, (float) centerPos.y, (float) centerPos.z, layer * spacing, growScale, lifetime, WarningTip.RED, gravity));
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = spacing * (i + 1);
            float angle = interval; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                Vec3 pos = centerPos.add(data.localToWorld(r * Math.cos(angle * Math.PI / 180F), 0, r * Math.sin(angle * Math.PI / 180F)));
                pos = GravityUtils.findGround(this.level(), new Vec3(Math.round(pos.x * 1e6) / 1e6, Math.round(pos.y * 1e6) / 1e6, Math.round(pos.z * 1e6) / 1e6), gravity);
                this.level().addFreshEntity(createGroundBone(attackTypeUUID, pos.x, pos.z, pos.y, pos.y, 1.0f, growScale, lifetime, delay, angle, (float) (this.random.nextGaussian() * 10f)).gravity(gravity));
            }
        }
    }

    /**
     * 骨刺波动 - 以自身为中心发射，单击（一阶段）
     */
    public void summonGroundBoneSpineWaveAroundSelf(@NotNull LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        int phaseFactor = getPhaseFactor();
        int staminaFactor = getStaminaFactor();
        int count = 3 + 2 * staminaFactor;
        double distance = this.distanceTo(target);
        float maxAngle = 30F;
        float slope = 1.03F;
        float interval = maxAngle - slope * (float) distance;
        interval = Mth.clamp(interval, 5f, maxAngle);
        Vec3 centerPos = target.position().subtract(this.position()).scale(0.5f).add(this.position());
        this.level().playSound(null, centerPos.x, centerPos.y, centerPos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        Vec3 toTarget = target.position().subtract(this.position());
        float scale = 0.9f + 0.1f * difficulty + 0.2f * staminaFactor;
        float growScale = 1.5f + getStaminaFactor() * 0.5f;
        float colSpacing = 0.4f * scale;
        float rowSpacing = 0.6f * scale;
        int rows = (int) (Math.min(toTarget.length(), getFollowRange()) * 2 + (difficulty + phaseFactor) * 2);
        int cols = 3;
        float length = (rows - 1) * rowSpacing;
        float width = (cols - 1) * colSpacing;
        Vec3 dir = toTarget.normalize();
        float baseYaw = RotUtils.yRotD(dir);
        // 起始角度
        float angle = interval * (1 - count) * 0.5f;
        for (int i = 0; i < count; i++, angle += interval) {
            Vec3 currentDir = dir.yRot(angle * Mth.DEG_TO_RAD);
            float currentYaw = baseYaw + angle;
            // 发送警告提示
            PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Quad(
                    (float) getX(), (float) EntityUtils.findGroundY(this.level(), target.getX(), target.getZ(), target.getY(), target.getY()) + (i * 0.01f + 0.01f), (float) getZ(),
                    length, width + 0.1f, currentYaw, 20, WarningTip.RED));
            summonGroundBoneSpineWave(target, scale, growScale, rows, cols,
                    this.position(), currentDir, 14, 30 * (phaseFactor + 1), currentYaw, 0.4f * (phaseFactor + 1));
        }
    }

    /**
     * 公用方法：召唤定向骨刺波动（OBB版本，无炸开）
     */
    public void summonGroundBoneSpineWave(LivingEntity target, float scale, float growScale, int rows, int cols,
                                          Vec3 startPos, Vec3 direction, int delay, int lifetime,
                                          float baseYaw, float holdTimeScale) {
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        float colSpacing = 0.4f * scale;
        float rowSpacing = 0.6f * scale;
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x).normalize();
        int currentDelay = delay;
        // 生成 rows×cols 骨刺矩阵
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double xOffset = (col - (cols - 1) * 0.5) * colSpacing;
                double zOffset = row * rowSpacing;
                Vec3 bonePos = startPos.add(perpendicular.scale(xOffset)).add(direction.scale(zOffset));
                // 计算旋转角度
                float yaw = baseYaw;
                // 行的正弦波动
                float waveYaw = (float) Math.sin(row * 0.6) * 20f;
                // 列的偏移
                float colBias = (col - (cols - 1) * 0.5f) * 15f;
                // 随机扰动
                float randomYaw = (float) (this.random.nextGaussian() * 8);
                yaw += waveYaw + colBias + randomYaw;
                // 俯仰角
                float t = (float) row / rows;
                float pitch = 45f * (1 - t * 0.3f);
                pitch += (float) (this.random.nextGaussian() * 10);
                yaw = Mth.clamp(yaw, -180, 180);
                pitch = Mth.clamp(pitch, 15, 70);
                this.level().addFreshEntity( createGroundBone(
                        attackTypeUUID, bonePos.x, bonePos.z, minY, maxY,
                        scale, growScale, lifetime, currentDelay, yaw, pitch).holdTimeScale(holdTimeScale)
                );
            }
            // 每3行增加延迟
            currentDelay += (row % 3 == 0 ? 1 : 0);
        }
    }

    /**
     * 在自身位置召唤地面骨刺扩张（圆形）
     */
    public void summonGroundBoneSpineAtSelf() {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 pos = this.position();
        String attackTypeUUID = UUID.randomUUID().toString();
        int staminaFactor = getStaminaFactor();
        int layer = 6 + 2 * (staminaFactor + difficulty);
        int delay = 13 - staminaFactor - difficulty;
        float scale = 1.2f + getPhaseFactor() * 0.4F;
        float spacing = 0.7f * scale;
        float growScale = 2.0F + staminaFactor;
        this.level().playSound(null, pos.x, pos.y, pos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Cylinder((float) pos.x, (float) pos.y, (float) pos.z, layer * spacing, growScale, 20, WarningTip.RED));
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = spacing * (i + 1);
            float angle = interval; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                this.level().addFreshEntity( createGroundBone(
                        attackTypeUUID, pos.x + r * Mth.cos(angle * Mth.DEG_TO_RAD), pos.z + r * Mth.sin(angle * Mth.DEG_TO_RAD),
                        pos.y, pos.y, scale, growScale, 20, delay, angle, (float) (this.random.nextGaussian() * 10f)).holdTimeScale(0.6f)
                );
            }
            delay += 1;
        }
    }

    /**
     * 召唤巨大的地面骨刺波动
     */
    public void summonHugeGroundBoneSpineWave(LivingEntity target) {
        Vec3 position = this.position();
        int delay = 14;
        int difficulty = this.level().getDifficulty().getId();
        int staminaFactor = getStaminaFactor();
        Vec3 targetPos = target.position();
        Vec3 dirToTarget = targetPos.subtract(position);
        Vec3 centerPos = dirToTarget.scale(0.5f).add(position);  // 炸开中心点（中点）
        Vec3 dir = dirToTarget.normalize();
        this.level().playSound(null, centerPos.x, centerPos.y, centerPos.z,
                SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        // 计算列数
        int cols = 2 + difficulty + staminaFactor;
        float scale = 1.7f + 0.3f * staminaFactor;
        float colSpacing = 0.4f * scale;
        float rowSpacing = 0.6f * scale;
        // 动态计算行数：距离 / 行间距
        double distanceToTarget = dirToTarget.length();
        int rows = (int) Math.ceil(distanceToTarget / rowSpacing);

        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;

        // 方向角度
        float baseYaw = RotUtils.yRotD(dir);
        Vec3 perpendicular = new Vec3(-dir.z, 0, dir.x);
        float growScale = 2.0f + staminaFactor * 0.5f;
        int lifetime = 60;
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Quad(
                (float) getX(), (float) EntityUtils.findGroundY(this.level(), target.getX(), target.getZ(), target.getY(), target.getY()) + 0.01f, (float) getZ(),
                (float) distanceToTarget, colSpacing * cols, baseYaw, 20, WarningTip.RED));
        // ========== 第一部分：矩阵推进（不规则向前） ==========
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double xOffset = (col - (cols - 1) * 0.5) * colSpacing;
                double zOffset = row * rowSpacing;
                Vec3 bonePos = position.add(perpendicular.scale(xOffset)).add(dir.scale(zOffset));
                // 推进阶段的旋转：不规则向前
                float yaw = baseYaw;
                // 行的正弦波动，模拟波浪推进
                float waveYaw = (float) Math.sin(row * 0.6) * 25f;
                // 列的偏移，让两侧骨刺微微外偏
                float colBias = (col - (cols - 1) * 0.5f) * 20f;
                // 随机扰动
                float randomYaw = (float) (this.random.nextGaussian() * 10);
                yaw += waveYaw + colBias + randomYaw;
                // 俯仰角：前排更陡，后排更平缓
                float t = (float) row / rows;
                float pitch = 50f * (1 - t * 0.4f);
                pitch += (float) (this.random.nextGaussian() * 5);
                // 限制角度
                yaw = Mth.clamp(yaw, -180, 180);
                pitch = Mth.clamp(pitch, 20, 75);
                GroundBone groundBone = createGroundBone(attackTypeUUID, bonePos.x, bonePos.z, minY, maxY,
                        scale, growScale, lifetime, delay, yaw, pitch).holdTimeScale(0.8f);
                groundBone.updateOBB();
            }
            delay += (row % 3 == 0 ? 1 : 0);
        }
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Circle(
                (float) targetPos.x, (float) EntityUtils.findGroundY(this.level(), targetPos.x, targetPos.z, targetPos.y, targetPos.y) + 0.01f, (float) targetPos.z,
                scale * growScale * 2.3f, 20, WarningTip.RED));
        int blastSpikeCount = (int) (30 * growScale);
        for (int i = 0; i < blastSpikeCount; i++) {
            // 使用斐波那契球体算法生成均匀分布在球面上的方向
            double phi = Math.PI * (3 - Math.sqrt(5.0));  // 黄金角
            double y = 1 - (i / (double) (blastSpikeCount - 1)) * 2;  // y 从 1 到 -1
            // 将 y 的范围从 [-1, 1] 重新映射到 [0.3, 1.0]，避免指向地下
            // 0.3 对应约 17.5度向上，1.0 对应 90度垂直向上
            double remappedY = 0.3 + (y + 1) / 2 * 0.7;
            double radiusAtY = Math.sqrt(1 - remappedY * remappedY);
            double theta = i * phi * 2 * Math.PI;
            double dirX = Math.cos(theta) * radiusAtY;
            double dirZ = Math.sin(theta) * radiusAtY;
            // 径向向外方向（单位向量）
            Vec3 radialDir = new Vec3(dirX, remappedY, dirZ).normalize();
            // 偏航角（水平旋转）
            float yaw = (float) Math.toDegrees(Math.atan2(radialDir.z, radialDir.x));
            // 俯仰角（垂直倾斜）
            float pitch = (float) Math.toDegrees(Math.asin(radialDir.y));
            // 添加随机扰动
            yaw += (float) (this.random.nextGaussian() * 8);
            pitch += (float) (this.random.nextGaussian() * 8);
            // ===== 随机生成位置 =====
            double randomRadius = Math.sqrt(this.random.nextDouble()) * colSpacing;  // 半径0-1格
            double randomAngle = this.random.nextDouble() * 2 * Math.PI;
            double offsetX = Math.cos(randomAngle) * randomRadius;
            double offsetZ = Math.sin(randomAngle) * randomRadius;
            Vec3 bonePos = target.position().add(offsetX, 2, offsetZ);
            float randomScale = scale * (0.8f + this.random.nextFloat() * 0.7f);  // 0.8 - 1.5 倍
            float randomGrowScale = growScale * (1.5f + this.random.nextFloat() * 1.5f);  // 1.5 - 3.0 倍
            GroundBone groundBone = createGroundBone(attackTypeUUID, bonePos.x, bonePos.z, minY, maxY,
                    randomScale, randomGrowScale, lifetime, delay, yaw, pitch).holdTimeScale(0.8f);
            groundBone.updateOBB();
        }
    }



    public void summonHugeThreadGroundBoneSpineWave(LivingEntity target) {
        int phaseFactor = getPhaseFactor();
        int curveCount = 10 * (1 + phaseFactor);
        int pointsPerCurve = 40;

        int type = this.random.nextInt(7);
        Function<Float, Vec3> unitCurve;
        switch (type) {
            case 0 -> unitCurve = ParametricCurveUtils.circle();
            case 1 -> unitCurve = ParametricCurveUtils.spiral(0.3f);
            case 2 -> unitCurve = ParametricCurveUtils.star(5, 0.3f);
            case 3 -> unitCurve = ParametricCurveUtils.flower(5);
            case 4 -> unitCurve = ParametricCurveUtils.heart();
            case 5 -> unitCurve = ParametricCurveUtils.sineWave(2);
            default -> unitCurve = ParametricCurveUtils.square();
        }
        // 反向，使 t=0 在最外层，t=1 在原点
        Function<Float, Vec3> curve = ParametricCurveUtils.reverse(unitCurve);
        summonHugeThreadGroundBoneSpineWave(target, curve, curveCount, pointsPerCurve);
    }
    /**
     * 通用生成方法
     */
    public void summonHugeThreadGroundBoneSpineWave(LivingEntity target, Function<Float, Vec3> curve,
                                                    int curveCount, int pointsPerCurve) {
        String attackTypeUUID = UUID.randomUUID().toString();
        float sizeScale = 2.0f;
        float growScale = 3.0f;
        int lifetime = 30;
        Vec3 centerPos = target.position();
        Gravity data = target.getData(AttachmentTypes.GRAVITY);
        Direction gravity = data.getGravity();
        // 实际期望的最大半径：根据点数和间距计算
        float spacing = 0.175F * sizeScale;
        float maxRadius = pointsPerCurve * spacing;
        for (int s = 0; s < curveCount; s++) {
            float baseAngle = s * 360f / curveCount;
            int delay = 0;
            for (int p = 0; p < pointsPerCurve; p++) {
                float r = p * spacing;
                if (r < 0.05f) continue;
                Vec3 unitPos = curve.apply(r / maxRadius);         // 单位曲线上的点
                Vec3 localPos = unitPos.scale(maxRadius); // 缩放到实际半径
                double rad = Math.toRadians(baseAngle);
                double rotatedX = localPos.x * Math.cos(rad) - localPos.z * Math.sin(rad);
                double rotatedZ = localPos.x * Math.sin(rad) + localPos.z * Math.cos(rad);
                Vec3 pos = centerPos.add(data.localToWorld(rotatedX, localPos.y, rotatedZ));
                pos = GravityUtils.findGround(this.level(), pos, gravity);
                this.level().addFreshEntity(createGroundBone(attackTypeUUID, pos.x, pos.z, pos.y, pos.y,
                        sizeScale, growScale, lifetime, delay += 1, 0, 0)
                        .gravity(gravity).holdTimeScale(0.6f));
            }
        }

        // 中心点
        this.level().addFreshEntity(
        createGroundBone(attackTypeUUID, centerPos.x, centerPos.z, centerPos.y, centerPos.y,
                sizeScale, growScale, lifetime, pointsPerCurve, 0, 0)
                .gravity(gravity).holdTimeScale(0.6f));
    }

    /**
     * 创建地面骨
     *
     * @param scale     地面骨缩放大小
     * @param growScale 地面骨生长缩放大小
     */
    protected GroundBone createGroundBone(String attackUUID, double targetX, double targetZ, double minY, double maxY, float scale, float growScale, int lifetime, int delay, float yRot, float xRot) {
        Level level = this.level();
        double spawnY = EntityUtils.findGroundY(level, targetX, targetZ, minY, maxY);
        // 如果找到有效地面，生成骨刺
        GroundBone bone = new GroundBone(level, this, scale, growScale, getAttackDamage(), lifetime, delay);
        if (spawnY != level.getMinBuildHeight()) {
            bone.setPos(targetX, spawnY, targetZ);
            bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackUUID, (byte) (scale*2f+growScale*1.5f)));
            bone.setYRot(yRot);
            bone.setXRot(xRot);
        }
        return bone;
    }


    /**
     * 在自身周围随机位置召唤GB
     */
    public void summonGBAroundSelf(LivingEntity target, int count, float size) {
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            GasterBlaster gb = createGasterBlaster(size);
            do {
                gb.setPos(this.getEyePosition().subtract(target.getEyePosition()).scale(0.5).add(this.getEyePosition()).add(RotUtils.getWorldVec3(
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
        int count = 2 + this.random.nextInt(1 + difficulty + getPhaseFactor());
        this.summonGBAroundTarget(target, count, this.random.nextInt(count) * 180f / count);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB，固定360度范围，根据数量自动计算角度步长和大小
     */
    public void summonGBAroundTarget(LivingEntity target, int count, float offsetAngle) {
        int difficulty = this.level().getDifficulty().getId();
        summonGBAroundTarget(target, count, target.getBbWidth() * 16, offsetAngle, 360f / count, 1.0f + (2 + difficulty - count) * 0.5f);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB，在自身前方对称召唤GB，用于自定义的序列攻击，
     */
    public void summonGBFront(LivingEntity target, int count, float angleStep, int charge) {
        int difficulty = this.level().getDifficulty().getId();
        summonGBAroundTarget(target, count, this.distanceTo(target) * 0.9f, (1 - count) * angleStep * 0.5f, angleStep, 1.0f + (1 + difficulty + getPhaseFactor() - count) * 0.25f);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB
     *
     * @param target      目标实体
     * @param count       GB数量
     * @param offsetAngle 初始偏移角度（度）
     * @param angleStep   角度步长（度）
     */
    public void summonGBAroundTarget(LivingEntity target, int count, float radius, float offsetAngle, float angleStep, float size) {
        Vec3 targetPos = new Vec3(target.getX(), target.getY(0.5f), target.getZ());
        float currentAngle = offsetAngle; // 从指定角度开始
        for (int i = 0; i < count; i++, currentAngle += angleStep) {
            GasterBlaster gb = createGasterBlaster(size);
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = -Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            gb.setPos(RotUtils.getWorldVec3(xOffset, 0, zOffset, this.getXRot(), this.getYHeadRot()).add(targetPos));
            gb.aim(target);
            this.level().addFreshEntity(gb);
        }
    }

    public GasterBlaster controlGBAim(LivingEntity target) {
        int factor = getPhaseFactor();
        int difficulty = getDifficulty();
        float size = 0.5f + difficulty * 0.3334f + factor * 0.5f;
        GasterBlaster gb = new GasterBlaster(level(), this, getAttackDamage(), size, (34 - factor * 17), (int) (100 * size), 100).follow(new Vec3(0, this.getBbHeight() * 0.5f, 1))
                .aimSmoothSpeed(0.1f + difficulty * 0.02f + factor * 0.04f);
        gb.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(UUID.randomUUID().toString(), (byte) 10));
        gb.aim(target);
        this.level().addFreshEntity(gb);
        return gb;
    }

    public GasterBlaster createGasterBlaster(float size) {
        GasterBlaster gb = new GasterBlaster(this.level(), this, getAttackDamage(), size);
        gb.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(UUID.randomUUID().toString(), (byte) 10));
        return gb;
    }

    public void gravitySlam(LivingEntity target, LocalDirection direction, float acceleration) {
        Gravity gravityData = Gravity.applyRelativeGravity(this, target, direction);
        target.addDeltaMovement(new Vec3(0, -acceleration, 0));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new GravityPacket(target.getId(), gravityData.getGravity(), acceleration));
        applyGravityControlTag(target, true);
    }

    public void applyGravityControlTag(LivingEntity target, boolean exist) {
        target.setData(AttachmentTypes.GRAVITY_CONTROL_TAG, exist);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new GravityControlTagPacket(target.getId(), exist));
    }

    public void timeJumpTeleport(LivingEntity target, int duration) {
        PacketDistributor.sendToPlayersTrackingEntity(this, new TimeJumpTeleportPacket(target.getId(), duration));
        this.teleportTo(originPos.x, originPos.y, originPos.z);
        target.teleportTo(originPos.x, originPos.y, originPos.z + getFollowRange() * 0.5f);
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvnets.SANS_TELEPORT_TIME_JUMP.get(), SoundSource.HOSTILE);
    }

    public void controlSoulMode(LivingEntity target, byte soulState) {
        target.setData(AttachmentTypes.SOUL_MODE, soulState);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new SoulModePacket(target.getId(), soulState));
    }

    public void applyKarma(LivingEntity target, boolean exist) {
        target.setData(AttachmentTypes.KARMA_TAG, exist);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new KaramTagPacket(target.getId(), exist));
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
    public static final RawAnimation ANIM_BONE_ROTATION = RawAnimation.begin().thenPlay("attack.bone.rotation");
    public static final RawAnimation ANIM_POUND_GROUND = RawAnimation.begin().thenPlayAndHold("attack.pound.ground");
    public static final RawAnimation ANIM_STAMP_GROUND = RawAnimation.begin().thenPlayAndHold("attack.stamp.ground");
    public static final RawAnimation ANIM_STAMP_POUND_GROUND = RawAnimation.begin().thenPlayAndHold("attack.stamp.pound.ground");
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
            if (animId == -1) {
                return PlayState.STOP;
            }
            if (state.isCurrentAnimation(ANIM_CAST_LEFT) || state.isCurrentAnimation(ANIM_CAST_CIRCLE_LEFT)) {
                if (tickCount != lastTickCount) {
                    Map<String, BoneAnimationQueue> boneAnimationQueues = state.getController().getBoneAnimationQueues();
                    BoneAnimationQueue leftHand = boneAnimationQueues.get("left_hand");
                    Vector3d worldPosition = leftHand.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                    lastTickCount = tickCount;
                }
            }
            if (state.isCurrentAnimation(ANIM_CAST) || state.isCurrentAnimation(ANIM_CAST_CIRCLE) || state.isCurrentAnimation(ANIM_POUND_GROUND) || state.isCurrentAnimation(ANIM_STAMP_POUND_GROUND)) {
                if (tickCount != lastTickCount) {
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
            if (state.isCurrentAnimation(ANIM_STAMP_GROUND) || state.isCurrentAnimation(ANIM_STAMP_POUND_GROUND)) {
                if (tickCount != lastTickCount) {
                    Map<String, BoneAnimationQueue> boneAnimationQueues = state.getController().getBoneAnimationQueues();
                    BoneAnimationQueue leftFoot = boneAnimationQueues.get("left_foot");
                    Vector3d worldPosition = leftFoot.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                    lastTickCount = tickCount;
                }
            }

            if (!controller.hasAnimationFinished() && animId == -2) {
                return PlayState.CONTINUE;
            }
            byte phaseID = getPhaseID();
            boolean isFirstPhase = phaseID == FIRST_PHASE;
            controller.setAnimationSpeed(1.0f);
            switch (animId) {
                case 0, 1, 2, 3, 4, 5 -> controller.setAnimation(THROW_ANIMATIONS[animId]);
                case 6 -> controller.setAnimation(isFirstPhase ? ANIM_CAST_LEFT : ANIM_CAST);
                case 7 -> controller.setAnimation(isFirstPhase ? ANIM_CAST_CIRCLE_LEFT : ANIM_CAST_CIRCLE);
                case 8 -> controller.setAnimation(isFirstPhase ? ANIM_BONE_PROJECTILE_LEFT : ANIM_BONE_PROJECTILE);
                case 9 -> controller.setAnimation(isFirstPhase ? ANIM_BONE_SWEEP_LEFT : ANIM_BONE_SWEEP);
                case 10 -> {
                    controller.setAnimation(ANIM_GB_FOLLOW);
                    controller.setAnimationSpeed(0.5 * (1 + getPhaseFactor()));
                }
                case 11 -> controller.setAnimation(ANIM_BONE_ROTATION);
                case 12 -> controller.setAnimation(ANIM_STAMP_GROUND);
                case 13 -> controller.setAnimation(ANIM_POUND_GROUND);
                case 14 -> controller.setAnimation(ANIM_STAMP_POUND_GROUND);
            }
            animId = -2;
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

