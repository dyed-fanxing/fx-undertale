package com.sakpeipei.undertale.entity.boss;

import com.sakpeipei.undertale.common.anim.RoundSequenceAnim;
import com.sakpeipei.undertale.entity.DelayAction;
import com.sakpeipei.undertale.entity.DelayEntity;
import com.sakpeipei.undertale.common.LocalDirection;
import com.sakpeipei.undertale.common.LocalVec3;
import com.sakpeipei.undertale.common.anim.SequenceAnim;
import com.sakpeipei.undertale.common.anim.SingleAnim;
import com.sakpeipei.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.entity.ai.goal.NeutralMobAngerTargetGoal;
import com.sakpeipei.undertale.entity.ai.goal.RoundSequenceAnimGoal;
import com.sakpeipei.undertale.entity.ai.goal.SequenceAnimGoal;
import com.sakpeipei.undertale.entity.ai.goal.SingleAnimGoal;
import com.sakpeipei.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.undertale.entity.projectile.GroundBoneProjectile;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GroundBone;
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
import net.minecraft.nbt.*;
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
import net.minecraft.world.Difficulty;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

public class Sans extends Monster implements NeutralMob, GeoEntity, IAnimatable, IEntityWithComplexSpawn {
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

    private static final EntityDataAccessor<Byte> PHASE_ID = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> IS_EYE_BLINK = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STAMINA = SynchedEntityData.defineId(Sans.class, EntityDataSerializers.INT);
    private int maxStamina;  // 最大体力/耐力
    private int fatigueLevel;// 疲劳等级，出汗等级


    boolean isAppendSpine; // 是否追加骨刺，主要用于重力控制后，落地触发



    private byte animId = -1;
    private float animSpeed;


    private final List<DelayEntity> delayEntities = new ArrayList<>();
    private final List<DelayAction<IntSupplier>> delayActions = new ArrayList<>();

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
//        this.goalSelector.addGoal(1, new SequenceAttackGoal());
        this.goalSelector.addGoal(2, new PersistentAttackGoal());
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
        delayEntities.removeIf(delayEntity -> delayEntity.tick(tickCount));
        delayActions.removeIf(delayAction -> {
            if(delayAction.delay() == tickCount){
                IntSupplier action = delayAction.action();
                action.getAsInt();
                return true;
            }
            return false;
        });
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

        fatigueLevel = 1 - Mth.floor((float) (stamina - 1)/maxStamina * 2);
        LivingEntity target = this.getTarget();
        if (target != null && target.getDeltaMovement() instanceof LocalVec3 && target.onGround() && isAppendSpine) {
            this.summonGroundBoneSpineAtTarget(target);
            isAppendSpine = false;
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
        if(stamina == 0){
            return super.hurt(source, power);
        }
        if (!source.is(Tags.DamageTypes.IS_TECHNICAL)) {
            if ((sourceEntity == null && directEntity == null) || getPhaseID() == SPECIAL_ATTACK) { // 免疫一切伤害 执行特殊攻击
                return false;
            }
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
        }
        stamina = Math.max(0,stamina - Mth.ceil(power));
        this.setStamina(stamina);
        if(stamina <= maxStamina/2){
            this.entityData.set(PHASE_ID,SECOND_PHASE);
        }

        if (sourceEntity == null) {
            rangedTeleport(directEntity);
        }else{
            if (sourceEntity instanceof LivingEntity livingEntity) {
                this.setLastHurtByMob(livingEntity);
            } else if (sourceEntity instanceof Player player) {
                this.setLastHurtByPlayer(player);
            }
            // todo 根据目标实体的攻击范围决定是远程攻击还是近战攻击
            if(sourceEntity instanceof Mob mob){
            }
            if(sourceEntity instanceof Player player){
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
        this.entityData.set(STAMINA,stamina);
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
        tag.putInt("Stamina", this.getStamina());
        tag.putInt("MaxStamina", this.maxStamina);
        ListTag list = new ListTag();
        delayEntities.forEach(delayEntity -> {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putInt("DelayTick",delayEntity.delayTick());
            compoundTag.putUUID("UUID",delayEntity.entity().getUUID());
            list.add(compoundTag);
        });
        tag.put("DelayEntityUUIDs",  list);
        this.addPersistentAngerSaveData(tag);
    }
    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Stamina")) {
            setStamina(tag.getInt("Stamina"));
        }
        if (tag.contains("MaxStamina")) {
            this.maxStamina = tag.getInt("MaxStamina");
        }
        if(tag.contains("DelayEntityUUIDs")) {
            ListTag list = tag.getList("DelayEntityUUIDs",Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag compound = list.getCompound(i);
                this.delayEntities.add(new DelayEntity(((ServerLevel)this.level()).getEntity(compound.getUUID("UUID")),compound.getInt("DelayTick")));
            }
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

    //持续攻击，可脱手
    class PersistentAttackGoal extends SequenceAnimGoal<Integer, Sans> {
        List<SequenceAnim<Integer>> attacks = new ArrayList<>(List.of(
                new SequenceAnim<>((byte) 6,4,30,150,1),
                new SequenceAnim<>((byte) 6,0,30,150,2),
                SequenceAnim.onceAnim(13,100,new SingleAnim<>((byte) 7, 4,30,20, 3))
        ));
        List<SequenceAnim<Integer>> groundAttacks = new ArrayList<>(List.of(
                SequenceAnim.onceAnim(8, 100,new SingleAnim<>((byte) 7, 4,40,20, 4))
        ));
//        HashMap<Integer, List<SequenceAnim<Integer>>> attackMap = new HashMap<>();
//        HashMap<Integer, List<SequenceAnim<Integer>>> groundAttackMap = new HashMap<>();
        public PersistentAttackGoal() {
            super(Sans.this);
//            for (int i = 0; i <= Difficulty.HARD.getId(); i++) {
//                attackMap.put(i,List.of(
//                        new SequenceAnim<>(3 + 4 * i, 100,new SingleAnim<>((byte) 7, 4,30,20, 3))
//                ));
//                groundAttackMap.put(i,List.of(
//                        new SequenceAnim<>(2 + 3 * i, 100,new SingleAnim<>((byte) 7, 4,40,20, 4))
//                ));
//            }
        }
        @Override
        public boolean canUse() {
            return super.canUse() && seeTime > -60 && !isAttacking;
        }
        @Override
        protected @NotNull SequenceAnim<Integer> select(LivingEntity target) {
            List<SequenceAnim<Integer>> availableAttacks = new ArrayList<>(attacks);
//            if (target.onGround()) {
//                availableAttacks.addAll(groundAttacks);
//                return availableAttacks.get(3);
//            }
            existPersistentAttack = true;
            isAttacking = true;
//            return availableAttacks.get(Sans.this.random.nextInt(availableAttacks.size()));
            return availableAttacks.get(0);
        }

        @Override
        protected int execute(LivingEntity target, SingleAnim<Integer> anim) {
            int difficulty = Sans.this.level().getDifficulty().getId();
            Integer action = anim.action();
            return switch (action) {
                case 1 -> Sans.this.shootAimedBarrage(target);
                case 2 -> Sans.this.shootForwardBarrage(target);
                case 3 -> Sans.this.summonGBAroundSelf(target, 1 ,1.0f + fatigueLevel*(0.25f*difficulty));
                case 4 -> Sans.this.summonGroundBoneSpineAroundTarget(target);
                default -> throw new IllegalStateException("Unexpected value: " + action);
            };
        }

        @Override
        public void stop() {
            super.stop();
            cooldownEndTick -= 10*fatigueLevel;
            isAttacking = false;
            existPersistentAttack = false;
            if (!FMLEnvironment.production) {
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("动画结束,CD：%d",anim.cd() - 10 * fatigueLevel)),false);
            }
        }

        @Override
        protected void stepStop(SingleAnim<Integer> curr) {
            super.stepStop(curr);
            cooldownEndTick -= 5*fatigueLevel;
        }
    }

    // 单次攻击
    class SingleAttackGoalSingle extends SingleAnimGoal<ToIntFunction<LivingEntity>, Sans> {
        List<SingleAnim<ToIntFunction<LivingEntity>>> attacks = new ArrayList<>(List.of(
                new SingleAnim<>((byte) 8,3,30,40, Sans.this::shootBoneRingVolley),
                new SingleAnim<>((byte) 9,4,30,40, (target) -> Sans.this.shootArcSweepVolley()),
                new SingleAnim<>((byte) 6,4,30,40, Sans.this::summonGBAroundSelf),
                new SingleAnim<>((byte) 6,4,30,40, Sans.this::summonGBAroundTarget),
                new SingleAnim<>((byte) 6,4,30,40, Sans.this::summonGBFront)
        ));
        List<SingleAnim<ToIntFunction<LivingEntity>>> groundAttacks = new ArrayList<>(List.of(
                new SingleAnim<>((byte) 6, 4 ,20, 50, (target) -> Sans.this.summonGroundBoneSpineAtSelf()),
                new SingleAnim<>((byte) 6, 10,20, 50, (target) -> Sans.this.summonGroundBoneSpineWaveAroundSelf(target, 30f, ColorAttack.WHITE)),
                new SingleAnim<>((byte) 6, 10,20, 50, (target) -> Sans.this.summonGroundBoneSpineWaveAroundSelf(target, ColorAttack.WHITE))
        ));
        public SingleAttackGoalSingle() {
            super(Sans.this);
        }
        @Override
        public boolean canUse() {
            byte phaseID = getPhaseID();
            return super.canUse() && seeTime > -60 && !isAttacking && fatigueLevel < 3 && ((phaseID == FIRST_PHASE && !existPersistentAttack) || phaseID == SECOND_PHASE) ;
        }

        @Override
        protected @NotNull SingleAnim<ToIntFunction<LivingEntity>> select(LivingEntity target) {
            boolean onGround = target.onGround();
            boolean canFlying = target instanceof FlyingMob || target instanceof FlyingAnimal || target.hasEffect(MobEffects.LEVITATION);
            List<SingleAnim<ToIntFunction<LivingEntity>>> availableAttacks = new ArrayList<>(attacks);
            if (target.onGround()) {
                availableAttacks.addAll(groundAttacks);
                return availableAttacks.get(3);
            }
            boolean inAir = target.isFallFlying() || (!onGround && (canFlying || target.onClimbable()));
            if(inAir || getPhaseID() >= SECOND_PHASE) {
                //todo 重力控制
            }
            isAttacking = true;
//            return availableAttacks.get(random.nextInt(availableAttacks.size()));
            return availableAttacks.get(2);
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
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("动画结束,动画ID：%d，CD：%d",anim.id(),anim.cd() - 5 * fatigueLevel)),false);
            }
            isAttacking = false;
        }
    }

    // 序列连击
    class SequenceAttackGoal extends SequenceAnimGoal<ToIntFunction<LivingEntity>, Sans> {
        private final List<SequenceAnim<ToIntFunction<LivingEntity>>> attacks = List.of(
                new SequenceAnim<>(200, List.of(
                        new SingleAnim<>((byte) 8 , 4,20,40,(target)-> Sans.this.summonGBAroundTarget(target,2,18.0)),
                        new SingleAnim<>((byte) -1 , 4,20,40,(target)-> Sans.this.summonGBAroundTarget(target,2,36.0)),
                        new SingleAnim<>((byte) -1 , 4,20,40,(target)-> Sans.this.summonGBAroundTarget(target,2,54.0)),
                        new SingleAnim<>((byte) -1 , 4,20,40,(target)-> Sans.this.summonGBAroundTarget(target,2,72.0)),
                        new SingleAnim<>((byte) -1 , 4,20,40,(target)-> Sans.this.summonGBAroundTarget(target,2,90.0))
                ))
        );
        List<SequenceAnim<ToIntFunction<LivingEntity>>> groundAttacks = List.of(
                new SequenceAnim<>(200, List.of(
                        new SingleAnim<>((byte) 3,4,30,40, (target)-> Sans.this.summonParallelGroundBoneSpineWaveAroundSelf(target,1,ColorAttack.WHITE)),
                        new SingleAnim<>((byte) 3,4,30,40, (target)-> Sans.this.summonParallelGroundBoneSpineWaveAroundSelf(target,2,ColorAttack.AQUA)),
                        new SingleAnim<>((byte) 3,4,30,40, (target)-> Sans.this.summonParallelGroundBoneSpineWaveAroundSelf(target,3,ColorAttack.WHITE)),
                        new SingleAnim<>((byte) 3,4,30,40, (target)-> Sans.this.summonParallelGroundBoneSpineWaveAroundSelf(target,4,ColorAttack.AQUA)),
                        new SingleAnim<>((byte) 3,4,30,40, (target)-> Sans.this.summonParallelGroundBoneSpineWaveAroundSelf(target,4,ColorAttack.WHITE)),
                        new SingleAnim<>((byte) 3,4,30,40, (target)-> Sans.this.summonParallelGroundBoneSpineWaveAroundSelf(target,5,ColorAttack.AQUA))
                ))
        );
        Map<Integer, List<SequenceAnim<ToIntFunction<LivingEntity>>>> attackMap = new HashMap<>();
        Map<Integer, List<SequenceAnim<ToIntFunction<LivingEntity>>>> groundAttackMap = new HashMap<>();
        public SequenceAttackGoal() {
            super(Sans.this);
            for (int i = 0; i <= Difficulty.HARD.getId(); i++) {
                attackMap.put(i,List.of(

                ));
                groundAttackMap.put(i,List.of(
                        new SequenceAnim<>(3 + 2 * i,100, List.of(
                                new SingleAnim<>((byte)  6, 4,50,20,(target) -> Sans.this.summonGroundBoneWall(target,ColorAttack.WHITE, 1f, LocalDirection.FRONT)),
                                new SingleAnim<>((byte) -1, 4,50,20,(target) -> Sans.this.summonGroundBoneWall(target,ColorAttack.AQUA, 1f, LocalDirection.FRONT))
                        ))
                ));
            }
        }
        @Override
        protected @NotNull SequenceAnim<ToIntFunction<LivingEntity>> select(LivingEntity target) {
            int difficulty = Sans.this.level().getDifficulty().getId();
            if(getPhaseID() == OPENING_ATTACK) {
                Sans.this.entityData.set(PHASE_ID,FIRST_PHASE);
                return new SequenceAnim<>(400, List.of(
                        new SingleAnim<>((byte) -1, 0,20,20, Sans.this::timeJumpTeleport),    // 黑屏传送
                        new SingleAnim<>((byte) 1 , 1,20,20,(t) -> Sans.this.gravityControl(t,LocalDirection.DOWN)),          // 重力下砸
                        new SingleAnim<>((byte) 4 , 4,40,20,(t) -> Sans.this.summonGroundBoneMatrix(t,0)),            // 向前召唤骨头矩阵
                        new SingleAnim<>((byte) -1, 4,40,20,(t) -> Sans.this.summonGBAroundTarget(t,4,0f)),   // 十字gb炮
                        new SingleAnim<>((byte) -1, 4,40,20,(t) -> Sans.this.summonGBAroundTarget(t,4,45f)),  // 交叉gb炮
                        new SingleAnim<>((byte) -1, 4,40,20,(t) -> Sans.this.summonGBAroundTarget(t,2,0f))   // 左右变大gb炮
                ));
            }
            if(getPhaseID() == SPECIAL_ATTACK){
                return new SequenceAnim<>(400, List.of(
                        new SingleAnim<>((byte) -1, 0,20,20, Sans.this::timeJumpTeleport),    // 黑屏传送
                        new SingleAnim<>((byte) 1 , 1,20,20,(t) -> Sans.this.gravityControl(t,LocalDirection.DOWN)),          // 重力下砸
                        new SingleAnim<>((byte) 4 , 4,40,20,(t) -> Sans.this.summonGroundBoneMatrix(t,0)),            // 向前召唤骨头矩阵
                        new SingleAnim<>((byte) -1, 4,40,20,(t) -> Sans.this.summonGBAroundTarget(t,4,0f)),   // 十字gb炮
                        new SingleAnim<>((byte) -1, 4,40,20,(t) -> Sans.this.summonGBAroundTarget(t,4,45f)),  // 交叉gb炮
                        new SingleAnim<>((byte) -1, 4,40,20,(t) -> Sans.this.summonGBAroundTarget(t,2,0f))   // 左右变大gb炮
                ));
            }

            List<SequenceAnim<ToIntFunction<LivingEntity>>> availableAttacks = new ArrayList<>(attacks);
            if (target.onGround()) {
                availableAttacks.addAll(groundAttacks);
                availableAttacks.addAll(groundAttackMap.get(difficulty));
            }
            boolean canFlying = target instanceof FlyingMob || target instanceof FlyingAnimal || target.hasEffect(MobEffects.LEVITATION);
//            boolean inAir = target.isFallFlying() || (!onGround && ( canFlying || target.onClimbable()));
//            if(inAir || Sans.this.stamina <= maxStamina / 2 ) {
//                availableList.addAll(stateSequences.get(3));
//            }
            isAttacking = true;
            return availableAttacks.get(random.nextInt(availableAttacks.size()));
        }


        @Override
        protected int execute(LivingEntity target, SingleAnim<ToIntFunction<LivingEntity>> anim) {
            return anim.action().applyAsInt(target);
        }

        @Override
        public void stop() {
            super.stop();
            this.cooldownEndTick -= 10*fatigueLevel;
            isAttacking = false;
        }

        @Override
        protected void stepStop(SingleAnim<ToIntFunction<LivingEntity>> curr) {
            super.stepStop(curr);
            this.cooldownEndTick -= 5*fatigueLevel;

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
        float speed = 1.0f + fatigueLevel*0.3f + difficulty *0.5f;
        int delay = 15 - fatigueLevel*5;
        int count = 10*(fatigueLevel + difficulty);
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("Sans疲劳程度：%d，飞行骨速度：%f，数量：%d",fatigueLevel, speed,count)),false);
        }
        // 三层高度：腿、身体、眼睛
        double[] heights = { target.getEyeY(),target.getY(0.5),target.getY(0.15),};
        for (int i = 0; i < count; i++) {
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            do {
                LevelUtils.addFreshProjectile(this.level(), bone, this.position().add(RotUtils.getWorldPos(
                        (this.random.nextDouble() - 0.5) * (6 + 3*(fatigueLevel+difficulty)),
                        this.random.nextDouble() * (2 + difficulty + fatigueLevel) + this.getBbHeight() * 0.5f,
                        this.random.nextDouble() * (3 + difficulty + fatigueLevel),
                        this.getYHeadRot(),this.getXRot()
                )), target.getX(), heights[this.random.nextInt(heights.length)], target.getZ());
            } while (!bone.level().noCollision(bone, bone.getBoundingBox()));
            bone.aimShoot();
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
        float speed = 0.8f + ( fatigueLevel + difficulty )*0.1f;
        int delay = 15 - 5* fatigueLevel;
        int count = 10 * (fatigueLevel + difficulty);
        if (!FMLEnvironment.production) {
            Objects.requireNonNull(this.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("Sans疲劳程度：%d，飞行骨速度：%f，数量：%d",fatigueLevel, speed,count)),false);
        }
        float targetBbHeight = target.getBbHeight();
        // Sans的视线方向（固定射击方向）
        Vec3 eyeLookAngle = this.getViewVector(1.0f);
        for (int i = 0; i < count; i++) {
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            do {
                // 计算相对于视线方向的偏移（在局部坐标系）
                float offsetX = (float) this.random.nextGaussian()*0.333333f * (1.0f + (fatigueLevel + difficulty) * 0.5f);  // 左右
                float offsetY = Mth.clamp(((float)this.random.nextGaussian()*0.1666667f + 0.5f) *targetBbHeight,0,targetBbHeight);     // 上下
                LevelUtils.addFreshProjectileByVec3(this.level(), bone, this.position().add(RotUtils.getWorldPos(offsetX, offsetY, 1f, this.getYHeadRot(), this.getXRot())), eyeLookAngle);
                bone.followAngleShoot(new Vec3(offsetX,offsetY,1f));
            } while (!bone.level().noCollision(bone, bone.getBoundingBox()));
            delay +=  6 - difficulty - fatigueLevel;
        }
        return delay;
    }


    /**
     * 骨环齐射 - 向目标射击
     */
    public int shootBoneRingVolley(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        double[] offsetXs = getPhaseID() == FIRST_PHASE ? new double[]{1.0} : new double[]{1.0, -1.0};
        float speed = 1.0f + (fatigueLevel + difficulty) * 0.2f ;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay = 9;
        int layer = 1 + fatigueLevel;
        FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
        bone.aimShoot();
        LevelUtils.addFreshProjectile(this.level(), bone, this.position().add(0,getY(0.5f),0), target);
        for (double x : offsetXs) {
            for (int l=0;l<layer;l++) {
                // 半径公式： 数量 * (骨头宽度 + 间隔 ) / 2PI = count * (0.3 + 0.1) / 2*Mth.PI
                float count = 3 + (fatigueLevel + difficulty + l) * 3;
                float radius = count * 0.2f / Mth.PI;
                float interval = 360f / count;
                float angle = interval/2; //起始位置偏移，每层错位分布
                for (int i = 0; i < count; i++,angle+=interval) {
                    bone = createFlyingBone(attackTypeUUID, speed, delay);
                    bone.aimShoot();
//                    Vec3 pos = new Vec3(
//                            x + radius * Mth.cos(angle * Mth.DEG_TO_RAD),
//                            1.5f + radius * Mth.sin(angle * Mth.DEG_TO_RAD),
//                            0
//                    ).xRot(-this.getXRot() * Mth.DEG_TO_RAD).yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
//                            .add(this.getX(), this.getY(0.5f), this.getZ());
                    LevelUtils.addFreshProjectile(this.level(), bone, RotUtils.getWorldPos(
                            x + radius * Mth.cos(angle * Mth.DEG_TO_RAD),
                            1.5f + radius * Mth.sin(angle * Mth.DEG_TO_RAD),
                            0,
                            this.getYHeadRot(),this.getXRot()
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
        float speed = 1.0f + fatigueLevel * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = 5 + fatigueLevel * 2;
        float angleScope = 60f + 30f * fatigueLevel;
        float[] offsetAngles = getPhaseID() == 1 ? new float[]{-1} : new float[]{-1, 1};
        float interval = angleScope / (count - 1);
        float rX = 0.8f + fatigueLevel * 0.1f;
        float rZ = 1f + fatigueLevel * 0.1f;
        // 圆心位置（实体位置）
        Vec3 center = new Vec3(this.getX(), this.getY(0.5f), this.getZ());
        for (float offsetAngle : offsetAngles) {
            float startAngle = offsetAngle * (angleScope / 2);
            for (int i = 0; i < count; i++) {
                FlyingBone bone = createFlyingBone(attackTypeUUID, speed, 0);
                float radian = (startAngle + (i * interval)) * Mth.DEG_TO_RAD;
                // 计算圆弧上的相对位置（相对于圆心的偏移）
                Vec3 relativeOffset = new Vec3(
                        rX * Mth.sin(radian),
                        0,
                        rZ * Mth.cos(radian) + 1
                ).xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                        .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD);
                // 径向向量：从圆心指向圆弧上的点（就是relativeOffset的方向）
                bone.vectorShoot(relativeOffset);
                LevelUtils.addFreshProjectileByVec3(this.level(), bone, center.add(relativeOffset), relativeOffset);
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
     * 在指定方向召唤前进骨墙
     */
    public int summonGroundBoneWall(LivingEntity target, ColorAttack color, float addHeight, LocalDirection direction) {
        Level level = this.level();
        int difficulty = level.getDifficulty().getId();
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = difficulty * 3;
        float speed = 0.8f + difficulty * 0.5f;
        double interval = 0.375;
        double xOffset = -interval * (count - 1) / 2;
        // 获取自身水平视线方向
        Vec3 horLookAngle = calculateViewVector(0, this.getYHeadRot());
        Vec3 perpendicular = new Vec3(-horLookAngle.z, 0, horLookAngle.x); // 垂直方向
        // 计算目标到自身的距离减1
        double distance = this.distanceTo(target) - 1;
        // 根据directionType计算起始位置（都基于目标位置）
        Vec3 centerPos;
        switch (direction) {
            // 前方：目标位置 + 视线方向 * (距离-1)
            case FRONT -> centerPos = target.position().add(horLookAngle.scale(distance));
            // 后方：目标位置 - 视线方向 * (距离-1)
            case BACK -> centerPos = target.position().add(horLookAngle.scale(-distance));
            // 左侧：目标位置 - 垂直方向 * (距离-1)
            case LEFT -> centerPos = target.position().add(perpendicular.scale(-distance));
            // 右侧：目标位置 + 垂直方向 * (距离-1)
            case RIGHT -> centerPos = target.position().add(perpendicular.scale(distance));
            // 默认前方
            default -> centerPos = target.position().add(horLookAngle.scale(distance));
        }
        // 计算骨墙朝向（从起始位置朝向目标）
        Vec3 toTarget = target.position().subtract(centerPos).normalize();
        float yRot = (float) Math.atan2(toTarget.x, toTarget.z);
        for (int i = 0; i < count; i++) {
            Vec3 pos = centerPos.add(new Vec3(xOffset, 0, 0).yRot(yRot));
            GroundBoneProjectile bone = new GroundBoneProjectile(level, this, pos.x, this.getY(), pos.z, addHeight, 1f, speed, color);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
            // 朝向目标射击
            bone.delayShoot(10, toTarget);
            bone.setYRot(RotUtils.shootYRot(toTarget));
            level.addFreshEntity(bone);
            xOffset += interval;
        }
        return 10;
    }

    /**
     * 带相对前方计算的正弦波缺口骨头矩阵
     *
     * @param waveType 正弦波形式
     */
    public int summonGroundBoneMatrix(LivingEntity target, int waveType) {
        int difficulty = this.level().getDifficulty().getId();
        Level level = this.level();
        String attackTypeUUID = UUID.randomUUID().toString();

        // 矩阵参数 - 就是骨头的行列数量
        int rows = 15 + 3 * difficulty;      // 行数（横向数量）
        int cols = 10 + 3 * difficulty;      // 列数（纵向/前后数量）
        double gapWidth = 2.1 - 0.3 * difficulty; // 缺口宽度（以列为单位）

        // 获取方向
        Vec3 direction = calculateViewVector(0, this.getYHeadRot());
        Vec3 normalDir = new Vec3(-direction.z, 0, direction.x).normalize();

        // 起始位置（目标前方一段距离）
        double startDistance = 8.0;
        Vec3 startPos = target.position().add(direction.scale(startDistance));

        // 确定矩阵覆盖的实际空间范围
        // 我们可以让骨头之间的间距固定，比如每个骨头间隔1.5格
        double spacing = 0.8; // 骨头之间的间距

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // 计算标准化横向位置（0到1）
                double t = row / (double) (rows - 1);
                // 计算正弦波缺口位置
                double sineValue = 0;
                switch (waveType) {
                    case 0 -> sineValue = Math.sin(t * Math.PI / 2);            // 上升1/4
                    case 1 -> sineValue = Math.sin(t * Math.PI / 2 + Math.PI / 2); // 下降1/4
                    case 2 -> sineValue = Math.sin(t * Math.PI);                // 先升后降
                    case 3 -> sineValue = -Math.sin(t * Math.PI);               // 先降后升
                }
                // 将正弦值[-1,1]映射到列索引[0, cols-1]
                double curveCol = (sineValue + 1) / 2 * (cols - 1);
                // 如果当前列在缺口附近，跳过生成
                if (Math.abs(col - curveCol) < gapWidth) {
                    continue;
                }
                // 直接计算位置：起始位置 + 右偏移 + 前偏移
                // 行索引转换为左右偏移：从-(rows-1)/2到+(rows-1)/2
                double xOffset = (row - (rows - 1) / 2.0) * spacing;
                // 列索引转换为前后偏移：0到负方向（向前）
                double zOffset = -col * spacing;
                Vec3 pos = startPos
                        .add(normalDir.scale(xOffset))     // 左右偏移
                        .add(direction.scale(zOffset));     // 前后偏移（负值表示向前）
                // 生成骨头
                GroundBoneProjectile bone = new GroundBoneProjectile(
                        level, this,
                        pos.x, this.getY(), pos.z,
                        0.5f, 1f, 0.6f, ColorAttack.WHITE
                );
                bone.setData(AttachmentTypeRegistry.KARMA_ATTACK,new KaramAttackData(attackTypeUUID, (byte) 6));
                bone.delayShoot(0, direction);
                bone.setYRot(RotUtils.shootYRot(direction));
                level.addFreshEntity(bone);
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
            float offset = startOffset + i * waveSpacing;
            Vec3 waveStartPos = this.position().add(perpendicular.scale(offset));
            int waveDelay = summonGroundBoneSpineWave(target, rows, cols, waveStartPos, direction, color, baseDelay + i * 2,10f);
            finalDelay = Math.max(finalDelay, waveDelay);
        }
        return finalDelay;
    }


    /**
     * 骨刺波动 - 以自身为中心发射，特定角度步长发射
     */
    public int summonGroundBoneSpineWaveAroundSelf(@NotNull LivingEntity target, float angleStep, ColorAttack color) {
        return summonGroundBoneSpineWaveAroundSelf(target, 1 + this.fatigueLevel*2, angleStep, color);
    }

    /**
     * 骨刺波动 - 以自身为中心发射
     */
    public int summonGroundBoneSpineWaveAroundSelf(@NotNull LivingEntity target, int count, float angleStep, ColorAttack color) {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 horLookAngle = calculateViewVector(0, getYHeadRot());
        int rows = ATTACK_RANGE * 2 + difficulty / 3 * 5;
        int cols = 3 + difficulty;
        int delay = 10;
        // 计算起始角度（让波动对称分布）
        float angle = -angleStep * (count - 1) / 2f;
        for (int i = 0; i < count; i++,angle += angleStep) {
            delay = summonGroundBoneSpineWave(target, rows, cols, this.position(), horLookAngle.yRot(angle * Mth.DEG_TO_RAD), color, 10,10f);
        }
        return delay;
    }
    /**
     * 骨刺波动 - 以自身为中心的圆径向发射，无初始偏移
     */
    public int summonGroundBoneSpineWaveAroundSelf(@NotNull LivingEntity target, ColorAttack color) {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 horLookAngle = calculateViewVector(0, getYHeadRot());
        int rows = ATTACK_RANGE * 2 + difficulty / 3 * 5;
        int cols = 3 + difficulty;
        int delay = 10;
        int count = 5 + fatigueLevel + difficulty;
        float angleStep = 360f/count, angle = 0;
        for (int i = 0; i < count; i++,angle += angleStep) {
            delay = summonGroundBoneSpineWave(target, rows, cols, this.position(), horLookAngle.yRot(angle * Mth.DEG_TO_RAD), color, 10,10f);
        }
        return delay;
    }

    /**
     * 公用方法：召唤定向骨刺波动
     */
    public int summonGroundBoneSpineWave(LivingEntity target, int rows, int cols, Vec3 startPos, Vec3 direction, ColorAttack color, int delay,float gaussianDev) {
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
                createGroundBone(attackTypeUUID, bonePos.x, bonePos.z, minY, maxY,
                        delay, 20, 1f, color, false,gaussianDev);
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
        int layer = 13 + fatigueLevel + difficulty;
        PacketDistributor.sendToPlayersTrackingEntity(this,new WarningTipPacket((float) pos.x, (float) pos.y, (float) pos.z, layer * 0.6f, 1.0f,20, Color.RED));
        this.level().playSound(null,pos.x,pos.y,pos.z,SoundRegistry.ENEMY_ENCOUNTER_ATTACK_TIP.get(),SoundSource.HOSTILE);

        int delay = 13 - fatigueLevel;
        String attackTypeUUID = UUID.randomUUID().toString();
        createGroundBone(attackTypeUUID, pos.x, pos.z, pos.y, pos.y, delay, 20, 1.0f, ColorAttack.WHITE, true,10f);
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = 0.6f * (i + 1);
            float angle = interval / 2; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                createGroundBone(attackTypeUUID,
                        pos.x + r * Mth.cos(angle * Mth.DEG_TO_RAD),
                        pos.z + r * Mth.sin(angle * Mth.DEG_TO_RAD),
                        pos.y, pos.y, delay, 20, 1.0f, ColorAttack.WHITE, false,10f);
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
        return summonCircleGroundBoneSpine(target, 3 * (difficulty + 1), pos.x, pos.z, 13 - difficulty, 10, 1f - (float) difficulty / 3);
    }

    /**
     * 在目标周围随机区域召唤地面骨刺
     */
    public int summonGroundBoneSpineAroundTarget(@NotNull LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 pos = target.position();
        return summonCircleGroundBoneSpine(target, 3 + difficulty, pos.x + this.random.nextDouble() * ATTACK_RANGE, pos.z + this.random.nextDouble() * ATTACK_RANGE
                , 13 - difficulty, 10 + this.random.nextInt(15), 1f);
    }

    /**
     * 以目标位置为中心的圆形骨刺
     *
     * @param target   目标
     * @param layer    圆环层数
     * @param x        中心坐标x
     * @param z        中心坐标z
     * @param delay    延迟
     * @param lifetime 生命周期
     * @param offset  骨刺刺出自身高度的比例
     * @return 执行CD
     */
    public int summonCircleGroundBoneSpine(LivingEntity target, int layer, double x, double z, int delay, int lifetime, float offset) {
        PacketDistributor.sendToPlayersTrackingEntity(this,new WarningTipPacket((float) x, (float) target.getY(), (float) z, layer * 0.6f, offset,lifetime, Color.RED));
        this.level().playSound(null,x,getY(),z,SoundRegistry.ENEMY_ENCOUNTER_ATTACK_TIP.get(),SoundSource.HOSTILE);
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        createGroundBone(attackTypeUUID, x, z, minY, maxY, delay, lifetime, offset, ColorAttack.WHITE, true,10f);
        for (int i = 0; i < layer; i++) {
            int count = 8 * (i + 1);
            float interval = 360f / count;
            float r = 0.7f * (i + 1);
            float angle = interval / 2; // 初始位置错位
            for (int j = 0; j < count; j++, angle += interval) {
                createGroundBone(attackTypeUUID,
                        x + r * Mth.cos(angle * Mth.DEG_TO_RAD),
                        z + r * Mth.sin(angle * Mth.DEG_TO_RAD),
                        minY, maxY, delay, lifetime, offset, ColorAttack.WHITE, false,10f);
            }
        }
        return delay;
    }

    /**
     * 实心正方形骨刺
     *
     * @param target   目标
     * @param size     正方形半径（从中心到边的格数）
     * @param x        中心坐标x
     * @param z        中心坐标z
     * @param delay    延迟
     * @param lifetime 生命周期
     * @param offset  骨刺刺出自身高度的比例
     * @return 执行CD
     */
    public int summonSquareGroundBoneSpine(LivingEntity target, int size, double x, double z, int delay, int lifetime, float offset) {
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        // 生成整个实心正方形区域
        for (int i = -size; i <= size; i++) {
            for (int j = -size; j <= size; j++) {
                double boneX = x + i * 0.5f;
                double boneZ = z + j * 0.5f;
                boolean isCenter = (i == 0 && j == 0);
                createGroundBone(attackTypeUUID, boneX, boneZ, minY, maxY, delay, lifetime, offset, ColorAttack.WHITE, isCenter,10f);
            }
        }
        return delay;
    }

    /**
     * @param offset 根据难度决定骨刺刺出自身高度的比例
     * @param gaussianDev xRot 高斯分布的标准差
     */
    protected void createGroundBone(String attackUUID, double targetX, double targetZ, double minY, double maxY, int delay, int lifetime, float offset, ColorAttack colorAttack, boolean isPlaySound,float gaussianDev) {
        Level level = this.level();
        double spawnY = EntityUtils.findGroundY(level, targetX, targetZ, minY, maxY);
        // 如果找到有效地面，生成骨刺
        if (spawnY != level.getMinBuildHeight()) {
            GroundBone bone = new GroundBone(level, this, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), delay, targetX, spawnY, targetZ,offset, colorAttack, isPlaySound, lifetime);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackUUID, (byte) 6));
            // 设置旋转：骨刺指向圆心（目标位置）
            bone.setYRot(this.getYHeadRot());
            // 添加随机X旋转，看起来不规则
            bone.setXRot((float) this.random.nextGaussian() * gaussianDev);
            level.addFreshEntity(bone);

            level.gameEvent(GameEvent.ENTITY_PLACE, new Vec3(targetX, spawnY, targetZ), GameEvent.Context.of(this));
        }
    }



    /**
     * 用于单击
     */
    public int summonGBAroundSelf(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        return summonGBAroundSelf(target, fatigueLevel/2 + difficulty/3 + 1, 1.0f + fatigueLevel*0.1f);
    }

    /**
     * 在自身周围随机位置召唤GB
     */
    public int summonGBAroundSelf(LivingEntity target, int count, float size) {
        for (int i = 0; i < count; i++) {
            GasterBlaster gb = createGasterBlaster(size);
            LevelUtils.addFreshEntity(level(),gb,this.getEyePosition().add(RotUtils.getWorldPos(
                    this.random.nextDouble() *12 -6,
                    this.random.nextDouble() * 3 + 3,
                    this.random.nextDouble() * 5,
                    this.getXRot(),this.getYHeadRot())),target.getEyePosition());
        }
        return 0;
    }

    /**
     * 用于单击，环绕目标周围
     */
    public int summonGBAroundTarget(LivingEntity target) {
        return summonGBAroundTarget(target,2 + this.random.nextInt(3 + fatigueLevel), 45.0*this.random.nextInt(4));
    }
    /**
     * 以目标和自身长度为半径的圆环上召唤GB，固定360度范围，根据数量自动计算角度步长和大小
     */
    public int summonGBAroundTarget(LivingEntity target, int count, double offsetAngle) {
        return summonGBAroundTarget(target, count, offsetAngle, 360.0 / count, 1.0f + (4 - count + fatigueLevel)*0.25f);
    }
    /**
     * 用于单击，在前方
     */
    public int summonGBFront(LivingEntity target) {
        return summonGBAroundTarget(target,(1 + fatigueLevel)*2 ,0.0, 30.0, 1.0f+fatigueLevel*0.1f);
    }

    /**
     * 以目标和自身长度为半径的圆环上召唤GB
     *
     * @param target      目标实体
     * @param count       GB数量
     * @param offsetAngle 初始偏移角度（度）
     * @param angleStep   角度步长（度）
     */
    public int summonGBAroundTarget(LivingEntity target, int count, double offsetAngle, double angleStep, float size) {
        double baseRadius = this.distanceTo(target) * 0.75f; // 以距离为基准半径
        double currentAngle = offsetAngle; // 从指定角度开始

        for (int i = 0; i < count; i++) {
            // 固定半径保持对称性
            double radius = baseRadius + (this.random.nextDouble() * 2.0 - 1.0);
            double height = this.random.nextDouble() * 3 + 1;
            GasterBlaster gb = createGasterBlaster(size);
            Vec3 targetEyePos = target.getEyePosition();
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            LevelUtils.addFreshEntity(this.level(), gb, targetEyePos.add(xOffset, height, zOffset), targetEyePos);
            // 按照固定角度步长递增
            currentAngle += angleStep;
        }
        return 0;
    }

    public GasterBlaster createGasterBlaster(float size) {
        GasterBlaster gb = new GasterBlaster(EntityTypeRegistry.GASTER_BLASTER.get(), this.level(), this, size);
        gb.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(UUID.randomUUID().toString(), (byte) 10));
        return gb;
    }

    public int gravityControl(LivingEntity target, LocalDirection direction) {
//        GravityData.applyRelativeGravity(this, target, direction);
        target.setDeltaMovement(LocalVec3.fromLocal(direction, target.getLookAngle(), target.getDeltaMovement()));
        this.isAppendSpine = true;
        return 0;
    }

    /**
     * 时间跳跃传送
     */
    public int timeJumpTeleport(LivingEntity target) {
        //todo 玩家黑屏 + 音效
        PacketDistributor.sendToServer(new TimeJumpTeleportPacket(target.getId(), target.tickCount + 20));
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.SANS_TELEPORT_TIME_JUMP.get(), SoundSource.HOSTILE);
        return 0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static final String ANIM_CAST = "attack.cast";
    private static final String ANIM_CAST_LEFT = "attack.cast.left";
    private static final String ANIM_CAST_ROUND = "attack.cast.round";
    private static final String ANIM_CAST_ROUND_LEFT = "attack.cast.round.left";
    private static final String ANIM_BONE_PROJECTILE = "attack.bone.projectile";
    private static final String ANIM_BONE_PROJECTILE_LEFT = "attack.bone.projectile.left";
    private static final String ANIM_BONE_SWEEP = "attack.bone.sweep";
    private static final String ANIM_BONE_SWEEP_LEFT = "attack.bone.sweep.left";

    private static final String[] THROW_ANIM_NAMES = new String[]{"attack.throw.up", "attack.throw.down", "attack.throw.left", "attack.throw.right", "attack.throw.front", "attack.throw.back"};

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Sans> attackController = new AnimationController<>(this, "attack", state -> {
            AnimationController<Sans> controller = state.getController();

            Sans animatable = state.getAnimatable();
            byte animID = animatable.getAnimID();
            float animSpeed = animatable.getAnimSpeed();
            if (animID == -1) {
                controller.forceAnimationReset();
                return PlayState.STOP;
            }
            byte phaseID = getPhaseID();
            switch (animID) {
                case 0, 1, 2, 3, 4, 5 -> controller.setAnimation(RawAnimation.begin().thenPlay(THROW_ANIM_NAMES[animID]));
                case 6 -> controller.setAnimation(RawAnimation.begin().thenPlay(phaseID == FIRST_PHASE ? ANIM_CAST_LEFT : ANIM_CAST));
                case 7 -> controller.setAnimation(RawAnimation.begin().thenPlay(phaseID == FIRST_PHASE ? ANIM_CAST_ROUND_LEFT : ANIM_CAST_ROUND));
                case 8 -> controller.setAnimation(RawAnimation.begin().thenPlay(phaseID == FIRST_PHASE ? ANIM_BONE_PROJECTILE_LEFT : ANIM_BONE_PROJECTILE));
                case 9 -> controller.setAnimation(RawAnimation.begin().thenPlay(phaseID == FIRST_PHASE ? ANIM_BONE_SWEEP_LEFT : ANIM_BONE_SWEEP));
            }
            controller.setAnimationSpeed(animSpeed);
            // todo 粒子
//            controller.setParticleKeyframeHandler(particleKeyframeEvent -> {
//                particleKeyframeEvent.getKeyframeData().getLocator()
//            })
            return PlayState.CONTINUE;
        });

        controllers.add(
                DefaultAnimations.genericWalkIdleController(this),
                DefaultAnimations.genericLivingController(this),
                attackController
        );
    }


}

