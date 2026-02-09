package com.sakpeipei.undertale.entity.boss;

import com.sakpeipei.undertale.common.anim.AnimStep;
import com.sakpeipei.undertale.common.anim.SequenceAnim;
import com.sakpeipei.undertale.common.anim.SingleAnim;
import com.sakpeipei.undertale.common.anim.TimelineAnim;
import com.sakpeipei.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.undertale.common.phys.LocalDirection;
import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.entity.ai.goal.NeutralMobAngerTargetGoal;
import com.sakpeipei.undertale.entity.ai.goal.SequenceAnimGoal;
import com.sakpeipei.undertale.entity.ai.goal.SingleAnimGoal;
import com.sakpeipei.undertale.entity.ai.goal.TimelineAnimGoal;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GroundBone;
import com.sakpeipei.undertale.entity.summon.LateralBone;
import com.sakpeipei.undertale.entity.summon.MovingGroundBone;
import com.sakpeipei.undertale.network.GravityPacket;
import com.sakpeipei.undertale.network.TimeJumpTeleportPacket;
import com.sakpeipei.undertale.network.WarningTipPacket;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.undertale.registry.SoundRegistry;
import com.sakpeipei.undertale.utils.EntityUtils;
import com.sakpeipei.undertale.utils.LevelUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.ToIntFunction;

public class Sans extends Monster implements NeutralMob, GeoEntity, IAnimatable, IEntityWithComplexSpawn {
    private static final Logger log = LogManager.getLogger(Sans.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 中立生物相关
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(30, 60);
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;


    private final static short ATTACK_RANGE = 16;  // 攻击距离

    public static final byte OPENING_ATTACK = 0;
    public static final byte FIRST_PHASE = 1;
    public static final byte MERCY_PHASE = 2;
    public static final byte SECOND_PHASE = 3;
    public static final byte SPECIAL_ATTACK = 4;
    public static final byte END_PHASE = 5;

    private static final EntityDataAccessor<Byte> PHASE_ID = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> IS_EYE_BLINK = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STAMINA = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.INT);
    private int maxStamina;  // 最大体力/耐力
    private int fatigueLevel;// 疲劳等级，出汗等级


    boolean isAppendSpine; // 是否追加骨刺，主要用于重力控制后，落地触发


    private byte animId = -1;
    private float animSpeed;


    public Sans(EntityType<? extends Monster> type, Level level) {
        super(type, level);
//        maxStamina = level.getDifficulty().getId() * 5;
        maxStamina = 5;
        setStamina(maxStamina);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SequenceAttackGoal());
//        this.goalSelector.addGoal(2, new PersistentAttackGoal());
//        this.goalSelector.addGoal(3, new SingleAttackGoalSingle());
        this.goalSelector.addGoal(4, new SansMovementGoal(1.0, 16.0f));


        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomStrollGoal(this, 0.5f));

        this.targetSelector.addGoal(0, new NeutralMobAngerTargetGoal(this, this.level()));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        if (this.globalCD > 0) {
            this.globalCD--;
        }
        int stamina = getStamina();
        if (getPhaseID() == FIRST_PHASE && this.tickCount % 40 == 0) {
            stamina = Math.min(stamina + 1, maxStamina);
            setStamina(stamina);
        }
        //todo 判断玩家是否选择了饶恕
        LivingEntity target = this.getTarget();
        if (target != null) {
            if (false) {
                gravityControl(target, LocalDirection.DOWN);
                isAppendSpine = true;
            }
            if (target.hasData(AttachmentTypeRegistry.GRAVITY) && target.onGround() && isAppendSpine) {
                if (false) {

                }
                this.summonGroundBoneSpineAtTarget(target);
                isAppendSpine = false;
            }
        }

        super.customServerAiStep();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float power) {
        Entity sourceEntity = source.getEntity();
        Entity directEntity = source.getDirectEntity();
        if (isInvulnerableTo(source) || source.is(Tags.DamageTypes.IS_ENVIRONMENT)) {
            return false;
        }
        int stamina = getStamina();
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
            flag = true;
        } else if (phaseID == SPECIAL_ATTACK || (sourceEntity == null && directEntity == null)) {
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
            if (phaseID <= FIRST_PHASE) {
                stamina = Math.max(maxStamina / 2, stamina - Mth.ceil(power));
                if (stamina <= maxStamina / 2) {
                    this.entityData.set(PHASE_ID, MERCY_PHASE);
                    fatigueLevel = 1;
                }
            } else if (phaseID == SECOND_PHASE) {
                stamina = Math.max(0, stamina - Mth.ceil(power));
                this.setStamina(stamina);
                if (stamina == 0) {
                    this.entityData.set(PHASE_ID, END_PHASE);
                }
            }
        }

        if (sourceEntity == null) {
            rangedTeleport(directEntity);
        } else {
            if (sourceEntity instanceof LivingEntity livingEntity) {
                this.setLastHurtByMob(livingEntity);
            } else if (sourceEntity instanceof Player player) {
                this.setLastHurtByPlayer(player);
            }
            // todo 根据目标实体的攻击范围决定是远程攻击还是近战攻击
            if (sourceEntity instanceof Mob mob) {
            }
            if (sourceEntity instanceof Player player) {
            }
            if (this.distanceToSqr(sourceEntity) <= 25) {
                meleeTeleport(sourceEntity);
            } else {
                rangedTeleport(directEntity);
            }
        }
        return false;
    }

    private void rangedTeleport(Entity entity) {
        float baseAngle;
        double r;
        if (entity == null) {
            baseAngle = this.getYHeadRot() - 90f;
            r = 3;
        } else {
            r = entity.getBbWidth() + this.getBbWidth() + 2 * getPickRadius();
            Vec3 movement = entity.getDeltaMovement();
            if (movement.lengthSqr() == 0f) {
                baseAngle = -RotUtils.yRotD(this.position().subtract(entity.position()));
            } else {
                baseAngle = -RotUtils.yRotD(movement);
            }
        }
        for (int i = 0; i < 64; i++) {
            boolean b = random.nextBoolean();
            float angle = baseAngle + (b ? 90 : -90);
            r = r + 0.5 + random.nextDouble() * 0.5;
            double targetX = this.getX() + Mth.cos(angle * Mth.DEG_TO_RAD) * r;
            double targetY = this.getY() + random.nextDouble() * 16 - 8;
            double targetZ = this.getZ() + Math.sin(angle * Mth.DEG_TO_RAD) * r;

            // 检查视线
            Vec3 from = this.getEyePosition();
            Vec3 to = new Vec3(targetX, targetY, targetZ);
            if (level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                if (tryTeleportTo(targetX, targetY, targetZ)) {
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
                double distance = (double) ATTACK_RANGE - random.nextDouble() * 4.0;
                double targetX = entity.getX() + Mth.cos(angle * Mth.DEG_TO_RAD) * distance;
                double targetY = entity.getY() + random.nextDouble() * 16 - 8;
                double targetZ = entity.getZ() + Mth.sin(angle * Mth.DEG_TO_RAD) * distance;
                // 检查视线
                Vec3 from = new Vec3(targetX, targetY, targetZ);
                Vec3 to = entity.getEyePosition();
                if (level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                    if (tryTeleportTo(targetX, targetY, targetZ)) {
                        return;
                    }
                }
            }
        }
    }

    private void randomTeleport() {
        RandomSource random = this.random;
        for (int i = 0; i < 64; i++) {
            float angle = random.nextFloat() * 360f; // 90度范围内随机
            double targetX = this.getX() + Mth.cos(angle * Mth.DEG_TO_RAD) * 16;
            double targetY = this.getY() + random.nextDouble() * 64 - 32;
            double targetZ = this.getZ() + Mth.sin(angle * Mth.DEG_TO_RAD) * 16;
            // 检查视线
            if (tryTeleportTo(targetX, targetY, targetZ)) {
                return;
            }
        }
    }

    private void teleportTowards(Entity target) {
        Vec3 dir = new Vec3(target.getX() - Sans.this.getX(), target.getEyeY() - Sans.this.getEyeY(), target.getZ() - Sans.this.getZ());
        dir = dir.normalize().scale((double) ATTACK_RANGE / 2);
        Sans.this.tryTeleportTo(
                Sans.this.getX() + dir.x + (random.nextDouble() - 0.5) * 4,
                Sans.this.getY() + dir.y + (random.nextDouble() - 0.5) * 16,
                Sans.this.getZ() + dir.z + (random.nextDouble() - 0.5) * 4
        );
    }

    /**
     * Sans的传送逻辑（基于末影人原版代码优化）
     *
     * @return 是否传送成功
     */
    private boolean tryTeleportTo(double x, double y, double z) {
        // 2. 检查目标位置是否有效（类似末影人原版逻辑）
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        Level level = this.level();

        // 从目标点向下搜索可站立位置（避免悬空）
        while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            pos.move(Direction.DOWN);
        }

        BlockState blockState = level.getBlockState(pos);
        // 3. 仅传送到固体且非水方块上
        if (!blockState.getCollisionShape(level, pos).isEmpty() && !blockState.getFluidState().is(FluidTags.WATER)) {
            // 触发传送事件（允许其他模组修改坐标）
            EntityTeleportEvent.EnderEntity event = EventHooks.onEnderTeleport(this, x, y, z);
            if (event.isCanceled()) {
                return false;
            }
            // 执行实际传送
            boolean success = this.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true);
            // 4. 传送成功后的处理
            if (success) {
                // 发送游戏事件（用于红石/侦测器）
                level.gameEvent(GameEvent.TELEPORT, this.position(), GameEvent.Context.of(this));
                // 播放音效（除非Sans是静音的）
                if (!this.isSilent()) {
                    level.playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
            }
            return success;
        }
        return false;
    }


    //可攻击的中立生物需要实现的
    @Override
    public int getRemainingPersistentAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {
        this.remainingPersistentAngerTime = time;
    }

    @Override
    public @Nullable UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.remainingPersistentAngerTime = PERSISTENT_ANGER_TIME.sample(this.random);
    }

    public int getMaxStamina() {
        return maxStamina;
    }

    public int getStamina() {
        return this.entityData.get(STAMINA);
    }

    public void setStamina(int stamina) {
        this.entityData.set(STAMINA, stamina);
    }

    public byte getPhaseID() {
        return this.entityData.get(PHASE_ID);
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
    public void setAnimSpeed(float speed) {
        this.animSpeed = speed;
    }

    @Override
    public float getAnimSpeed() {
        return animSpeed;
    }

    public boolean getIsEyeBlink() {
        return this.entityData.get(IS_EYE_BLINK);
    }

    public void setIsEyeBlink(boolean blink) {
        this.entityData.set(IS_EYE_BLINK, blink);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAMINA, 0);
        builder.define(PHASE_ID, FIRST_PHASE);
        builder.define(IS_EYE_BLINK, false);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(maxStamina);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.maxStamina = buffer.readInt();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("maxStamina", this.maxStamina);
        tag.putInt("stamina", this.getStamina());
        tag.putByte("phaseId", this.getPhaseID());
        this.addPersistentAngerSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("maxStamina")) {
            this.maxStamina = tag.getInt("maxStamina");
        }
        if (tag.contains("stamina")) {
            setStamina(tag.getInt("stamina"));
        }
        if (tag.contains("phaseId")) {
            this.entityData.set(PHASE_ID, tag.getByte("phaseId"));
        }
        this.readPersistentAngerSaveData(this.level(), tag);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }


    private int seeTime;
    private int globalCD = 0;           //全局CD
    private boolean existPersistentAttack;
    private boolean isAttacking;
    private int timeJumpAttackCount = 0;

    class SansMovementGoal extends Goal {
        private final double speedModifier;
        private final float attackRadiusSqr;
        private final float backRadiusSqr;
        private final float pursuitRadiusSqr;

        public SansMovementGoal(double speedModifier, float attackRadius) {
            this.speedModifier = speedModifier;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.backRadiusSqr = this.attackRadiusSqr / 4;
            this.pursuitRadiusSqr = this.attackRadiusSqr + this.attackRadiusSqr / 2;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = Sans.this.getTarget();
            return livingentity != null && livingentity.isAlive();
        }


        @Override
        public boolean canContinueToUse() {
            return this.canUse() || !Sans.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            Sans.this.setAggressive(true);
            seeTime = 0;
            globalCD = 40;
        }

        @Override
        public void stop() {
            Sans.this.setAggressive(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                double disSqr = Sans.this.distanceToSqr(target.getX(), target.getY(), target.getZ());
                boolean hasSeeSight = Sans.this.getSensing().hasLineOfSight(target);
                if (hasSeeSight) {
                    seeTime++;
                } else {
                    seeTime = Math.max(-60, seeTime - 1);
                }
                Sans.this.getLookControl().setLookAt(target, Sans.this.getHeadRotSpeed(), Sans.this.getMaxHeadXRot());
                byte phaseID = getPhaseID();
                if (phaseID == MERCY_PHASE || phaseID == END_PHASE) {
                    return;
                }
                if (seeTime > 0) {
                    //                    ++this.strafingTime;
                    if (disSqr <= backRadiusSqr) {
                        Sans.this.getNavigation().stop();
                        Sans.this.getMoveControl().strafe(-0.75f, 0.0f);
                        Sans.this.setYRot(Sans.this.yHeadRot);
                    } else if (disSqr > backRadiusSqr && disSqr <= attackRadiusSqr) {
                        Sans.this.getNavigation().stop();
                    } else {
                        Sans.this.getNavigation().moveTo(target, speedModifier);
                        if (disSqr > pursuitRadiusSqr) {
                            teleportTowards(target);
                        }
                    }
                } else if (seeTime > -60) { // 丢失视线3秒内
                    if (disSqr <= pursuitRadiusSqr) {
                        Sans.this.getNavigation().moveTo(target, this.speedModifier);
                    } else {
                        teleportTowards(target);
                    }
                }
            }
        }
    }

    class MercyGoal extends Goal {
        @Override
        public boolean canUse() {
            return getPhaseID() == MERCY_PHASE;
        }

    }

    //持续攻击，可脱手
    class PersistentAttackGoal extends TimelineAnimGoal<Sans> {
        List<TimelineAnim> attacks = new ArrayList<>(List.of(
                new TimelineAnim((byte) 6, 30, 400, 4, mob::shootAimedBarrage),
                new TimelineAnim((byte) 6, 30, 400, 0, mob::shootForwardBarrage)
        ));

        public PersistentAttackGoal() {
            super(Sans.this);
        }

        @Override
        public boolean canUse() {
            byte phaseID = getPhaseID();
            return (phaseID == FIRST_PHASE || phaseID == SECOND_PHASE) && !isAttacking && timeJumpAttackCount == 0 && super.canUse() && seeTime > -60;
        }

        @Override
        protected @NotNull TimelineAnim select(LivingEntity target) {
            int difficulty = mob.level().getDifficulty().getId();
            List<TimelineAnim> availableAttacks = new ArrayList<>(attacks);
            int factor = mob.random.nextInt(2);
            availableAttacks.add(TimelineAnim.create(3 * (difficulty + 1 - factor), 15 - 2 * difficulty - 5 * fatigueLevel + factor * 10, 400, (byte) 7, 4, (t) -> mob.summonGBAroundSelf(t, 1, 0.5f + (difficulty + fatigueLevel) * 0.25f + factor * 0.5f)));
            if (target.onGround()) {
                availableAttacks.add(TimelineAnim.create(3 * difficulty, 26 - 3 * difficulty - 5 * fatigueLevel, 400, (byte) 7, 4, mob::summonGroundBoneSpineAtTarget));
            }
            existPersistentAttack = true;
            isAttacking = true;
            return availableAttacks.get(1);
//            return availableAttacks.get(mob.random.nextInt(availableAttacks.size()));
        }

        @Override
        public void stop() {
            super.stop();
            cooldownEndTick -= 10 * fatigueLevel;
            isAttacking = false;
            existPersistentAttack = false;
            if (!FMLEnvironment.production) {
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("持久攻击结束，动画ID：%d冷却时间为：%d", anim.anims().keySet().iterator().next(), anim.cd() - 10 * fatigueLevel)), false);
            }
        }
    }

    // 单次攻击
    class SingleAttackGoalSingle extends SingleAnimGoal<ToIntFunction<LivingEntity>, Sans> {
        List<SingleAnim<ToIntFunction<LivingEntity>>> attacks = new ArrayList<>(List.of(
                new SingleAnim<>((byte) 8, 3, 30, 40, mob::shootBoneRingVolley),
                new SingleAnim<>((byte) 9, 7, 30, 40, (target) -> mob.shootArcSweepVolley()),
                new SingleAnim<>((byte) 6, 4, 30, 60, mob::summonGBAroundSelf),
                new SingleAnim<>((byte) 6, 4, 30, 60, mob::summonGBAroundTarget),
                new SingleAnim<>((byte) 6, 4, 30, 60, mob::summonGBFront)
        ));
        List<SingleAnim<ToIntFunction<LivingEntity>>> groundAttacks = new ArrayList<>(List.of(
                new SingleAnim<>((byte) 6, 10, 20, 50, mob::summonGroundBoneSpineWaveAroundSelf)
        ));
        SingleAnim<ToIntFunction<LivingEntity>> closeGroundAttack = new SingleAnim<>((byte) 6, 4, 20, 50, (target) -> mob.summonGroundBoneSpineAtSelf());

        public SingleAttackGoalSingle() {
            super(Sans.this);
        }

        @Override
        public boolean canUse() {
            byte phaseID = getPhaseID();
            return ((phaseID == FIRST_PHASE && !isAttacking) || (phaseID == SECOND_PHASE && (!isAttacking || existPersistentAttack))) && timeJumpAttackCount == 0 && seeTime > -60 && super.canUse();
        }

        @Override
        protected @NotNull SingleAnim<ToIntFunction<LivingEntity>> select(LivingEntity target) {
            boolean onGround = target.onGround();
            boolean canFlying = target instanceof FlyingMob || target instanceof FlyingAnimal || target.hasEffect(MobEffects.LEVITATION);
            List<SingleAnim<ToIntFunction<LivingEntity>>> availableAttacks = new ArrayList<>(attacks);
            if (target.onGround()) {
                if (target.closerThan(mob, 5)) {
                    availableAttacks.add(closeGroundAttack);
                }
                availableAttacks.addAll(groundAttacks);
//                return availableAttacks.get(2);
                return closeGroundAttack;
            }
            boolean inAir = target.isFallFlying() || (!onGround && (canFlying || target.onClimbable()));
            if (inAir || getPhaseID() >= SECOND_PHASE) {
                //todo 重力控制
            }
            isAttacking = true;
            return availableAttacks.get(random.nextInt(availableAttacks.size()));
        }

        @Override
        protected int execute(LivingEntity target) {
            return anim.action().applyAsInt(target);
        }

        @Override
        public void stop() {
            super.stop();
            cooldownEndTick -= 5 * fatigueLevel;
            if (!FMLEnvironment.production) {
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("单次攻击结束，动画ID：%d,冷却时间为：%d", anim.id(), anim.cd() - 10 * fatigueLevel)), false);
            }
            isAttacking = false;
        }
    }


    // 序列连击
    class SequenceAttackGoal extends SequenceAnimGoal<Sans> {
        private final List<SequenceAnim> attacks = List.of(
                new SequenceAnim(200, List.of(
                        new AnimStep((byte) 6, 4, (target) -> mob.summonGBFront(target, 2, 30F, 51), 10),
                        new AnimStep(4, (target) -> mob.summonGBFront(target, 2, 60F, 51), 10),
                        new AnimStep(4, (target) -> mob.summonGBFront(target, 2, 90F, 51), 10),
                        new AnimStep(4, (target) -> mob.summonGBFront(target, 2, 120F, 51), 10),
                        new AnimStep(4, (target) -> mob.summonGBFront(target, 2, 150F, 51), 10)
                ))
        );
        private final List<SequenceAnim> groundAttacks = List.of(
                new SequenceAnim(200, List.of(
                        new AnimStep((byte) 7, 4, (target) -> mob.summonParallelGroundBoneSpineWaveAroundSelf(target, 3, ColorAttack.WHITE), 1),
                        new AnimStep(4, (target) -> mob.summonParallelGroundBoneSpineWaveAroundSelf(target, 2, ColorAttack.AQUA), 20),
                        new AnimStep(4, (target) -> mob.summonParallelGroundBoneSpineWaveAroundSelf(target, 4, ColorAttack.WHITE), 1),
                        new AnimStep(4, (target) -> mob.summonParallelGroundBoneSpineWaveAroundSelf(target, 3, ColorAttack.AQUA), 20),
                        new AnimStep(4, (target) -> mob.summonParallelGroundBoneSpineWaveAroundSelf(target, 5, ColorAttack.WHITE), 1),
                        new AnimStep(4, (target) -> mob.summonParallelGroundBoneSpineWaveAroundSelf(target, 4, ColorAttack.AQUA), 20)
                ))
        );

        public SequenceAttackGoal() {
            super(Sans.this);
        }

        @Override
        public boolean canUse() {
            byte phaseID = getPhaseID();
            return phaseID != MERCY_PHASE && (!isAttacking || timeJumpAttackCount > 0) && seeTime > -60 && super.canUse();
        }

        @Override
        protected @NotNull SequenceAnim select(LivingEntity target) {
            int difficulty = mob.level().getDifficulty().getId();
            if (getPhaseID() == OPENING_ATTACK) {
                return new SequenceAnim(500, List.of(
                        new AnimStep(0, mob::timeJumpTeleport, 20),
                        new AnimStep((byte) 1, 0, (t) -> mob.gravityControl(t, LocalDirection.DOWN), 20),
                        new AnimStep((byte) 4, 0, (t) -> mob.summonLateralBoneMatrix(t, -1), 120),
                        new AnimStep((byte) -1, 0, (t) -> mob.summonGBAroundTarget(t, 4, 0f), 30),
                        new AnimStep(0, (t) -> mob.summonGBAroundTarget(t, 4, 45f), 30),
                        new AnimStep(0, (t) -> mob.summonGBAroundTarget(t, 2, 0f), 30)
                ));
            }
//            if (getPhaseID() == SPECIAL_ATTACK) {
//                return new TimelineAnim(400, 400, Map.of(4, (byte) 1, 8, (byte) 4), Map.of(
//                        4, mob::timeJumpTeleport
//                ));
//            }

//            if (timeJumpAttackCount == 0 && Sans.this.random.nextDouble() < 1.0f) {
//                timeJumpAttackCount = 5;
//            }
//            if (timeJumpAttackCount > 0) {
//                timeJumpAttackCount--;
//                return new SequenceAnim(timeJumpAttackCount == 0 ? 200 : 0, timeJumpAction(target));
//            }
            List<SequenceAnim> availableAttacks = new ArrayList<>(attacks);
            availableAttacks.add(new SequenceAnim((byte) 0, 4, (t) -> mob.gravityControl(t, LocalDirection.UP),20, 100));
            if (target.onGround()) {
//                availableAttacks.addAll(groundAttacks);
                availableAttacks.addAll(List.of(
                        new SequenceAnim((byte) 6, 5, mob::summonGroundBoneWall, 6, 100),
                        SequenceAnim.create(4, 14 - difficulty, 100, (byte) 6, 0, (t) -> mob.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 1f))
                ));
                return availableAttacks.get(1);
            }
            boolean canFlying = target instanceof FlyingMob || target instanceof FlyingAnimal || target.hasEffect(MobEffects.LEVITATION);
//            boolean inAir = target.isFallFlying() || (!onGround && ( canFlying || target.onClimbable()));
//            if(inAir || mob.stamina <= maxStamina / 2 ) {
//                availableList.addAll(stateSequences.get(3));
//            }
            isAttacking = true;
//            return availableAttacks.get(random.nextInt(availableAttacks.size()));
            return availableAttacks.get(0);
        }

        @Override
        public void stop() {
            super.stop();
            this.cooldownEndTick -= 10 * fatigueLevel;
            isAttacking = false;
            byte phaseID = getPhaseID();
            if (phaseID == OPENING_ATTACK) {
                mob.entityData.set(PHASE_ID, FIRST_PHASE);
            } else if (phaseID == SPECIAL_ATTACK) {
                mob.entityData.set(PHASE_ID, END_PHASE);
            }
        }

        private List<AnimStep> timeJumpAction(LivingEntity target) {
            List<List<AnimStep>> timeJumpActions = new ArrayList<>(List.of(
                    List.of(new AnimStep(5, mob::summonGBAroundTarget, 50)),
                    List.of(new AnimStep(5, mob::summonGBFront, 50))
            ));
            if (target.onGround()) {
                timeJumpActions.addAll(List.of(
                        List.of(new AnimStep(5, (t) -> mob.summonLateralBoneMatrix(t, -1), 120)),
                        List.of(
                                new AnimStep(5, (t) -> mob.summonGroundBoneWallAroundTarget(t, ColorAttack.AQUA, 2f), 10),
                                new AnimStep(5, (t) -> mob.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 1f), 50)
                        ),
                        List.of(new AnimStep(5, (t) -> mob.summonGroundBoneMatrix(t, 1.0f), 50))
                ));
            }
            List<AnimStep> animSteps = new ArrayList<>(timeJumpActions.get(Sans.this.random.nextInt(timeJumpActions.size())));
//            List<AnimStep> animSteps = new ArrayList<>(timeJumpActions.get(2));
            animSteps.addFirst(new AnimStep(0, mob::timeJumpTeleport, 1));
            return animSteps;
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
        float speed = 1.0f + fatigueLevel * 0.3f + difficulty * 0.5f;
        int delay = 15 - fatigueLevel * 5;
        int count = 10 * (fatigueLevel + difficulty);
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("Sans疲劳程度：%d，飞行骨速度：%f，数量：%d", fatigueLevel, speed, count)), false);
        }
        // 三层高度：腿、身体、眼睛
        double[] heights = {target.getEyeY(), target.getY(0.5), target.getY(0.15),};
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            do {
                bone.setPos(this.position().add(RotUtils.getWorldPos(
                        (this.random.nextDouble() - 0.5) * (6 + 3 * (fatigueLevel + difficulty)),
                        this.random.nextDouble() * (2 + difficulty + fatigueLevel) + this.getBbHeight() * 0.5f,
                        this.random.nextDouble() * (3 + difficulty + fatigueLevel),
                        this.getXRot(), this.getYHeadRot()
                )));
            } while (this.level().noBlockCollision(bone, bone.getBoundingBox()) && !this.level().getEntities(bone, bone.getBoundingBox()).isEmpty() && ++attempts < 16);
            bone.aimShoot();
            RotUtils.lookAtShoot(bone, target.getX(), heights[this.random.nextInt(heights.length)], target.getZ());
            level().addFreshEntity(bone);
            delay += 6 - difficulty - fatigueLevel;
        }
        return delay;
    }

    /**
     * 射向前方随机骨头弹幕 - 以目标碰撞高度为随机范围高度，向前方范围随机射击
     */
    public int shootForwardBarrage(LivingEntity target) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int difficulty = this.level().getDifficulty().getId();
        float speed = 0.8f + (fatigueLevel + difficulty) * 0.1f;
        int delay = 15 - 5 * fatigueLevel;
        int count = 10 * (fatigueLevel + difficulty);
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("Sans疲劳程度：%d，飞行骨速度：%f，数量：%d", fatigueLevel, speed, count)), false);
        }
        float targetBbHeight = target.getBbHeight();
        // Sans的视线方向（固定射击方向）
        Vec3 eyeLookAngle = this.getViewVector(1.0f);
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            do {
                // 计算相对于视线方向的偏移（在局部坐标系）
                float offsetX = (float) this.random.nextGaussian() * 0.333333f * (1.0f + (fatigueLevel + difficulty) * 0.5f);  // 左右
                float offsetY = Mth.clamp(((float) this.random.nextGaussian() * 0.1666667f + 0.5f) * targetBbHeight, 0, targetBbHeight);     // 上下
                bone.setPos(this.position().add(RotUtils.getWorldPos(offsetX, offsetY, 1f, this.getXRot(), this.getYHeadRot())));
                bone.followAngleShoot(new Vec3(offsetX, offsetY, 1f));
            } while (this.level().noBlockCollision(bone, bone.getBoundingBox()) && !this.level().getEntities(bone, bone.getBoundingBox()).isEmpty() && ++attempts < 16);
            RotUtils.lookVecShoot(bone, eyeLookAngle);
            this.level().addFreshEntity(bone);
            delay += 6 - difficulty - fatigueLevel;
        }
        return delay;
    }


    /**
     * 骨环齐射 - 向目标射击
     */
    public int shootBoneRingVolley(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        double[] offsetXs = getPhaseID() == FIRST_PHASE ? new double[]{1.0} : new double[]{1.0, -1.0};
        float speed = 1.0f + (fatigueLevel + difficulty) * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay = 0;
        for (double x : offsetXs) {
            delay = 9;
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            bone.aimShoot();
            LevelUtils.addFreshProjectile(this.level(), bone, RotUtils.getWorldPos(x, 1.5f, 0, this.getXRot(), this.getYHeadRot())
                    .add(this.getX(), this.getY(0.5f), this.getZ()), target);
            for (int l = 0; l < 2; l++) {
                delay += 5 - difficulty - fatigueLevel;
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
        return delay;
    }

    /**
     * 弧形横扫齐射 - 骨头在圆弧上朝外径向发射
     */
    public int shootArcSweepVolley() {
        int difficulty = this.level().getDifficulty().getId();
        float speed = 1.0f + (difficulty + fatigueLevel) * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = (difficulty + fatigueLevel) * 5 | 1; // 或1，确保是奇数
        float interval = 10f - difficulty - fatigueLevel;
        float[] offsetAngles = getPhaseID() == 1 ? new float[]{0} : new float[]{-3, 3};
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("弧形横扫，数量：%d,角度间隔：%f", count, interval)), false);
        }
        // 圆心位置（实体位置）
        Vec3 center = new Vec3(this.getX(), this.getY(0.5f), this.getZ());
        for (float offsetAngle : offsetAngles) {
            int middleIndex = (count - 1) / 2;
            float angle = -middleIndex * interval;
            for (int i = 0; i < count; i++, angle += interval) {
                Vec3 worldOffsetPos = RotUtils.getWorldPos(Mth.sin(angle * Mth.DEG_TO_RAD), 0, 0.8f * Mth.cos(angle * Mth.DEG_TO_RAD), offsetAngle, this.getXRot(), this.getYHeadRot());
                FlyingBone bone = new FlyingBone(EntityTypeRegistry.FLYING_BONE.get(), this.level(), this, 1f, speed, worldOffsetPos);
                bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
                LevelUtils.addFreshProjectileByVec3(this.level(), bone, center.add(worldOffsetPos), worldOffsetPos);
            }
        }
        return 0;
    }

    protected FlyingBone createFlyingBone(String attackTypeUUID, float speed, int delay) {
        FlyingBone bone = new FlyingBone(EntityTypeRegistry.FLYING_BONE.get(), this.level(), this, 1f, speed, delay);
        bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
        return bone;
    }

    /**
     * 四个方向召唤骨墙围剿
     */
    public int summonGroundBoneWallAroundTarget(LivingEntity target, ColorAttack colorAttack, float height) {
        summonGroundBoneWall(target, colorAttack, height, LocalDirection.FRONT, 5, 8.0);
        summonGroundBoneWall(target, colorAttack, height, LocalDirection.BACK, 5, 8.0);
        summonGroundBoneWall(target, colorAttack, height, LocalDirection.LEFT, 5, 8.0);
        return summonGroundBoneWall(target, colorAttack, height, LocalDirection.RIGHT, 5, 8.0);
    }

    public int summonGroundBoneWall(LivingEntity target) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int difficulty = this.level().getDifficulty().getId();
        float halfX = (7 + difficulty * 4) * 0.375f;
        int delay = 1;
        for (int i = 0; i < 3; i++) {
            summonGroundBoneWall(target, ColorAttack.WHITE, 1.0f, LocalDirection.FRONT, delay, 12.0);
            delay += 7 - difficulty;
            summonGroundBoneWall(target, ColorAttack.AQUA, 2.0f, LocalDirection.FRONT, delay, 12.0);
            delay += 12;
        }
        delay += 20;
        for (int i = 0; i < 3; i++) {
            summonGroundBoneWall(target, ColorAttack.WHITE, 1.0f, LocalDirection.BACK, delay, 12.0);
            delay += 7 - difficulty;
            summonGroundBoneWall(target, ColorAttack.AQUA, 2.0f, LocalDirection.BACK, delay, 12.0);
            delay += 12;
        }
        Level level = this.level();
        float[] offsetX = new float[]{1.0f, -1.0f};
        for (float x : offsetX) {
            for (int i = -12; i < 12; i++) {
                GroundBone bone = new GroundBone(level, this, 3f, 3f, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), delay, 0, ColorAttack.AQUA, false, false);
                bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
                Vec3 pos = target.position().add(RotUtils.yRot(new Vec3(x * halfX, 0, i * 0.3125 * 3f), this.getYHeadRot()));
                bone.setPos(pos);
                bone.setYRot(this.getYHeadRot() + 90f);
                level.addFreshEntity(bone);
                level.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(this));
            }
        }
        return delay;
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
            MovingGroundBone bone = new MovingGroundBone(level, this, 1.0f, growScale, delay, speed, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), color);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
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
    public int summonGroundBoneMatrix(LivingEntity target, float growScale) {
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
                MovingGroundBone bone = new MovingGroundBone(level, this, scale, growScale * (r > rows * 0.5f ? 0.33333334f : 1f), speed, lookVector, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), ColorAttack.WHITE);
                bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
                // 设置位置和朝向
                bone.setPos(centerPos.add(RotUtils.yRot(new Vec3(xOffset + c * spacing, 0, -r * spacing), boneLookYRot)));
                RotUtils.lookVec(bone, lookVector);
                level.addFreshEntity(bone);
            }
        }
        return 0;
    }

    /**
     * 带相对前方计算的正弦波缺口骨头矩阵
     *
     * @param factor 影响矩阵难度的额外因子
     */
    public int summonLateralBoneMatrix(LivingEntity target, int factor) {
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
            float x = i * Mth.sin(t * Mth.PI * (6 + factor + fatigueLevel) * 0.25f) * width * (2 + difficulty + fatigueLevel + factor) * 0.1f - (i * width * 0.1f * (float) this.random.nextGaussian() * 0.033333f + 0.1f);
            float leftWidth = (width + x) - gap * 0.5f;
            float rightWidth = (width - x) - gap * 0.5f;
            for (int c = 0; c < cols; c++) {
                // 创建左侧骨头
                LateralBone leftBone = new LateralBone(level, this, 1.0f, leftWidth, speed, calculateViewVector(0, yRot), (float) getAttributeValue(Attributes.ATTACK_DAMAGE), ColorAttack.WHITE);
                leftBone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(id, (byte) 6));
                leftBone.setPos(RotUtils.yRot(new Vec3(-width + leftWidth * 0.5f, c * spacing, -r * spacing), yRot).add(this.position()));
                level.addFreshEntity(leftBone);
                // 创建右侧骨头
                LateralBone rightBone = new LateralBone(level, this, 1.0f, rightWidth, speed, calculateViewVector(0, yRot), (float) getAttributeValue(Attributes.ATTACK_DAMAGE), ColorAttack.WHITE);
                rightBone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(id, (byte) 6));
                rightBone.setPos(RotUtils.yRot(new Vec3(width - rightWidth * 0.5f, c * spacing, -r * spacing), yRot).add(this.position()));
                level.addFreshEntity(rightBone);
            }
        }
        return 0;
    }


    /**
     * 召唤平行骨刺波动 - 等间距平行直线波动
     */
    public int summonParallelGroundBoneSpineWaveAroundSelf(@NotNull LivingEntity target, int waveCount, ColorAttack color) {
        int difficulty = this.level().getDifficulty().getId();
        int baseDelay = 25 - difficulty * 5;
        Vec3 direction = calculateViewVector(0, this.getYHeadRot());
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x); // 垂直方向
        int rows = ATTACK_RANGE * 2 + difficulty / 3 * 5;
        int cols = 3 + difficulty;
        float waveSpacing = 2.0f; // 波动之间距离2格
        // 计算总宽度和起始偏移
        float totalWidth = (waveCount - 1) * waveSpacing;
        float startOffset = -totalWidth / 2; // 从中间开始向两边分布
        int finalDelay = baseDelay;
        for (int i = 0; i < waveCount; i++) {
            finalDelay = Math.max(finalDelay, summonGroundBoneSpineWave(target, rows, cols, this.position().add(perpendicular.scale(startOffset + i * waveSpacing)), direction, color, baseDelay));
        }
        return finalDelay;
    }


    /**
     * 骨刺波动 - 以自身为中心发射，单击
     */
    public int summonGroundBoneSpineWaveAroundSelf(@NotNull LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        int count = 3 + 2 * fatigueLevel;
        float interval = 27f - (difficulty + fatigueLevel) * 3f;
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("骨刺波动，数量：%d,角度间隔：%f", count, interval)), false);
        }
        Vec3 horLookAngle = calculateViewVector(0, getYHeadRot());
        int rows = ATTACK_RANGE * 2 + (difficulty + fatigueLevel) * 2;
        int cols = 2 + difficulty + fatigueLevel;
        int delay = 10;
        // 计算起始角度（让波动对称分布）
        float angle = interval * (1 - count) * 0.5f;
        for (int i = 0; i < count; i++, angle += interval) {
            delay = summonGroundBoneSpineWave(target, rows, cols, this.position(), horLookAngle.yRot(angle * Mth.DEG_TO_RAD), ColorAttack.WHITE, 10);
        }
        return delay;
    }

    /**
     * 公用方法：召唤定向骨刺波动
     */
    public int summonGroundBoneSpineWave(LivingEntity target, int rows, int cols, Vec3 startPos, Vec3 direction, ColorAttack color, int delay) {
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        float colSpacing = 0.375f;
        float rowSpacing = 0.6f;
        // 计算垂直方向
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x);
        // 生成 rows×cols 骨刺矩阵
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // 计算当前位置
                double xOffset = (col - (cols - 1) * 0.5) * colSpacing;
                double zOffset = row * rowSpacing;
                Vec3 bonePos = startPos.add(perpendicular.scale(xOffset)).add(direction.scale(zOffset));
                createGroundBone(attackTypeUUID, bonePos.x, bonePos.z, minY, maxY, 1f, 1f,
                        20, delay, color, false);
            }
            // 每3行增加1tick延迟，行越少，波浪越窄
            delay += (row % 3 == 0 ? 1 : 0);
        }
        return delay;
    }


    /**
     * 在自身位置召唤地面骨刺扩张（圆形）
     */
    public int summonGroundBoneSpineAtSelf() {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 pos = this.position();
        String attackTypeUUID = UUID.randomUUID().toString();
        int layer = 6 + 2 * (fatigueLevel + difficulty);
        int delay = 13 - fatigueLevel - difficulty;
        float spacing = 0.7f;
        float growScale = 1.0f;
        double groundY = createGroundBone(attackTypeUUID, pos.x, pos.z, pos.y, pos.y, 1.0f, growScale, 20, delay, ColorAttack.WHITE, true);
        this.level().playSound(null, pos.x, pos.y, pos.z, SoundRegistry.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket((float) pos.x, (float) groundY, (float) pos.z, layer * spacing, growScale, 20, Color.RED.getRGB()));
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
        return delay;
    }

    /**
     * 在目标脚下召唤地面骨刺
     */
    public int summonGroundBoneSpineAtTarget(@NotNull LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 pos = target.position();
        return summonCircleGroundBoneSpine(target, 3 * difficulty, pos.x, pos.z, difficulty * 0.3333334f, 10, 10);
    }


    /**
     * 以目标位置为中心的圆形骨刺
     *
     * @param target 目标
     * @param layer  圆环层数
     * @param x      中心坐标x
     * @param z      中心坐标z
     */
    public int summonCircleGroundBoneSpine(LivingEntity target, int layer, double x, double z, float growScale, int lifetime, int delay) {
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("地面骨刺，层数：%d：延迟：%d，高度缩放：%f", layer, delay, growScale)), false);
        }
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        float spacing = 0.7f;
        double groundY = createGroundBone(attackTypeUUID, x, z, minY, maxY, 1.0f, growScale, lifetime, delay, ColorAttack.WHITE, true);
        this.level().playSound(null, x, getY(), z, SoundRegistry.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
        PacketDistributor.sendToPlayersTrackingEntity(this, new WarningTipPacket((float) x, (float) groundY, (float) z, layer * spacing, growScale, lifetime, Color.RED.getRGB()));
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = spacing * (i + 1);
            float angle = interval; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                createGroundBone(attackTypeUUID, x + r * Mth.cos(angle * Mth.DEG_TO_RAD), z + r * Mth.sin(angle * Mth.DEG_TO_RAD),
                        minY, maxY, 1.0f, growScale, lifetime, delay, ColorAttack.WHITE, false);
            }
        }
        return delay;
    }


    /**
     * 创建应用曲线的地面骨
     *
     * @param scale     地面骨缩放大小
     * @param growScale 地面骨生长缩放大小
     */
    protected double createGroundBone(String attackUUID, double targetX, double targetZ, double minY, double maxY, float scale, float growScale, int lifetime, int delay, ColorAttack colorAttack, boolean isPlaySound) {
        Level level = this.level();
        double spawnY = EntityUtils.findGroundY(level, targetX, targetZ, minY, maxY);
        // 如果找到有效地面，生成骨刺
        if (spawnY != level.getMinBuildHeight()) {
            GroundBone bone = new GroundBone(level, this, scale, growScale, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), lifetime, delay, colorAttack, isPlaySound, true);
            bone.setPos(targetX, spawnY, targetZ);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackUUID, (byte) 6));
            // 设置旋转：骨刺指向圆心（目标位置）
            bone.setYRot(this.getYHeadRot());
            // 添加随机X旋转，看起来不规则
            bone.setXRot((float) this.random.nextGaussian() * 10);
            level.addFreshEntity(bone);
            level.gameEvent(GameEvent.ENTITY_PLACE, new Vec3(targetX, spawnY, targetZ), GameEvent.Context.of(this));
        }
        return spawnY;
    }


    /**
     * 用于单击
     */
    public int summonGBAroundSelf(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("GB单击，数量：%d,大小：%f", 1 + fatigueLevel + difficulty / 3, 1.0f + difficulty * 0.25f)), false);
        }
        return summonGBAroundSelf(target, 1 + fatigueLevel + difficulty / 3, 1.0f + difficulty * 0.25f);
    }

    /**
     * 在自身周围随机位置召唤GB
     */
    public int summonGBAroundSelf(LivingEntity target, int count, float size) {
        for (int i = 0; i < count; i++) {
            int attempts = 0;
            GasterBlaster gb = createGasterBlaster(size, 17, 28);
            do {
                gb.setPos(this.getEyePosition().add(RotUtils.getWorldPos(
                        this.random.nextDouble() * 16 - 8,
                        this.random.nextDouble() * 3 + 1,
                        this.random.nextDouble() * 4,
                        this.getXRot(), this.getYHeadRot()
                )));
                gb.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            } while (this.level().noBlockCollision(gb, gb.getBoundingBox()) && !this.level().getEntities(gb, gb.getBoundingBox()).isEmpty() && ++attempts < 16);
            this.level().addFreshEntity(gb);
        }
        return 0;
    }

    /**
     * 用于单击，环绕目标周围
     */
    public int summonGBAroundTarget(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        // 简单2~4 普通2~5 困难2~6
        int count = 2 + this.random.nextInt(1 + difficulty + fatigueLevel);
        return summonGBAroundTarget(target, count, this.random.nextInt(count) * 180f / count);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB，固定360度范围，根据数量自动计算角度步长和大小
     */
    public int summonGBAroundTarget(LivingEntity target, int count, float offsetAngle) {
        int difficulty = this.level().getDifficulty().getId();
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("GB单击，数量：%d,大小：%f", count, 1.0f + (1 + difficulty + fatigueLevel - count) * 0.25f)), false);
        }
        return summonGBAroundTarget(target, count, offsetAngle, 360f / count, 1.0f + (1 + difficulty + fatigueLevel - count) * 0.25f, 17, 28);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB，在自身前方对称召唤GB，用于单击
     */
    public int summonGBFront(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        // 简单1~3 普通1~4 困难1~5
        int count = 1 + this.random.nextInt(1 + difficulty + fatigueLevel);
        float angleStep = 90f / count;
        return summonGBFront(target, count, angleStep, 17);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB，在自身前方对称召唤GB，用于自定义的序列攻击，
     */
    public int summonGBFront(LivingEntity target, int count, float angleStep, int charge) {
        int difficulty = this.level().getDifficulty().getId();
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("GB单击，数量：%d,大小：%f,偏转角度：%f，角度步长：%f", count, 1.0f + (1 + difficulty + fatigueLevel - count) * 0.25f, -(count - 1) * angleStep * 0.5f, angleStep)), false);
        }
        return summonGBAroundTarget(target, count, (1 - count) * angleStep * 0.5f, angleStep, 1.0f + (1 + difficulty + fatigueLevel - count) * 0.25f, charge, 28);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB
     *
     * @param target      目标实体
     * @param count       GB数量
     * @param offsetAngle 初始偏移角度（度）
     * @param angleStep   角度步长（度）
     */
    public int summonGBAroundTarget(LivingEntity target, int count, float offsetAngle, float angleStep, float size, int charge, int shot) {
        Vec3 targetPos = new Vec3(target.getX(), target.getY(0.5f), target.getZ());
        double radius = this.distanceTo(target) * 0.75f + (this.random.nextDouble() * 2.0 - 1.0);
        float currentAngle = offsetAngle; // 从指定角度开始
        float height = this.random.nextFloat() * 3f;
        for (int i = 0; i < count; i++, currentAngle += angleStep) {
            GasterBlaster gb = createGasterBlaster(size, charge, shot);
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = -Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            LevelUtils.addFreshEntity(this.level(), gb, RotUtils.getWorldPos(xOffset, height, zOffset, this.getXRot(), this.getYHeadRot())
                    .add(targetPos), targetPos);
        }
        return 0;
    }


    public GasterBlaster createGasterBlaster(float size, int charge, int shot) {
        GasterBlaster gb = new GasterBlaster(this.level(), this, size, charge, shot);
        gb.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(UUID.randomUUID().toString(), (byte) 10));
        return gb;
    }

    public int gravityControl(LivingEntity target, LocalDirection direction) {
        GravityData gravityData = GravityData.applyRelativeGravity(this, target, direction);
        PacketDistributor.sendToPlayersTrackingEntity(this, new GravityPacket(this.getId(),target.getId(),gravityData.getGravity(),target.getDeltaMovement()));
        this.isAppendSpine = true;
        return 0;
    }

    /**
     * 时间跳跃传送
     */
    public int timeJumpTeleport(LivingEntity target) {
        PacketDistributor.sendToPlayersTrackingEntity(this, new TimeJumpTeleportPacket(target.getId(), 5));
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.SANS_TELEPORT_TIME_JUMP.get(), SoundSource.HOSTILE);
        Vec3 pos = this.position().add(RotUtils.getWorldPos(0f, 0f, 12f, 0, this.getYHeadRot()));
        target.teleportTo(pos.x, pos.y, pos.z);
        return 0;
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
    private static final RawAnimation[] THROW_ANIMATIONS = new RawAnimation[]{
            RawAnimation.begin().thenPlay("attack.throw.up"),
            RawAnimation.begin().thenPlay("attack.throw.down"),
            RawAnimation.begin().thenPlay("attack.throw.left"),
            RawAnimation.begin().thenPlay("attack.throw.right"),
            RawAnimation.begin().thenPlay("attack.throw.front"),
            RawAnimation.begin().thenPlay("attack.throw.back")
    };

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Sans> attackController = new AnimationController<>(this, "attack", state -> {
            AnimationController<Sans> controller = state.getController();
            if (animId == -1) {
                controller.forceAnimationReset();
                return PlayState.STOP;
            }
            byte phaseID = getPhaseID();
            switch (animId) {
                case 0, 1, 2, 3, 4, 5 -> controller.setAnimation(THROW_ANIMATIONS[animId]);
                case 6 -> this.handleCastSpell(state, phaseID == FIRST_PHASE, ANIM_CAST_LEFT, ANIM_CAST);
                case 7 -> this.handleCastSpell(state, phaseID == FIRST_PHASE, ANIM_CAST_CIRCLE_LEFT, ANIM_CAST_CIRCLE);
                case 8 ->
                        controller.setAnimation(phaseID == FIRST_PHASE ? ANIM_BONE_PROJECTILE_LEFT : ANIM_BONE_PROJECTILE);
                case 9 -> controller.setAnimation(phaseID == FIRST_PHASE ? ANIM_BONE_SWEEP_LEFT : ANIM_BONE_SWEEP);
            }
            controller.setAnimationSpeed(animSpeed);
            return PlayState.CONTINUE;
        });

        controllers.add(
//                DefaultAnimations.genericWalkIdleController(this),
                DefaultAnimations.genericLivingController(this),
                attackController
        );
    }

    private void handleCastSpell(AnimationState<Sans> state, boolean isFirstPhase, RawAnimation anim1, RawAnimation anim2) {
        if (isFirstPhase) {
            state.getController().setAnimation(anim1);
            if (state.getAnimationTick() % 0.05 < 0.001) {
                BoneAnimationQueue leftHand = state.getController().getBoneAnimationQueues().get("left_hand");
                if (leftHand != null) {
                    Vector3d worldPosition = leftHand.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                }
            }
        } else {
            state.getController().setAnimation(anim2);
            if (state.getAnimationTick() % 0.05 < 0.001) {
                BoneAnimationQueue leftHand = state.getController().getBoneAnimationQueues().get("left_hand");
                if (leftHand != null) {
                    Vector3d worldPosition = leftHand.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                }
                BoneAnimationQueue rightHand = state.getController().getBoneAnimationQueues().get("right_hand");
                if (rightHand != null) {
                    Vector3d worldPosition = rightHand.bone().getWorldPosition();
                    level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.8627f, 0.8627f, 0.8627f), worldPosition.x, worldPosition.y, worldPosition.z, 0, 0, 0);
                }
            }
        }
    }

}

