package com.fanxing.fx_undertale.entity.boss.sans;

import com.fanxing.fx_undertale.client.render.component.RadialPlaneTrailStrip;
import com.fanxing.fx_undertale.client.render.effect.WarningTip;
import com.fanxing.fx_undertale.common.phys.CollisionDeflection;
import com.fanxing.fx_undertale.common.phys.LocalDirection;
import com.fanxing.fx_undertale.common.phys.motion.CircularMotionModel;
import com.fanxing.fx_undertale.common.phys.motion.PhysicsMotionModel;
import com.fanxing.fx_undertale.common.phys.motion.RoseSpiralMotionModel;
import com.fanxing.fx_undertale.common.phys.motion.SpringMotionModel;
import com.fanxing.fx_undertale.entity.AbstractUTMonster;
import com.fanxing.fx_undertale.entity.ai.control.PatchedMoveControl;
import com.fanxing.fx_undertale.entity.attachment.KaramJudge;
import com.fanxing.fx_undertale.entity.block.PlatformBlockEntity;
import com.fanxing.fx_undertale.entity.capability.Animatable;
import com.fanxing.fx_undertale.entity.component.EllipsoidProjectileShield;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.entity.projectile.FlyingBone;
import com.fanxing.fx_undertale.entity.summon.*;
import com.fanxing.fx_undertale.net.packet.*;
import com.fanxing.fx_undertale.registry.*;
import com.fanxing.fx_undertale.utils.*;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
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

public class Sans extends AbstractUTMonster implements GeoEntity, Animatable, IEntityWithComplexSpawn {
    private static final Logger log = LogManager.getLogger(Sans.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public int testAttackId;

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
            MemoryModuleTypes.COOLDOWN_3.get(),
            MemoryModuleTypes.COOLDOWN_4.get(),
            MemoryModuleTypes.MOVE_LOCKING.get(),
            MemoryModuleTypes.ACTIVE_ATTACK_NODES.get()
    );

    private float maxStamina;       // 最大体力/耐力
    private int animId = -1;
    public Vec3 originPos;          // 存储生成的原点
    float structYaw;
    private boolean mercyTriggered = false;  // 仁慈触发标记

    // 添加BOSS条相关字段
    private final ServerBossEvent bossEvent;
    private final Set<ServerPlayer> trackingPlayers = new HashSet<>();

    private GasterBlaster controllerAimGB = null;
    private final EllipsoidProjectileShield shield ;

    public Sans(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        maxStamina = level.getDifficulty().getId() * 150;
        setStamina(maxStamina);
        this.moveControl = new PatchedMoveControl(this);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setPlayBossMusic(true).setDarkenScreen(false);
        this.shield = new EllipsoidProjectileShield(this, getBbWidth()+0.8F, getBbHeight()+0.5F, getBbWidth()+0.8F);
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
        if (level().isClientSide()) {
            if(phantomActive){
                int elapsed = tickCount - phantomStartTick;
                if (elapsed >= PHANTOM_DURATION) {
                    phantomActive = false;
                    phantomStartPos = null;
                }
            }
            if (this.getIsEyeBlink() && this.tickCount % 2 == 0) {
                Vec3 vec3 = RotUtils.rotateYX(this.getAttachments().get(EntityAttachment.WARDEN_CHEST, 0, 0), this.getYHeadRot(), this.getXRot()).add(this.position());
                long time = this.tickCount;
                float cycle = (Mth.sin(time * 1.0f) + 1.0f) / 2.0f; // 0.1f 控制变化速度
                int colorB = 0xC0F6FD29;// 原本是 0xFFF6FD29
                int[] colorA = Sans.ENERGY_AQUA[1];
                this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.lerp(cycle,FastColor.ARGB32.color(colorA[3],colorA[0],colorA[1],colorA[2]), colorB)), vec3.x, vec3.y, vec3.z, 0, 0, 0);
            }
        }else{
            EntityHitResult hitResult = shield.tick((t)-> t.getOwner() != this);
            float range = getPhaseID() == SPECIAL_ATTACK?1.0f:(getPhaseID()==SECOND_PHASE?0.6F:0.9f);
            if (hitResult != null && random.nextFloat() <  range) {
                Vec3 pos = hitResult.getLocation();
                Projectile entity = (Projectile) hitResult.getEntity();
                entity.setPos(pos);
                Vec3 radial = pos.subtract(this.getBoundingBox().getCenter()).normalize();
                entity.discard();
                DisplayBone displayBone = new DisplayBone(this.level(), 10, 1.0f);
                this.level().playSound(null,pos.x,pos.y,pos.z,SoundEvnets.BLOCK,SoundSource.HOSTILE,1.0f,1.0f);
                displayBone.setPos(hitResult.getLocation());
                RotUtils.lookVec(displayBone,radial);
                this.level().addFreshEntity(displayBone);
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
        LivingEntity target = getTargetFromBrain();
        if (target != null) {
            for (ServerPlayer p : trackingPlayers) {
                bossEvent.addPlayer(p);
            }
        }
        SansAi.updateActivity(this);
        super.customServerAiStep();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float power) {
        if (this.level().isClientSide()) {
            return super.hurt(source, power);
        }
        Entity sourceEntity = source.getEntity();
        Entity directEntity = source.getDirectEntity();
        this.getBrain().setMemory(MemoryModuleType.HURT_BY, source);
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
        if (isInvulnerableTo(source) || source.is(Tags.DamageTypes.IS_ENVIRONMENT) || this.isDeadOrDying()) {
            return false;
        }
        float stamina = getStamina();
        byte phaseID = getPhaseID();
        boolean flag;
        // 直接免疫的
        if (phaseID == MERCY_PHASE) {
            // 触发仁慈可以被攻击，但是不会立马进入二阶段
            if (isMercyTriggered()) flag = true;
            else {
                this.entityData.set(PHASE_ID, SECOND_PHASE);
                flag = false;
            }
        } else if (source.is(Tags.DamageTypes.IS_TECHNICAL) || getPhaseID() == END_PHASE) {
            this.setStamina(power);
            return super.hurt(source, power);
        } else if (phaseID == SPECIAL_ATTACK) {
            return false;
        } else {
            power *= 0.5f;
            if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                power *= 0.9f;
            }
            if (source.is(Tags.DamageTypes.IS_MAGIC)) {
                power *= 0.95f;
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
                    if (getTarget() instanceof ServerPlayer player) {
                        this.entityData.set(PHASE_ID, MERCY_PHASE);
                        SansDialogue.mercy(player, this);
                    } else {
                        this.entityData.set(PHASE_ID, SECOND_PHASE);
                        setIsEyeBlink(true);
                    }
                }
            } else if (phaseID == SECOND_PHASE) {
                stamina = Math.max(0, stamina - power);
                this.setStamina(stamina);
                this.bossEvent.setProgress(stamina / maxStamina);
                if (stamina == 0) {
                    this.entityData.set(PHASE_ID, SPECIAL_ATTACK);
                }
            }
        }

        if (sourceEntity != null) {
            if (source.isDirect()) {
                log.debug("攻击伤害来源实体和直接实体相等，触发近战传送，距离：{}", sourceEntity.distanceTo(this));
                meleeTeleport(sourceEntity);
            } else {
                if (sourceEntity.distanceToSqr(this) <= 25) {
                    log.debug("攻击伤害来源实体和直接实体不相等，触发近战传送，攻击来源距离：{}", sourceEntity.distanceTo(this));
                    meleeTeleport(sourceEntity);
                } else if (directEntity != null) {
                    if (directEntity instanceof LivingEntity) {
                        meleeTeleport(directEntity);
                        log.debug("攻击伤害来源实体和直接实体不相等，且与攻击来源实体距离超出近战范围，且直接实体是活体，触发近战传送，攻击来源距离：{}", sourceEntity.distanceTo(this));
                    } else {
                        log.debug("攻击伤害来源实体和直接实体不相等，且与攻击来源实体距离超出近战范围，且直接实体不是活体，触发范围传送，攻击来源距离：{}", sourceEntity.distanceTo(this));
                        rangedTeleport(directEntity);
                    }
                }
            }
        } else {
            if (directEntity != null) {
                if (directEntity.distanceToSqr(this) <= 25) {
                    log.debug("没有伤害来源实体，但是有直接实体，距离：{}，触发近身传送", directEntity.distanceTo(this));
                    meleeTeleport(directEntity);
                } else {
                    log.debug("没有伤害来源实体，但是有直接实体，距离：{}，触发范围传送", directEntity.distanceTo(this));
                    rangedTeleport(directEntity);
                }
            } else {
                randomTeleport(this.getX() + (random.nextDouble() - 0.5) * 16,
                        this.getY() + (random.nextDouble() - 0.5) * 8,
                        this.getZ() + (random.nextDouble() - 0.5) * 16, true);
            }
        }
        return true;
    }

    @Override
    public void die(@NotNull DamageSource p_21014_) {
        super.die(p_21014_);
    }

    @Override
    public void remove(@NotNull RemovalReason removalReason) {
        this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> SansAi.clearTargetTag(this, target));
        super.remove(removalReason);
    }

    @Override
    public void onRemovedFromLevel() {
        brain.getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> SansAi.clearTargetTag(this, target));
        super.onRemovedFromLevel();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (originPos == null) {
            originPos = this.position();
        }
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
            Vec3 offset = RotUtils.rotateYX(left ? distance : -distance, 0, 0, baseYaw, 0);
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

    public boolean isMercyTriggered() {
        return mercyTriggered;
    }

    public void setMercyTriggered(boolean triggered) {
        this.mercyTriggered = triggered;
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
        builder.define(IS_EYE_BLINK, false);
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
        if (originPos != null) tag.put("originPos", this.newDoubleList(originPos.x, originPos.y, originPos.z));
        tag.putFloat("structYaw",this.structYaw);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("maxStamina")) this.maxStamina = tag.getFloat("maxStamina");
        if(tag.contains("stamina")) setStamina(tag.getFloat("stamina"));
        if(tag.contains("phaseId")) this.entityData.set(PHASE_ID, tag.getByte("phaseId"));
        if(tag.contains("originPos")) {
            ListTag list = tag.getList("originPos", 6);
            this.originPos = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }
        if(tag.contains("structYaw")) structYaw  = tag.getFloat("structYaw");
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


    /**
     * 瞄准目标随机骨头弹幕 - 持续射击目标
     *
     * @return 需要执行完攻击的动画CD
     */
    public int shootAimedBarrage(LivingEntity target) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int difficulty = this.level().getDifficulty().getId();
        int staminaFactor = getStaminaFactor();
        int phaseFactor = getPhaseFactor();
        float scale = 1.0f + staminaFactor * 0.1f + phaseFactor * 0.2f;
        float growScale = 1.1f + staminaFactor * 0.1f + phaseFactor * 0.2f;
        float speed = 1.0f + phaseFactor * 0.3f + difficulty * 0.5f;
        int delay = 15 - phaseFactor * 5;
        int count = 10 * (phaseFactor + difficulty);
        // 三层高度：腿、身体、眼睛
        double[] heights = {target.getEyeY(), target.getY(0.5), target.getY(0.15),};
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            FlyingBone bone = createFlyingBone(attackTypeUUID, scale, growScale);
            do {
                bone.setPos(this.position().add(RotUtils.rotateYX(
                        (this.random.nextDouble() - 0.5) * (6 + 3 * (phaseFactor + difficulty)),
                        this.random.nextDouble() * (2 + difficulty + phaseFactor) + this.getBbHeight() * 0.5f,
                        this.random.nextDouble() * (3 + difficulty + phaseFactor),
                        this.getYHeadRot(), this.getXRot()
                )));
            } while (this.level().noBlockCollision(bone, bone.getBoundingBox()) && !this.level().getEntities(bone, bone.getBoundingBox()).isEmpty() && ++attempts < 16);
            bone.aimShoot(delay, speed);
            RotUtils.lookAtEyeShoot(bone, target.getX(), heights[this.random.nextInt(heights.length)], target.getZ());
            level().addFreshEntity(bone);
            delay += 6 - difficulty - phaseFactor;
        }
        return delay;
    }

    /**
     * 射向前方随机骨头弹幕 - 以目标碰撞高度为随机范围高度，向前方范围随机射击
     */
    public int shootForwardBarrage(LivingEntity target) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int difficulty = this.level().getDifficulty().getId();
        int staminaFactor = getStaminaFactor();
        int phaseFactor = getPhaseFactor();
        float scale = 1.0f + staminaFactor * 0.1f + phaseFactor * 0.2f;
        float growScale = 1.1f + staminaFactor * 0.1f + phaseFactor * 0.2f;
        float speed = 0.8f + (phaseFactor + difficulty) * 0.1f;
        int delay = 15 - 5 * phaseFactor;
        int count = 10 * (phaseFactor + difficulty);
        float targetBbHeight = target.getBbHeight();
        // Sans的视线方向（固定射击方向）
        Vec3 dir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            FlyingBone bone = createFlyingBone(attackTypeUUID, scale, growScale);
            do {
                // 计算相对于视线方向的偏移（在局部坐标系）
                float offsetX = (float) this.random.nextGaussian() * 0.333333f * (1.0f + (phaseFactor + difficulty) * 0.5f);  // 左右
                float offsetY = Mth.clamp(((float) this.random.nextGaussian() * 0.1666667f + 0.5f) * targetBbHeight, 0, targetBbHeight);     // 上下
                bone.setPos(this.position().add(RotUtils.rotateYX(offsetX, offsetY, 1f, this.getYHeadRot(), this.getXRot())));
                bone.followShoot(delay, speed, new Vec3(offsetX, offsetY, 1f));
            } while (this.level().noBlockCollision(bone, bone.getBoundingBox()) && !this.level().getEntities(bone, bone.getBoundingBox()).isEmpty() && ++attempts < 16);
            RotUtils.lookVecShoot(bone, dir);
            this.level().addFreshEntity(bone);
            delay += 6 - difficulty - phaseFactor;
        }
        return delay;
    }


    /**
     * 骨环齐射 - 向目标射击
     */
    public void shootBoneRingVolley(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        double[] offsetXs = getPhaseID() == FIRST_PHASE ? new double[]{1.0} : new double[]{1.0, -1.0};
        String attackTypeUUID = UUID.randomUUID().toString();
        int staminaFactor = getStaminaFactor();
        int phaseFactor = getPhaseFactor();
        float scale = 1.0f + staminaFactor * 0.2f + phaseFactor * 0.3f;
        float growScale = 1.1f + staminaFactor * 0.3f + phaseFactor * 0.5f;
        float speed = 1.0f + (phaseFactor + difficulty) * 0.2f;
        int delay;
        for (double x : offsetXs) {
            delay = 9;
            FlyingBone bone = createFlyingBone(attackTypeUUID, scale, growScale);
            bone.aimShoot(delay, speed);
            LevelUtils.addFreshProjectile(this.level(), bone, RotUtils.rotateYX(x, 1.5f, 0, this.getYHeadRot(), this.getXRot()).add(this.getX(), this.getY(0.5f), this.getZ()), target);
            for (int l = 0; l < 2 + 2 * phaseFactor; l++) {
                delay += 5 - difficulty - phaseFactor;
                int count = (l + 1) * (6 + difficulty);
                float radius = (l + 1) * scale * 0.5f;
                float interval = 360f / count;
                float angle = interval; //起始位置偏移，每层错位分布
                for (int i = 0; i < count; i++, angle += interval) {
                    bone = createFlyingBone(attackTypeUUID, scale, growScale);
                    bone.aimShoot(delay, speed);
                    LevelUtils.addFreshProjectile(this.level(), bone, RotUtils.rotateYX(
                            x + radius * Mth.cos(angle * Mth.DEG_TO_RAD),
                            1.5f + radius * Mth.sin(angle * Mth.DEG_TO_RAD),
                            0,
                            this.getYHeadRot(), this.getXRot()
                    ).add(this.getX(), this.getY(0.5f), this.getZ()), target);
                }
            }
        }
    }

    /**
     * 弧形横扫齐射 - 骨头在圆弧上朝外径向发射
     */
    public void shootArcSweepVolley(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        int staminaFactor = getStaminaFactor();
        int phaseFactor = getPhaseFactor();
        float scale = 1.0f + staminaFactor * 0.2f + phaseFactor * 0.3f;
        float growScale = 1.1f + staminaFactor * 0.3f + phaseFactor * 0.5f;
        float speed = 1.0f + (difficulty + phaseFactor) * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        float interval = 10f - difficulty - phaseFactor;
        // 圆心位置（实体位置）
        Vec3 center = new Vec3(this.getX(), this.getY(0.5f), this.getZ());
        int pitchLayer = (int) (3 + scale * 2);
        int yawLayer = (int) (1 + scale);
        Vec3 dir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
        for (int k = 0; k < yawLayer; k++) {
            int count = difficulty * 8 + 1 + k; // 或1，确保是奇数
            float centerOffset = (count - 1) * 0.5f;
            float pitchLayerAngle = -pitchLayer * 0.5f * 4;
            for (int j = 0; j < pitchLayer; j++, pitchLayerAngle += 4) {
                float angle = -centerOffset * interval;
                for (int i = 0; i < count; i++, angle += interval) {
                    Vec3 worldOffsetPos = RotUtils.rotateYX(Mth.sin(angle * Mth.DEG_TO_RAD), 0, 0.8f * Mth.cos(angle * Mth.DEG_TO_RAD), RotUtils.yRotD(dir), RotUtils.xRotD(dir) + pitchLayerAngle);
                    FlyingBone bone = new FlyingBone(EntityTypes.FLYING_BONE.get(), this.level(), this, getAttackDamage(), scale, growScale);
                    bone.shoot(worldOffsetPos.x, worldOffsetPos.y, worldOffsetPos.z, speed, 0);
                    bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
                    LevelUtils.addFreshProjectileByVec3(this.level(), bone, center.add(worldOffsetPos), worldOffsetPos);
                }
            }
        }
    }

    protected FlyingBone createFlyingBone(String attackTypeUUID, float scale, float growScale) {
        FlyingBone bone = new FlyingBone(EntityTypes.FLYING_BONE.get(), this.level(), this, getAttackDamage(), scale, growScale);
        bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
        return bone;
    }

    /**
     * 带相对前方计算的正弦波缺口骨头矩阵
     *
     * @param randomIntervalSize 随机区间大小（0~0.5 为宜），决定所有随机参数的变化幅度
     */
    public int summonLateralBoneMatrix(LivingEntity target, float randomIntervalSize) {
        Level level = this.level();
        int difficulty = level.getDifficulty().getId();
        String attackUUID = UUID.randomUUID().toString();
        int rows = 7 * (3 + difficulty);
        int cols = (int) (target.getBbHeight() * 2);
        Vec3 toTarget = target.position().subtract(this.position());

        // 1. 生成随机倍率（范围 [1-intervalSize, 1+intervalSize]）
        float randomFactor = 1.0f + (this.random.nextFloat() * 2 - 1) * randomIntervalSize;
        randomFactor = Math.max(0.5f, Math.min(1.5f, randomFactor)); // 限制范围
        float scale = 1.25f + 0.25f * randomFactor;                     // 矩阵整体缩放（固定，也可调整）
        float yRot = RotUtils.yRotD(toTarget);
        float baseWidth = target.getBbWidth() * 1.66666667f;
        float width = 10f * baseWidth * scale;           // 总半宽

        float spacing = 0.5F * scale;
        int direction = this.random.nextBoolean() ? 1 : -1;
        Vec3 vel = calculateViewVector(0, yRot);
        double groundY = EntityUtils.findGroundY(this.level(), this.position());
        // 2. 随机化频率和振幅基础值
        float frequency = 6.0f * randomFactor;               // 基础频率6
        float amplitudeBase = 2.0f * randomFactor;           // 基础振幅系数2
        // 3. 随机化噪声相关参数
        float noiseAmplitude = 0.1f * randomFactor;
        float noiseGaussScale = 0.033333f * randomFactor;
        float constOffset = 0.1f * randomFactor;
        float amplitudeScale = 0.1f;   // 振幅缩放固定
        // 4. 根据最终频率和振幅动态调整速度和缺口（线性关系）
        float freqRatio = frequency / 6.0f;      // 频率相对基准（基准=6）
        float ampRatio = amplitudeBase / 2.0f;   // 振幅相对基准（基准=2）
        // 缺口与频率成正比
        float gap = 2.5f * baseWidth * scale * freqRatio;
        // 速度与振幅成反比（振幅越大速度越慢）
        float sensitivity = 0.7f;   // 可调节 0.1~0.5 之间
        float speed = (0.45f - scale * 0.02f) / (1 + (ampRatio - 1) * sensitivity);
        // 可选：限制速度和缺口的合理范围
        speed = Math.max(0.25f, Math.min(1.0f, speed));
        gap = Math.max(1.0f, Math.min(6.0f, gap));

        // 计算矩阵在 z 轴方向的总长度（近似为 rows * spacing）+距离目标的距离 除以速度，并返回所需 tick 数
        int reactionTicks = 30;
        float frontDist = reactionTicks * speed;
        float rDist = rows * spacing;
        int ticks = reactionTicks + (int) (rDist / speed) + (int) (1 / speed);
        double length = toTarget.length();
        for (int r = 0; r < rows; r++) {
            float t = r / (float) (rows - 1);
            // 使用随机化后的参数计算 x
            float x = direction * Mth.sin(t * Mth.PI * frequency * 0.25f)
                    * width * (amplitudeBase + difficulty) * amplitudeScale
                    - (direction * width * noiseAmplitude * (float) this.random.nextGaussian() * noiseGaussScale + constOffset);

            float minX = gap * 0.5f - width;
            float maxX = width - gap * 0.5f;
            x = Math.max(minX, Math.min(maxX, x));
            float leftWidth = (width + x) - gap * 0.5f;
            float rightWidth = (width - x) - gap * 0.5f;
            // 钳位 x 到有效范围，确保 leftWidth 和 rightWidth 非负

            for (int c = 0; c < cols; c++) {
                // 左侧骨头
                RotationBone leftBone = createLaterBone(attackUUID, scale, leftWidth / scale, ticks).holdTimeScale(0.9f).initOrientation(-yRot, 0, -90f); //KEY 这里yRot必须取反和MC对齐，不然Z轴会错位，下方同理
                leftBone.setPos(RotUtils.rotateY(new Vec3(-width, c * spacing + leftBone.getBbWidth() * 0.5f, length - frontDist - r * spacing), yRot).add(this.getX(), groundY, this.getZ()));
                leftBone.shoot(vel.scale(speed));
                leftBone.updateOBB();
                level.addFreshEntity(leftBone);

                // 右侧骨头
                RotationBone rightBone = createLaterBone(attackUUID, scale, rightWidth / scale, ticks).holdTimeScale(0.9f).initOrientation(-yRot, 0, 90f);
                rightBone.setPos(RotUtils.rotateY(new Vec3(width, c * spacing + rightBone.getBbWidth() * 0.5f, length - frontDist - r * spacing), yRot).add(this.getX(), groundY, this.getZ()));
                rightBone.shoot(vel.scale(speed));
                rightBone.updateOBB();
                level.addFreshEntity(rightBone);
            }
        }
        return ticks;
    }

    public void shootRotationBone(LivingEntity target, float isRightHand, float angularVelocity) {
        Direction gravity = target.getData(AttachmentTypes.GRAVITY);
        double targetHalfBbHeight = target.getBbHeight() * 0.5f;
        float bbWidth = this.getBbWidth();
        float scale = 1.5f + getPhaseFactor() * 1f;
        float growScale = 2f + getStaminaFactor() * 2f;
        Direction targetG = target.getData(AttachmentTypes.GRAVITY);
        Vec3 targetGroundPos = GravityUtils.findGround(level(), target.position(), targetG);
        PhysicsMotionModel motionModel = new RoseSpiralMotionModel(0.1f, 0.1f);
        RotationBone bone = createRotationBone(UUID.randomUUID().toString(), scale, growScale, 300 + 100 * getPhaseID()).angularVelocity(new Vector3f(0, angularVelocity * Mth.DEG_TO_RAD, 0)).holdTimeScale(0.9F).motion(motionModel,
                targetGroundPos.add(GravityUtils.localToWorld(targetG, RotUtils.rotateYXZ(scale * growScale * isRightHand, (float) targetHalfBbHeight, 0, getYHeadRot(), 0, 0)))
        );
        bone.setPos(this.position().add(RotUtils.rotateYXZ(bbWidth * isRightHand, (float) targetHalfBbHeight, 2 * bbWidth, getYHeadRot(), 0, 0)));
        bone.initOrientation(0, 90, 0);
        bone.gravity(gravity);
        bone.updateOBB();
        level().addFreshEntity(bone);


        RotationBone bone1 = createRotationBone(UUID.randomUUID().toString(), scale, growScale, 300 + 100 * getPhaseID()).angularVelocity(new Vector3f(0, angularVelocity * Mth.DEG_TO_RAD, 0)).holdTimeScale(0.9F).motion(motionModel,
                targetGroundPos.add(GravityUtils.localToWorld(targetG, RotUtils.rotateYXZ(scale * growScale * isRightHand, (float) targetHalfBbHeight, 0, getYHeadRot(), 0, 0))));
        bone1.setPos(this.position().add(RotUtils.rotateYXZ(bbWidth * isRightHand, (float) targetHalfBbHeight, bbWidth * 2, getYHeadRot(), 0, 0)));
        bone1.initOrientation(0, -90f, 0);
        bone1.gravity(gravity);
        bone1.updateOBB();
        level().addFreshEntity(bone1);
    }
    public RotationBone createLaterBone(String attackUUID, float scale, float growScale, int lifetime) {
        RotationBone leftBone = new RotationBone(this.level(), this, scale, growScale, lifetime, getAttackDamage());
        leftBone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackUUID, (byte) 4));
        return leftBone;
    }
    public RotationBone createRotationBone(String attackUUID, float scale, float growScale, int lifetime) {
        RotationBone leftBone = new RotationBone(this.level(), this, scale, growScale, lifetime, getAttackDamage());
        leftBone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackUUID, (byte) 8));
        return leftBone;
    }

    public int summonGroundBoneWallAroundTarget(LivingEntity target, ColorAttack colorAttack, float growScale) {
        summonGroundBoneWall(target, colorAttack, 1.2F + getStaminaFactor() * 0.1F + getPhaseFactor() * 0.2F, growScale, LocalDirection.FRONT, 0, 3, 12.0);
        summonGroundBoneWall(target, colorAttack, 1.2F + getStaminaFactor() * 0.1F + getPhaseFactor() * 0.2F, growScale, LocalDirection.BACK, 0, 3, 12.0);
        summonGroundBoneWall(target, colorAttack, 1.2F + getStaminaFactor() * 0.1F + getPhaseFactor() * 0.2F, growScale, LocalDirection.LEFT, 0, 3, 12.0);
        return summonGroundBoneWall(target, colorAttack, 1.2F + getStaminaFactor() * 0.1F + getPhaseFactor() * 0.2F, growScale, LocalDirection.RIGHT, 0, 3, 12.0);
    }

    /**
     * 在指定方向召唤前进骨墙
     */
    public int summonGroundBoneWall(LivingEntity target, ColorAttack color, float scale, float growScale, LocalDirection direction, int delay, int shotDelay, double distance) {
        Level level = this.level();
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = 8 + Math.max(8, (int) (target.getBbWidth() * 20) + getPhaseFactor() * 5) & ~1;// 确保为偶数
        float speed = 0.75F + getStaminaFactor() * 0.25F;
        float spacing = 0.375f * scale;
        float xOffset = -spacing * (count - 1) * 0.5f;
        // 计算骨砖朝向角度
        Vec3 toTarget = target.position().subtract(this.position());
        float casterYRot = RotUtils.yRotD(toTarget);
        // 计算骨砖朝向角度
        float boneLookYRot = switch (direction) {
            case BACK -> casterYRot + 180;   // 后方
            case LEFT -> casterYRot - 90;    // 左侧
            case RIGHT -> casterYRot + 90;   // 右侧
            default -> casterYRot;           // 前方
        };
        int lifetime = (int) (shotDelay + distance / speed);
        // 计算生成中心位置
        Vec3 lookVector = calculateViewVector(0, boneLookYRot);
        Vec3 centerPos = target.position().add(lookVector.scale(-distance));
        double groundY = EntityUtils.findGroundY(level, target.position());
//        log.info("地面位置：{}",groundY);
        centerPos = new Vec3(centerPos.x, groundY, centerPos.z);
        for (int i = 0; i < count; i++) {
            // 计算每个骨砖的位置
            Vec3 finalPos = centerPos.add(RotUtils.rotateY(new Vec3(xOffset, 0, 0), boneLookYRot));
            GroundBone bone = createGroundBone(attackTypeUUID, scale, growScale, lifetime * 2).colorAttack(color).holdTimeScale(0.8F).delay(delay).delayShoot(shotDelay, speed);
            bone.setPos(finalPos);
            RotUtils.lookVec(bone, lookVector);
            level.addFreshEntity(bone);
            xOffset += spacing;
        }
        return lifetime;
    }

    /**
     * 将目标困在矩形骨内
     */
    public void summonGroundBoneArrange(LivingEntity target, float scale, double distance, int lifetime) {
        Level level = this.level();
        // 计算骨砖朝向角度
        Vec3 toTarget = target.position().subtract(this.position());
        float yRot = RotUtils.yRotD(toTarget);
        // 计算生成中心位置
        Vec3 lookVector = calculateViewVector(0, yRot);
        Vec3 perpendicular = new Vec3(-lookVector.z, 0, lookVector.x).normalize();
        double groundY = EntityUtils.findGroundY(level, target.position());
        float spacing = 0.375f * scale;
        int count = 10 + Math.max(8, (int) (target.getBbWidth() * 20)) | 1;// 确保为偶数
        float xOffset = spacing * (count - 1) * 0.5f;
        float iScale = 3F;
        float iSpacing = iScale * 0.275F;
        int rows = (int) (distance / iSpacing) + 5;
        String attackTypeUUID = UUID.randomUUID().toString();
        Vec3 centerPos = target.position().add(lookVector.scale(-rows * spacing));
        centerPos = new Vec3(centerPos.x, groundY, centerPos.z);
        float length = rows * iSpacing;
        int delay = 0;
        int layer = 3;
        int startOffsetLayer = -layer - 4;
        float holdTimeScale = 0.8F;
        // 矩形左右边
        for (int r = startOffsetLayer; r < rows + layer; r++) {
            for (int i = 0; i < layer; i++) {
                GroundBone lBone = createGroundBone(attackTypeUUID, iScale, 6F, lifetime).delay(delay).holdTimeScale(holdTimeScale);
                lBone.setPos(centerPos.add(RotUtils.rotateY(new Vec3(-xOffset - i * iSpacing, 0, r * iSpacing), yRot)));
                RotUtils.lookVec(lBone, perpendicular);
                level.addFreshEntity(lBone);

                GroundBone rBone = createGroundBone(attackTypeUUID, iScale, 6F, lifetime).delay(delay).holdTimeScale(holdTimeScale);
                rBone.setPos(centerPos.add(RotUtils.rotateY(new Vec3(xOffset + i * iSpacing, 0, r * iSpacing), yRot)));
                RotUtils.lookVec(rBone, perpendicular);
                level.addFreshEntity(rBone);
            }
        }
        //矩形前后边
        int halfCols = (Mth.ceil(count * spacing / iSpacing) | 1) / 2;
        for (int c = 0; c < halfCols; c++) {
            for (int i = 0; i < layer; i++) {
                // 起始边
                GroundBone lBone = createGroundBone(attackTypeUUID, iScale, 6F, lifetime).delay(delay).holdTimeScale(holdTimeScale);
                lBone.setPos(centerPos.add(RotUtils.rotateY(new Vec3(xOffset - c * iSpacing, 0, (i + startOffsetLayer) * iSpacing), yRot)));
                RotUtils.lookVec(lBone, lookVector);
                level.addFreshEntity(lBone);

                GroundBone rBone = createGroundBone(attackTypeUUID, iScale, 6F, lifetime).delay(delay).holdTimeScale(holdTimeScale);
                rBone.setPos(centerPos.add(RotUtils.rotateY(new Vec3(-xOffset + c * iSpacing, 0, (i + startOffsetLayer) * iSpacing), yRot)));
                RotUtils.lookVec(rBone, lookVector);
                level.addFreshEntity(rBone);

                // 目标后方边
                GroundBone lBoneF = createGroundBone(attackTypeUUID, iScale, 6F, lifetime).delay(delay).holdTimeScale(holdTimeScale);
                lBoneF.setPos(centerPos.add(RotUtils.rotateY(new Vec3(xOffset - c * iSpacing, 0, length + i * iSpacing), yRot)));
                RotUtils.lookVec(lBoneF, lookVector);
                level.addFreshEntity(lBoneF);

                GroundBone rBoneF = createGroundBone(attackTypeUUID, iScale, 6F, lifetime).delay(delay).holdTimeScale(holdTimeScale);
                rBoneF.setPos(centerPos.add(RotUtils.rotateY(new Vec3(-xOffset + c * iSpacing, 0, length + i * iSpacing), yRot)));
                RotUtils.lookVec(rBoneF, lookVector);
                level.addFreshEntity(rBoneF);
            }
        }
    }

    /**
     * 在指定方向召唤前进骨墙矩阵
     */
    public int summonGroundBoneMatrix(LivingEntity target, float growScale) {
        Level level = this.level();
        int difficulty = target.level().getDifficulty().getId();
        int cols = (7 + difficulty * 4) * 2;
        int rows = 14 + difficulty * 2;
        String attackTypeUUID = UUID.randomUUID().toString();
        float speed = 0.7F + 0.3f * getStaminaFactor();
        float scale = 1.5F;
        float spacing = 0.375f * scale;
        float xOffset = -spacing * (cols - 1) * 0.5f;
        // 计算骨砖朝向角度
        Vec3 toTarget = target.position().subtract(this.position());
        double toTargetLength = toTarget.length();
        float yRot = RotUtils.yRotD(toTarget);
        toTarget = new Vec3(toTarget.x, 0, toTarget.z).normalize();
        // 计算生成中心位置
        float length = rows * spacing * 1.5f;
        Vec3 centerPos = target.position().add(toTarget.scale(-length));
        double groundY = EntityUtils.findGroundY(level, target.position());
        centerPos = new Vec3(centerPos.x, groundY, centerPos.z);
        int lifetime = (int) ((length + toTargetLength) / speed);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // 计算每个骨砖的位置
                GroundBone bone = createGroundBone(attackTypeUUID, scale, growScale * (r > rows * 0.5f ? 3F : 1f), lifetime * 2).delay(0).holdTimeScale(0.95F);
                bone.setPos(centerPos.add(RotUtils.rotateY(new Vec3(xOffset + c * spacing, 0, -r * spacing), yRot)));
                bone.shoot(toTarget.scale(speed));
                level.addFreshEntity(bone);
            }
        }
        return lifetime;
    }

    public void summonSpecialGlobalGroundBoneMatrix(LivingEntity target) {
        Level level = this.level();
        String attackTypeUUID = UUID.randomUUID().toString();
        int rows = 50;
        int halfRows = rows / 2;
        int cols = 28;
        float scale = 4f;
        float growScale = 0.8f;
        float spacing = scale * 0.375f;
        float height = scale*growScale;
        float rowSpacing = scale* 0.3F;
        float colOffset = -spacing * (cols - 1) * 0.5f;
        float heightBase = height+0.5f;
        // 计算骨砖朝向角度
        float yRot = 90f;
        double groundY = EntityUtils.findGroundY(level, originPos);
        Vec3 centerPos = new Vec3(originPos.x, groundY, originPos.z);
        int delay = 10;
        // target
        Vec3 pos = target.position().add(RotUtils.rotateY(-2,height,0,structYaw));
        PlatformBlockEntity platform = new PlatformBlockEntity(this.level(),2.0f,1.0f).anchorPos(pos).isSaved(false);
        platform.setPos(pos);
        level.addFreshEntity(platform);
        // this
        pos = this.position().add(0,height,0);
        platform = new PlatformBlockEntity(this.level(),2.0f,1.0f).anchorPos(pos).isSaved(false);
        platform.setPos(pos);
        level.addFreshEntity(platform);
        for (int i=0;i<2;i++){
            float dx = (i==0?colOffset:-colOffset)*0.5f;
            RotationBone bone = createRotationBone(UUID.randomUUID().toString(), scale, growScale*4F, 20000).angularVelocity(new Vector3f(0, (i==0?30F:-30F)*Mth.DEG_TO_RAD, 0)).holdTimeScale(0.995F)
                    .motion(new SpringMotionModel(0.001F),originRela(0,heightBase,dx));
            bone.setPos(this.position().add(RotUtils.rotateY(0,heightBase,dx,structYaw)));
            bone.initOrientation(0, -90f, 0);
            bone.updateOBB();
            level().addFreshEntity(bone);
            RotationBone bone1 = createRotationBone(UUID.randomUUID().toString(), scale, growScale*4F, 20000).angularVelocity(new Vector3f(0, (i==0?30F:-30F)*Mth.DEG_TO_RAD, 0)).holdTimeScale(0.995F)
                    .motion(new SpringMotionModel(0.001F),originRela(0,heightBase,dx));
            bone1.setPos(this.position().add(RotUtils.rotateY(0,heightBase,dx,structYaw)));
            bone1.initOrientation(0, 90f, 0);
            bone1.updateOBB();
            level().addFreshEntity(bone1);
        }
        this.teleportRelative(0,height+0.5f,0);

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
        for (int r = 0; r < rows; r++) {
            float dz = (halfRows - r) * rowSpacing;
            if(r==4){
                pos = originRela(dz,heightBase,0);
                platform = new PlatformBlockEntity(this.level(),3.0F,1.0f).anchorPos(pos).isSaved(false);
                platform.setPos(pos);
                level.addFreshEntity(platform);
            }
            if(r==13){
                pos = originRela(dz,heightBase,0);
                platform = new PlatformBlockEntity(this.level(),3.0F,1.0f).anchorPos(pos.add(RotUtils.rotateY(3,0,0,structYaw) )).motion(new SpringMotionModel(0.01f)).isSaved(false);
                platform.setPos(pos);
                level.addFreshEntity(platform);
            }
            if(r==23){
                pos = originRela(dz,heightBase,0);
                platform = new PlatformBlockEntity(this.level(),3.0F,1.0f).anchorPos(pos.add(RotUtils.rotateY(3,0,0,structYaw))).isSaved(false)
                        .motion(new CircularMotionModel(3f,0.05F));
                platform.setPos(pos);
                level.addFreshEntity(platform);
            }
            if(r==29){
                pos = originRela(dz,heightBase,0);
                platform = new PlatformBlockEntity(this.level(),3.0F,1.0f).anchorPos(pos).ease(-2f);
                platform.setPos(pos);
                level.addFreshEntity(platform);
            }
            if(r==35){
                pos = originRela(dz,heightBase,0);
                platform = new PlatformBlockEntity(this.level(),3.0F,1.0f).anchorPos(pos).motion(new SpringMotionModel(0.01F)).isSaved(false);
                platform.setPos(pos.add(0,0,4));
                level.addFreshEntity(platform);
            }
            if(r==45){
                pos = originRela(dz,heightBase,0);
                platform = new PlatformBlockEntity(this.level(),3.0F,1.0f).anchorPos(pos.add(RotUtils.rotateY(3,0,0,structYaw))).isSaved(false)
                        .motion(new CircularMotionModel(1F,0.2F));
                platform.setPos(pos);
                level.addFreshEntity(platform);
            }
            for (int c = 0; c < cols; c++) {
                float dx = colOffset + c * spacing;
                GroundBone bone = createGroundBone(attackTypeUUID, scale, growScale, 20000).delay(delay).holdTimeScale(-2f);
                bone.setPos(centerPos.add(RotUtils.rotateY(new Vec3(dx, 0, dz), structYaw+yRot)));
                bone.setYRot(structYaw+yRot);
                level.addFreshEntity(bone);
            }
            delay += 1;
        }
    }


    public GroundBone createGroundBone(String attackTypeUUID, float scale, float growScale, int lifetime) {
        GroundBone bone = new GroundBone(this.level(), this, scale, growScale, lifetime, getAttackDamage());
        bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackTypeUUID, (byte) 6));
        return bone;
    }


    public void summonCircleGroundBoneSpine(LivingEntity target, int layer, float growScale, int lifetime, int delay, float holdTimeScale) {
        summonCircleGroundBoneSpine(target, layer, growScale, lifetime, delay, holdTimeScale, 8f * (1 + getStaminaFactor() + getPhaseFactor()));
    }

    /**
     * 以目标位置为中心的圆形骨刺
     *
     * @param target 目标
     * @param layer  圆环层数
     */
    public void summonCircleGroundBoneSpine(LivingEntity target, int layer, float growScale, int lifetime, int delay, float holdTimeScale, float randomScale) {
        String attackTypeUUID = UUID.randomUUID().toString();
        float spacing = 0.7f;
        Vec3 centerPos = target.position();
        Direction gravity = target.getData(AttachmentTypes.GRAVITY);
        this.level().addFreshEntity(createGroundBone(attackTypeUUID, centerPos, 1.0f, growScale, lifetime, delay, 0f, 0f).gravity(gravity).holdTimeScale(holdTimeScale));
        this.level().playSound(null, centerPos.x, centerPos.y, centerPos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Cylinder((float) centerPos.x, (float) centerPos.y, (float) centerPos.z, layer * spacing, growScale, 10, WarningTip.RED, gravity));
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = spacing * (i + 1);
            float angle = interval; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                Vec3 pos = centerPos.add(GravityUtils.localToWorld(gravity, r * Math.cos(angle * Math.PI / 180F), 0, r * Math.sin(angle * Math.PI / 180F)));
                pos = GravityUtils.findGround(this.level(),pos,gravity);
                GroundBoneOBB groundBone = createGroundBone(attackTypeUUID, pos, 1.0f, growScale, lifetime, delay, -angle, (float) (this.random.nextGaussian() * randomScale)).gravity(gravity).holdTimeScale(holdTimeScale);
                this.level().addFreshEntity(groundBone);
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
        float scale = 0.9f + 0.1f * difficulty + 0.2f * phaseFactor;
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
            PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.QuadPrecession(
                    (float) getX(), (float) EntityUtils.findGroundY(this.level(), target.getX(), target.getZ(), target.getY(), target.getY()) + (i * 0.01f + 0.01f), (float) getZ(), 20, WarningTip.RED,
                    length, width + 0.1f, currentYaw));
            summonGroundBoneSpineWave(target, scale, growScale, rows, cols,
                    this.position(), currentDir, 14, 30 * (phaseFactor + 1), currentYaw, 0.4f * (phaseFactor + 1));
        }
    }

    /**
     * 公用方法：召唤定向骨刺波动
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
                this.level().addFreshEntity(createGroundBone(
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
        float scale = 1.2f + getPhaseFactor() * 0.2F;
        float spacing = 0.7f * scale;
        float growScale = 1.5F + staminaFactor;
        this.level().playSound(null, pos.x, pos.y, pos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.Cylinder((float) pos.x, (float) pos.y, (float) pos.z, layer * spacing, growScale, 20, WarningTip.RED));
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = spacing * (i + 1);
            float angle = interval; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                this.level().addFreshEntity(createGroundBone(
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
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.QuadCirclePrecession(
                (float) getX(), (float) EntityUtils.findGroundY(this.level(), target.getX(), target.getZ(), target.getY(), target.getY()) + 0.01f, (float) getZ(), 30, WarningTip.RED,
                (float) distanceToTarget, colSpacing * cols, baseYaw, scale * growScale * 1.3f));
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
                GroundBoneOBB groundBone = createGroundBone(attackTypeUUID, bonePos.x, bonePos.z, minY, maxY,
                        scale, growScale, lifetime, delay, yaw, pitch).holdTimeScale(0.8f);
                groundBone.updateOBB();
                this.level().addFreshEntity(groundBone);
            }
            delay += (row % 3 == 0 ? 1 : 0);
        }
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
            float randomGrowScale = growScale * (1.0F + this.random.nextFloat());  // 1.5 - 3.0 倍
            this.level().addFreshEntity(createGroundBone(attackTypeUUID, bonePos.x, bonePos.z, minY, maxY,
                    randomScale, randomGrowScale, lifetime, delay, yaw, pitch).holdTimeScale(0.8f));
        }
    }

    public int summonHugeParametricGroundBoneSpineWave(LivingEntity target) {
        int type = this.random.nextInt(6);
        return summonHugeParametricGroundBoneSpineWave(target,type);
    }
    public int summonHugeParametricGroundBoneSpineWave(LivingEntity target,int type) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int phaseFactor = getPhaseFactor();
        int curveCount = 10 + phaseFactor * 6;
        int lifetime = 30;
        float sizeScale = 2.0f;
        float growScale = 3.0f;
        int pointsPerCurve = 60;
        float spacing = 0.175F * sizeScale;
        float radius = pointsPerCurve * spacing;
        Vec3 targetPos = target.position();
        // 反向，使 t=0 在最外层，t=1 在原点
        Direction gravity = target.getData(AttachmentTypes.GRAVITY);
        targetPos = GravityUtils.findGround(this.level(), targetPos, gravity);
        int tipLifetime = 40;
        int delay = tipLifetime / 2;
        Function<Float, Vec3> curve = ParametricCurveUtils.reverse(switch (type) {
            case 1 -> {
                PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.RadialPrecessionCurveStripsGravityPacket((float) targetPos.x, (float) targetPos.y, (float) targetPos.z, tipLifetime, WarningTip.RED, curveCount, radius, spacing, pointsPerCurve, gravity, ParametricCurveType.FRACTAL_REVERSED, 8, 1.0F));
                yield ParametricCurveUtils.fractal(8, 1.0F);
            }
            case 2 -> {
                PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.RadialPrecessionCurveStripsGravityPacket((float) targetPos.x, (float) targetPos.y, (float) targetPos.z, tipLifetime, WarningTip.RED, curveCount, radius, spacing, pointsPerCurve, gravity, ParametricCurveType.FLOWER_REVERSED, 4));
                yield ParametricCurveUtils.flower(4);
            }
            case 3 -> {
                curveCount-=2;
                PacketDistributor.sendToPlayersTrackingEntity(this,
                        new WarningTipPacket.RadialPrecessionCurveStripsGravityPacket(
                                (float) targetPos.x, (float) targetPos.y, (float) targetPos.z,
                                tipLifetime, WarningTip.RED, curveCount, radius, spacing,
                                pointsPerCurve, gravity, ParametricCurveType.SAWTOOTH_RADIAL_REVERSED,
                                4.2f, 0.45f  // 示例参数，可自行定义
                        ));
                yield ParametricCurveUtils.sawtoothRadial(4.2f, 0.45f);
            }
            case 4 -> {
                PacketDistributor.sendToPlayersTrackingEntity(this,
                        new WarningTipPacket.RadialPrecessionCurveStripsGravityPacket(
                                (float) targetPos.x, (float) targetPos.y, (float) targetPos.z,
                                tipLifetime, WarningTip.RED, curveCount, radius, spacing,
                                pointsPerCurve, gravity, ParametricCurveType.STARBURST_REVERSED,
                                6.0f, 0.6f
                        ));
                yield ParametricCurveUtils.starburst(6.0f, 0.6f);
            }
            case 5 -> {
                curveCount-=1;
                PacketDistributor.sendToPlayersTrackingEntity(this,
                        new WarningTipPacket.RadialPrecessionCurveStripsGravityPacket(
                                (float) targetPos.x, (float) targetPos.y, (float) targetPos.z,
                                tipLifetime, WarningTip.RED, curveCount, radius, spacing,
                                pointsPerCurve, gravity, ParametricCurveType.WAVEFOLD_REVERSED,
                                4f, 8f, 0.3f
                        ));
                yield ParametricCurveUtils.wavefold(4f, 8f, 0.3f);
            }
            default -> {
                PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket.RadialPrecessionCurveStripsGravityPacket((float) targetPos.x, (float) targetPos.y, (float) targetPos.z, tipLifetime, WarningTip.RED, curveCount, radius, spacing, pointsPerCurve,gravity, ParametricCurveType.HEART_REVERSED));
                yield ParametricCurveUtils.heart();
            }
        });
        this.level().playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvnets.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        for (int s = 0; s < curveCount; s++) {
            float baseAngle = s * 360f / curveCount;
            delay = 20;
            for (int p = 0; p < pointsPerCurve; p++) {
                Vec3 unitPos = curve.apply((float) p / pointsPerCurve);         // 单位曲线上的点
                Vec3 localPos = unitPos.scale(radius); // 缩放到实际半径
                double rad = Math.toRadians(baseAngle);
                double rotatedX = localPos.x * Math.cos(rad) - localPos.z * Math.sin(rad);
                double rotatedZ = localPos.x * Math.sin(rad) + localPos.z * Math.cos(rad);
                Vec3 pos = targetPos.add(GravityUtils.localToWorld(gravity, rotatedX, localPos.y, rotatedZ));
                pos = GravityUtils.findGround(this.level(),pos, gravity);

                this.level().addFreshEntity(createGroundBone(attackTypeUUID, pos, sizeScale, growScale, lifetime, delay, 0, 0).gravity(gravity).holdTimeScale(0.6f));
                delay += p % 2;
            }
        }
        return delay;
    }

    /**
     * 创建地面骨，默认重力下的，需要查找y轴位置
     *
     * @param scale     地面骨缩放大小
     * @param growScale 地面骨生长缩放大小
     */
    protected GroundBoneOBB createGroundBone(String attackUUID, double targetX, double targetZ, double minY, double maxY, float scale, float growScale, int lifetime, int delay, float yRot, float xRot) {
        Level level = this.level();
        double spawnY = EntityUtils.findGroundY(level, targetX, targetZ, minY, maxY);
        // 如果找到有效地面，生成骨刺
        GroundBoneOBB bone = new GroundBoneOBB(level, this, scale, growScale, getAttackDamage(), lifetime, delay);
        if (spawnY != level.getMinBuildHeight()) {
            bone.setPos(targetX, spawnY, targetZ);
            bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackUUID, (byte) 6));
            bone.orientation(yRot, xRot, 0);
        }
        return bone;
    }

    /**
     * 创建地面骨，直接设置位置
     *
     * @param scale     地面骨缩放大小
     * @param growScale 地面骨生长缩放大小
     */
    protected GroundBoneOBB createGroundBone(String attackUUID, Vec3 pos, float scale, float growScale, int lifetime, int delay, float yRot, float xRot) {
        Level level = this.level();
        // 如果找到有效地面，生成骨刺
        GroundBoneOBB bone = new GroundBoneOBB(level, this, scale, growScale, getAttackDamage(), lifetime, delay);
        bone.setPos(pos);
        bone.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(attackUUID, (byte) 6));
        bone.orientation(yRot, xRot, 0);
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
                gb.setPos(this.getEyePosition().add(RotUtils.rotateYX(
                        this.random.nextDouble() * 16 - 8,
                        this.random.nextDouble() * 6 + 1,
                        this.random.nextDouble() * 4,
                        this.getYHeadRot(), this.getXRot()
                )));
            } while (this.level().noCollision(gb, gb.getBoundingBox()) && ++attempts < 16);
            gb.aim(target);
            gb.restAnimPos();
            this.level().addFreshEntity(gb);
        }
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
     * @param radius      半径
     * @param offsetAngle 初始偏移角度（度）
     * @param angleStep   角度步长（度）
     */
    public void summonGBAroundTarget(LivingEntity target, int count, float radius, float offsetAngle, float angleStep, float size) {
        Direction gravity = target.getData(AttachmentTypes.GRAVITY);
        radius*=0.5f;
        Vec3 toTarget = target.position().subtract(this.position());
        Vec3 centerPos = this.position().add(toTarget.scale(0.5f));
        float currentAngle = offsetAngle; // 从指定角度开始
        for (int i = 0; i < count; i++, currentAngle += angleStep) {
            GasterBlaster gb = createGasterBlaster(size);
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = -Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            gb.setPos(centerPos.add(RotUtils.rotateYX(xOffset, target.getBbHeight() * 0.5f, zOffset, RotUtils.yRotD(toTarget), 0)));
            gb.aim(target.position().add(GravityUtils.localToWorld(gravity, 0.0, target.getBbHeight() * 0.5f, 0.0)));
            gb.restAnimPos();
            this.level().addFreshEntity(gb);
        }
    }


    /**
     * 用于单击，环绕目标周围
     */
    public void summonGBAroundTarget(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        int count = 2 + this.random.nextInt(1 + difficulty + getPhaseFactor());
        summonGBAroundTarget(target, count, this.random.nextInt(count) * 180f / count);
    }

    /**
     * 在目标周围召唤GB，环绕目标攻击
     */
    public void summonGBAroundTarget(LivingEntity target, int count, float offsetAngle) {
        summonGBAtTargetHeight(target, count, offsetAngle, target.getBbHeight() * 0.5f);
    }

    /**
     * 在目标所在位置周围的指定高度上召唤四周GB，用于重力控制的追加技能，根据数量自动计算角度步长和大小
     */
    public void summonGBAtTargetHeight(LivingEntity target, int count,float offsetAngle, float height) {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 position = target.position();
        Direction gravity = target.getData(AttachmentTypes.GRAVITY);
        float radius = target.getBbWidth() * 13;
        float angleStep = 360f / count;
        float size = 1.25F + (2 + difficulty - count) * 0.25f;
        float currentAngle = offsetAngle; // 从指定角度开始
        for (int i = 0; i < count; i++, currentAngle += angleStep) {
            GasterBlaster gb = createGasterBlaster(size);
            gb.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(UUID.randomUUID().toString(), (byte) 10));
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = -Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            gb.setPos(position.add(GravityUtils.localToWorld(gravity, xOffset, height, zOffset)));
            gb.aim(position.add(GravityUtils.localToWorld(gravity, 0.0, height, 0.0)));
            gb.restAnimPos();
            this.level().addFreshEntity(gb);
        }
    }
    /**
     * 在目标所在位置周围的指定高度上召唤四周GB，用于重力控制的追加技能，根据数量自动计算角度步长和大小
     */
    public void summonGBAimOriginPos(LivingEntity target, int count,float offsetAngle,float size) {
        float height = target.getBbHeight() * 0.5f;
        float radius = target.getBbWidth() * 13;
        float angleStep = 360f / count;
        float currentAngle = offsetAngle; // 从指定角度开始
        for (int i = 0; i < count; i++, currentAngle += angleStep) {
            GasterBlaster gb = createGasterBlaster(size);
            gb.setData(AttachmentTypes.KARMA_ATTACK, new KaramJudge(UUID.randomUUID().toString(), (byte) 10));
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = -Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            gb.setPos(originPos.add(xOffset, height, zOffset));
            gb.aim(originPos.add( 0.0, height, 0.0));
            gb.restAnimPos();
            this.level().addFreshEntity(gb);
        }
    }
    /**
     * 给仁慈阶段使用的自定义射击时间GB
     */
    public void summonGBAroundTarget(LivingEntity target, int count, float radius, int shot) {
        Direction gravity = target.getData(AttachmentTypes.GRAVITY);
        float angleStep = 360f / count;
        float currentAngle = 0; // 从指定角度开始
        radius = target.getBbWidth() * radius;
        for (int i = 0; i < count; i++, currentAngle += angleStep) {
            GasterBlaster gb = createGasterBlaster(2.0f).shot(shot);
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = -Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            gb.setPos(target.position().add(GravityUtils.localToWorld(gravity, xOffset, target.getBbHeight() * 0.5f, zOffset)));
            gb.aim(target.position().add(GravityUtils.localToWorld(gravity, 0.0, target.getBbHeight() * 0.5f, 0.0)));
            gb.restAnimPos();
            this.level().addFreshEntity(gb);
        }
    }


    public GasterBlaster controlGBAim(LivingEntity target) {
        int factor = getPhaseFactor();
        int difficulty = getDifficulty();
        float size = 0.5f + difficulty * 0.3334f + factor * 0.5f;
        GasterBlaster gb = new GasterBlaster(level(), this, getAttackDamage(), size, (34 - factor * 17), (int) (100 * size), 100).follow(new Vec3(0, this.getBbHeight() * 0.5f, 1))
                .aimSmoothSpeed(0.11f + getStaminaFactor() * 0.02f + factor * 0.02f);
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
        Direction gravity = GravityUtils.applyRelativeGravity(this, target, direction);
        gravitySlam(target, gravity, acceleration);
    }
    public void gravitySlam(LivingEntity target, Direction direction, float acceleration) {
        gravitySlamDirect(target, GravityUtils.applyGravity(target, direction), acceleration);
    }
    public void gravitySlamDirect(LivingEntity target, Direction direction, float acceleration) {
        target.addDeltaMovement(new Vec3(0, -acceleration, 0));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new GravityPacket(target.getId(), direction, -acceleration));
        applyGravityControlAcc(target, 0.08F);
    }

    public void applyGravityControlAcc(LivingEntity target, float acc) {
        target.setData(AttachmentTypes.GRAVITY_ACC, acc);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new GravityAccPacket(target.getId(), acc));
    }

    public void timeJumpTeleport(LivingEntity target, int duration) {
        PacketDistributor.sendToPlayersTrackingEntity(this, new TimeJumpTeleportPacket(target.getId(), duration));
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvnets.SANS_TELEPORT_TIME_JUMP.get(), SoundSource.HOSTILE);
    }

    public Vec3 originRela(double dx, double dy, double dz) {
        Vec3 vec3 = RotUtils.rotateY(dx, dy, dz, structYaw);
        return originPos.add(vec3);
    }
    public void teleportOrigin(LivingEntity target,double dx,double dy,double dz) {
        Vec3 pos = originRela(dx,dy,dz);
        target.teleportTo(pos.x,pos.y,pos.z);
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
    public int getAnimID() {
        return animId;
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public AnimatableManager<Sans> getAnimatableManager() {
        return getAnimatableInstanceCache().getManagerForId(this.getId());
    }

    public AnimationController<Sans> getAttackAnimController() {
        return getAnimatableManager().getAnimationControllers().get("attack");
    }

    public static final RawAnimation ANIM_CAST = RawAnimation.begin().thenPlay("attack.cast");
    public static final RawAnimation ANIM_CAST_LEFT = RawAnimation.begin().thenPlay("attack.cast.left");
    public static final RawAnimation ANIM_CAST_CIRCLE = RawAnimation.begin().thenPlay("attack.cast.circle");
    public static final RawAnimation ANIM_CAST_CIRCLE_LEFT = RawAnimation.begin().thenPlay("attack.cast.circle.left");
    public static final RawAnimation ANIM_BONE_PROJECTILE = RawAnimation.begin().thenPlay("attack.bone.projectile");
    public static final RawAnimation ANIM_BONE_PROJECTILE_LEFT = RawAnimation.begin().thenPlay("attack.bone.projectile.left");
    public static final RawAnimation ANIM_BONE_SWEEP = RawAnimation.begin().thenPlay("attack.bone.sweep");
    public static final RawAnimation ANIM_BONE_SWEEP_LEFT = RawAnimation.begin().thenPlay("attack.bone.sweep.left");
    public static final RawAnimation ANIM_GB_CONTROL = RawAnimation.begin().thenPlay("attack.gb.control");
    public static final RawAnimation ANIM_BONE_ROTATION = RawAnimation.begin().thenPlay("attack.bone.rotation");
    public static final RawAnimation ANIM_POUND_GROUND = RawAnimation.begin().thenPlayAndHold("attack.pound.ground");
    public static final RawAnimation ANIM_STAMP_GROUND = RawAnimation.begin().thenPlayAndHold("attack.stamp.ground");
    public static final RawAnimation ANIM_STAMP_POUND_GROUND = RawAnimation.begin().thenPlayAndHold("attack.stamp.pound.ground");
    public static final RawAnimation[] THROW_ANIMATIONS = new RawAnimation[]{
            RawAnimation.begin().thenPlay("attack.throw.up"),
            RawAnimation.begin().thenPlay("attack.throw.down"),
            RawAnimation.begin().thenPlay("attack.throw.left"),
            RawAnimation.begin().thenPlay("attack.throw.right"),
            RawAnimation.begin().thenPlay("attack.throw.front")
//            RawAnimation.begin().thenPlay("attack.throw.back")
    };
    private int lastTickCount = -1;

    @Override
    public void setAnimID(int id) {
        this.animId = id;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Sans> attackController = new AnimationController<>(this, "attack", state -> {
            AnimationController<Sans> controller = state.getController();
            if (animId == -1) {
                return PlayState.STOP;
            }
            if (state.isCurrentAnimation(ANIM_CAST_LEFT) || state.isCurrentAnimation(ANIM_CAST_CIRCLE_LEFT) || state.isCurrentAnimation(ANIM_BONE_PROJECTILE_LEFT) || state.isCurrentAnimation(ANIM_BONE_SWEEP_LEFT)) {
                if (tickCount != lastTickCount) {
                    Map<String, BoneAnimationQueue> boneAnimationQueues = state.getController().getBoneAnimationQueues();
                    BoneAnimationQueue leftHand = boneAnimationQueues.get("left_hand");
                    Vector3d worldPosition = leftHand.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                    lastTickCount = tickCount;
                }
            }
//             || state.isCurrentAnimation(ANIM_BONE_PROJECTILE) || state.isCurrentAnimation(ANIM_BONE_SWEEP)
            if (state.isCurrentAnimation(ANIM_CAST) || state.isCurrentAnimation(ANIM_CAST_CIRCLE) ||
                    state.isCurrentAnimation(ANIM_GB_CONTROL) || state.isCurrentAnimation(ANIM_POUND_GROUND) || state.isCurrentAnimation(ANIM_STAMP_POUND_GROUND)) {
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

            for (RawAnimation throwAnimation : THROW_ANIMATIONS) {
                if (state.isCurrentAnimation(throwAnimation)) {
                    if (tickCount != lastTickCount) {
                        Map<String, BoneAnimationQueue> boneAnimationQueues = state.getController().getBoneAnimationQueues();
                        BoneAnimationQueue leftHand = boneAnimationQueues.get("left_hand");
                        Vector3d worldPosition = leftHand.bone().getWorldPosition();
                        level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.36f, 0.66f, 0.94F), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                        lastTickCount = tickCount;
                    }
                }
            }
            if (controller.hasAnimationFinished()) {
                controller.stop();
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
                    controller.setAnimation(ANIM_GB_CONTROL);
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


    public static boolean isSameRawAnimation(RawAnimation curr, RawAnimation... animations) {
        for (RawAnimation animation : animations) {
            if (animation.equals(curr)) return true;
        }
        return false;
    }

    // 蓝色（激光风格）
    public static final int[][] ENERGY_AQUA = {
            {226, 255, 255,255},    // 内层 能量层：高亮白（最亮）
            {0, 97, 165,255},       // 外层 泛光层：加法混合下呈现浅蓝
            {25, 97, 165, 255}     // 外层 流动条纹层：加法混合下呈现亮蓝偏青
    };
    public final RadialPlaneTrailStrip leftHandTrail = new RadialPlaneTrailStrip(10f).color(ENERGY_AQUA[1]).progressCurve((t) -> CurvesUtils.powerFallEaseOut(t, 2)).width(0.2F);
    public final RadialPlaneTrailStrip rightHandTrail = new RadialPlaneTrailStrip(10f).color(ENERGY_AQUA[1]).progressCurve((t) -> CurvesUtils.powerFallEaseOut(t, 2)).width(0.2F);

}

