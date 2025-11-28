package com.sakpeipei.mod.undertale.entity.boss;

import com.sakpeipei.mod.undertale.common.RelativeDirection;
import com.sakpeipei.mod.undertale.entity.IAnimatable;
import com.sakpeipei.mod.undertale.entity.ai.goal.AbstractAnimExecuteGoal;
import com.sakpeipei.mod.undertale.entity.ai.goal.SingleAnimExecuteGoal;
import com.sakpeipei.mod.undertale.entity.ai.goal.NeutralMobAngerTargetGoal;
import com.sakpeipei.mod.undertale.entity.attachment.GravityData;
import com.sakpeipei.mod.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.mod.undertale.entity.common.*;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.mod.undertale.entity.projectile.GroundBoneProjectile;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import com.sakpeipei.mod.undertale.entity.summon.GroundBone;
import com.sakpeipei.mod.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.mod.undertale.network.WarningTipPacket;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.mod.undertale.utils.EntityUtils;
import com.sakpeipei.mod.undertale.utils.LevelUtils;
import com.sakpeipei.mod.undertale.utils.RotUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
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
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.ToIntFunction;

public class Sans extends Monster implements NeutralMob, GeoEntity, IAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final Logger log = LoggerFactory.getLogger(Sans.class);

    private final static short ATTACK_RANGE = 16;  // 攻击距离

    private int physicalStrength;     // 当前体力
    private int maxPhysicalStrength;  // 最大体力

    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(30, 60);
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    public Sans(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        physicalStrength = level.getDifficulty().getId() * 50;
        maxPhysicalStrength = physicalStrength;
    }

    @Override
    public void handleEntityEvent(byte p_21375_) {
        super.handleEntityEvent(p_21375_);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
//        this.goalSelector.addGoal(0,new GravityControlCollisionDetectionGoal());
        // 远程攻击，需要实现performRangedAttack，然后通过goal去调用
        this.goalSelector.addGoal(1, new TransitionPhaseGoal());
        // 远程攻击，需要实现performRangedAttack，然后通过goal去调用
        this.goalSelector.addGoal(1, new SingleAttackGoalSingle());
        this.goalSelector.addGoal(2, new SansMovementGoal(1.0,16.0f));


        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomStrollGoal(this, 0.5f));

        this.targetSelector.addGoal(0, new NeutralMobAngerTargetGoal(this,this.level()));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            this.updatePersistentAnger((ServerLevel)this.level(), true);
        }
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        if(this.globalCD > 0) {
            this.globalCD--;
        }
        if(physicalStrength > maxPhysicalStrength / 2 && !isSpecial) {
            if(this.tickCount % 20 == 0){
                physicalStrength = Math.min(physicalStrength + 1 ,maxPhysicalStrength);
            }
        }else{
            isSpecial = true;
        }
        super.customServerAiStep();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float power) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if(physicalStrength > 0) {
            Entity sourceEntity = source.getEntity();
            // 体力消耗逻辑
            if(source.is(DamageTypes.MOB_PROJECTILE) || source.is(DamageTypes.ARROW) || source.is(DamageTypes.MAGIC) || source.is(Tags.DamageTypes.IS_ENVIRONMENT)){
                physicalStrength = Math.max(1 , physicalStrength - 1);
            }else if(source.is(Tags.DamageTypes.IS_TECHNICAL)){
                physicalStrength = Math.max(0 , physicalStrength - (int)power);
                return super.hurt(source, power);
            }else{
                physicalStrength = Math.max(1 , physicalStrength - 2);
            }

            if(getTarget() == null){
                if (sourceEntity instanceof LivingEntity livingEntity) {
                    this.setLastHurtByMob(livingEntity);
                }else if(sourceEntity instanceof Player player){
                    this.setLastHurtByPlayer(player);
                }
            }
            if(physicalStrength <= maxPhysicalStrength / 2 && isSpecial){
                isSpecial = false;
            }
            if(sourceEntity != null){
                if(this.distanceToSqr(sourceEntity) <= 25){
                    meleeTeleport(sourceEntity);
                }else{
                    rangedTeleport(source.getDirectEntity());
                }
            }else{
                randomTeleport();
            }
            return true;
        }

        return super.hurt(source, power);
    }
    private void rangedTeleport(Entity entity) {
        float baseAngle;
        double r;
        if(entity == null){
            baseAngle = this.getYHeadRot() - 90f;
            r = 3 ;
        }else{
            r = entity.getBbWidth() + this.getBbWidth()+ 2*getPickRadius();
            Vec3 movement = entity.getDeltaMovement();
            if(movement.lengthSqr() == 0f){
                baseAngle = -RotUtils.yRotD(this.position().subtract(entity.position()));
            }else{
                baseAngle = -RotUtils.yRotD(movement);
            }
        }
        for(int i = 0; i < 64 ; i++){
            boolean b = random.nextBoolean();
            float angle = baseAngle + (b? 90 : -90);
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
                Sans.this.getZ() + dir.z + (random.nextDouble() - 0.5) * 4);
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

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PhysicalStrength",this.physicalStrength);
        tag.putInt("MaxPhysicalStrength",this.maxPhysicalStrength);
        this.addPersistentAngerSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag){
        super.readAdditionalSaveData(tag);
        if(tag.contains("PhysicalStrength")){
            this.physicalStrength = tag.getInt("PhysicalStrength");
        }
        if(tag.contains("MaxPhysicalStrength")){
            this.maxPhysicalStrength = tag.getInt("MaxPhysicalStrength");
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }



    private int canAttack; //可攻击
    private int globalCD = 0;           //全局CD
    private boolean isSpecial = false;  //标识需要特殊处理的阶段，即初见杀，饶恕阶段，特殊攻击

    class TransitionPhaseGoal extends Goal{
        public TransitionPhaseGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return physicalStrength == maxPhysicalStrength / 2;
        }
    }



    private final int[][][] sequences = {
            {{3,30,1,1,0},{3,30,2,2,0},{3,30,2,1,0}},                       // 骨刺波动进阶
            {{3,0,2,1,0},{3,40,1,1,1}, {3,0,3,1,0},{3,40,2,1,1}, {3,0,4,1,1},{3,40,3,1,0}},                       // 骨刺波动进阶
            {{3,0,3,1,0},{3,40,1,2,1}, {3,0,3,1,0},{3,40,2,1,1}, {3,0,4,1,1},{3,40,3,1,0}},                       // 骨刺波动进阶
    };
    private final List<List<Integer>> stateSequences = Arrays.asList(
            Arrays.asList(1,2,8),
            Arrays.asList(4,5,6,7),
            Arrays.asList(3),
            Arrays.asList(9)
    );

    private int seeTime;
    private int phase;
    class SansMovementGoal extends Goal {
        private final double speedModifier;
        private final float attackRadiusSqr;
        private final float backRadiusSqr;
        private final float pursuitRadiusSqr;

        public SansMovementGoal(double speedModifier, float attackRadius) {
            this.speedModifier = speedModifier;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.backRadiusSqr = this.attackRadiusSqr / 4;
            this.pursuitRadiusSqr = this.attackRadiusSqr + this.attackRadiusSqr / 2 ;
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
                if(hasSeeSight){ seeTime++; }
                else{ seeTime = Math.min(0,seeTime - 1); }

                Sans.this.getLookControl().setLookAt(target, Sans.this.getHeadRotSpeed(), Sans.this.getMaxHeadXRot());
                if(seeTime > 0){
                    //                    ++this.strafingTime;
                    if (disSqr <= backRadiusSqr){
                        Sans.this.getNavigation().stop();
                        Sans.this.getMoveControl().strafe(-0.75f,0.0f);
                        Sans.this.setYRot(Mth.rotateIfNecessary(Sans.this.getYRot(), Sans.this.yHeadRot, 0.0F));
                    }else if(disSqr > backRadiusSqr && disSqr <= attackRadiusSqr){
                        Sans.this.getNavigation().stop();
                    }else{
                        Sans.this.getNavigation().moveTo(target, speedModifier);
                        if(disSqr > pursuitRadiusSqr){
                            teleportTowards(target);
                        }
                    }



                }else if(seeTime > -60){ // 丢失视线3秒内
                    if(disSqr <= pursuitRadiusSqr){
                        Sans.this.getNavigation().moveTo(target,this.speedModifier);
                    }else{
                        teleportTowards(target);
                    }
                }
            }
        }
    }

    private final ToIntFunction<LivingEntity> CONE_SPIKE_PULSE_BONE  = target -> onceFlyingBone(target,0);
    private final ToIntFunction<LivingEntity> CURVED_SPIKE_PULSE_BONE  = target -> onceFlyingBone(target,1);
    private final ToIntFunction<LivingEntity> TRIANGULAR_ASSAULT_PULSE_BONE  = target -> onceFlyingBone(target,2);
    private final ToIntFunction<LivingEntity> GROUND_WAVE_SPIKE_1   = target -> waveSpineTargetAttack(target,1,1,0);
    private final ToIntFunction<LivingEntity> GROUND_WAVE_SPIKE_2   = target -> waveSpineTargetAttack(target,1,2,0);
    private final ToIntFunction<LivingEntity> SELF_WAVE_SPIKE  = this::selfSpikeAttack;

    // 单次攻击Goal - 处理所有简单攻击
    private final List<OnceTimingAnim<List<ToIntFunction<LivingEntity>>>> singleAttacks = List.of(
            new OnceTimingAnim<>((byte) 2, 20, 10, 30, List.of(CONE_SPIKE_PULSE_BONE)),
            new OnceTimingAnim<>((byte) 2, 20, 10, 30, List.of(CURVED_SPIKE_PULSE_BONE)),
            new OnceTimingAnim<>((byte) 2, 20, 10, 30, List.of(TRIANGULAR_ASSAULT_PULSE_BONE)),
            new OnceTimingAnim<>((byte) 2, 20, 10, 30, List.of(GROUND_WAVE_SPIKE_1, GROUND_WAVE_SPIKE_2)), // 波动骨刺
            new OnceTimingAnim<>((byte) 5, 20, 10, 30, List.of(SELF_WAVE_SPIKE)) // 自身骨刺
    );

    private boolean existPersistentAttack;

    private final ToIntFunction<LivingEntity> SPINE_ATTACK = this::targetSpineAttack;

    private class PersistentAttackGoal extends AbstractAnimExecuteGoal<ToIntFunction<LivingEntity>> {

        private final List<AnimType<ToIntFunction<LivingEntity>>> attacks = List.of(
                new OnceTimingAnim<>((byte) 1, 20, 4, 30, Sans.this::aimTargetRandomBarrage),
                new OnceTimingAnim<>((byte) 1, 20, 4, 30, Sans.this::shootForwardRandomBarrage),
                new RoundSequenceAnim<>(10,List.of(new OnceTimingAnim<>((byte)7,20,4,30, Sans.this::summonBoneSpineAroundTarget))),
                new RoundSequenceAnim<>(10,List.of(new OnceTimingAnim<>((byte)7,20,4,30, target -> Sans.this.selfGBAttack(target,1))))
        );

        private int cooldown;

        public PersistentAttackGoal(int cooldown) {
            super(Sans.this);
            this.cooldown = cooldown;
        }

        @Override
        public boolean canUse() {
            cooldown--;
            return cooldown <=0  && super.canUse();
        }

        @Override
        protected @NotNull AnimType<ToIntFunction<LivingEntity>> select(LivingEntity target) {
            existPersistentAttack = true;
            return attacks.get(Sans.this.random.nextInt(attacks.size()));
        }

        @Override
        protected int execute(LivingEntity target, AnimType<ToIntFunction<LivingEntity>> anim) {
            return anim.getAction().applyAsInt(target);
        }

        @Override
        protected void onCompleted() {
            existPersistentAttack = false;
        }
    }

    // 连击Goal - 处理所有连击
    class SequenceAttackGoal extends AbstractAnimExecuteGoal<List<ToIntFunction<LivingEntity>>> {
        private final ToIntFunction<LivingEntity> BONE_WALL_WHITE = target -> groundBoneProjectileAttack(target,0,0,0);
        private final ToIntFunction<LivingEntity> BONE_WALL_AQUA = target -> groundBoneProjectileAttack(target,1,1,0);


            private final List<AnimType<List<ToIntFunction<LivingEntity>>>> attacks = List.of(
                    new RoundSequenceAnim<>(3, List.of( new OnceTimingAnim<>((byte) 3, 30, 4, 20, BONE_WALL_WHITE),new OnceTimingAnim<>((byte)3,30,4,30,BONE_WALL_AQUA ))), // 骨墙
                    new RoundSequenceAnim<>(3, List.of(new OnceTimingAnim<>((byte) 1, 30, 4, 30, List.of(GROUND_WAVE_SPIKE_1, GROUND_WAVE_SPIKE_2)))),
                    new RoundSequenceAnim<>(3, List.of(new OnceTimingAnim<>((byte) 1, 30, 4, 30, List.of(GROUND_WAVE_SPIKE_1, GROUND_WAVE_SPIKE_2)))),
                    new RoundSequenceAnim<>(3, List.of(new OnceTimingAnim<>((byte) 1, 30, 4, 30, List.of(GROUND_WAVE_SPIKE_1, GROUND_WAVE_SPIKE_2)))),
                    new RoundSequenceAnim<>(3, List.of(new OnceTimingAnim<>((byte) 1, 30, 4, 30, List.of(GROUND_WAVE_SPIKE_1, GROUND_WAVE_SPIKE_2))))
            );

        public SequenceAttackGoal() {
            super(Sans.this);
        }


        @Override
        protected @NotNull AnimType<List<ToIntFunction<LivingEntity>>> select(LivingEntity target) {
            return new RoundSequenceAnim<>(3, List.of(new OnceTimingAnim<>((byte) 1, 30, 4, 30, List.of(GROUND_WAVE_SPIKE_1, GROUND_WAVE_SPIKE_2))));
        }

        @Override
        protected int execute(LivingEntity target, AnimType<List<ToIntFunction<LivingEntity>>> anim) {
            return 0;
        }

        @Override
        protected void onCompleted() {

        }
    }


    //动画执行
    private class SingleAttackGoalSingle extends SingleAnimExecuteGoal<List<ToIntFunction<LivingEntity>>> {
        public SingleAttackGoalSingle() {
            super(Sans.this);
        }
        @Override
        public boolean canUse() {
            return seeTime > 20 && super.canUse();
        }

        @Override
        protected @NotNull AbstractAnimType<List<ToIntFunction<LivingEntity>>> select(LivingEntity target) {
            boolean onGround =  target.onGround();
            boolean canFlying = target instanceof FlyingMob || target instanceof FlyingAnimal || target.hasEffect(MobEffects.LEVITATION);
//            boolean inAir = target.isFallFlying() || (!onGround && ( canFlying || target.onClimbable()));
            List<Integer> availableList = new ArrayList<>(List.of(0, 1, 2, 3));
            if(onGround){
                Collections.addAll(availableList, 4, 5);
            }
//            if(inAir || Sans.this.physicalStrength <= maxPhysicalStrength / 2 ) {
//                availableList.addAll(stateSequences.get(3));
//            }
            return singleAttacks.get(availableList.get(random.nextInt(availableList.size())));
        }

        @Override
        protected int execute(LivingEntity target) {
            if(anim.getSoundEvent() != null){
                Sans.this.level().playSound(null,target.getX(),target.getY(),target.getZ(),anim.getSoundEvent(),SoundSource.HOSTILE,1.0f,1.0f);
            }
            int cd = 0;
            for (ToIntFunction<LivingEntity> action : anim.getAction()) {
                cd = Math.max(cd,action.applyAsInt(target));
            }
            return cd;
        }
    }


    private class GravityControlCollisionDetectionGoal extends Goal {
        private boolean lastOnGround;

        @Override
        public void start() {
            LivingEntity target = Sans.this.getTarget();
            if (target != null) {
                this.lastOnGround = target.onGround();
            }else{
                this.lastOnGround = false;
            }
        }

        @Override
        public boolean canUse() {
            LivingEntity target = Sans.this.getTarget();
            if (target != null) {
                GravityData gravityData = target.getData(AttachmentTypeRegistry.GRAVITY);
                return gravityData != null;
    /        }else return false;
        }

        @Override
        public void tick() {
            LivingEntity target = Sans.this.getTarget();
            boolean onGround = target.onGround();
            if(onGround && lastOnGround != onGround){
                log.info("落地");
//                Sans.this.targetSpineAttack(target,Sans.this.level().getDifficulty().getId());
            }
        }
    }

    private int executeAttack(LivingEntity target, ActionData data){
        int[] params = data.getParams();
        return switch (data.getId()) {
            case 1 -> Sans.this.continueFlyingBone(target)  ;
            case 2 -> Sans.this.onceFlyingBone(target, params[0]);
            case 3 -> Sans.this.groundBoneProjectileAttack(target, params[0], params[1], params[2]);
            case 4 -> Sans.this.waveSpineTargetAttack(target, params[0], params[1], params[2]);
            case 5 -> Sans.this.selfSpikeAttack(target);
            case 6 -> Sans.this.targetSpineAttack(target);
            case 7 -> Sans.this.randomAreaSpineAttack(target);
            case 8 -> Sans.this.gbAttack(target,0,params[0]);
            case 9 -> {
                GravityData.applyRelativeGravity(Sans.this, target, GravityData.RelativeDirection.values()[params[0]]);
                yield  0;
            }
            default -> throw new IllegalStateException("Unexpected value: " + data.getId());
        };
    }
    // 持续射击
    /**
     * 瞄准目标随机骨头弹幕 - 持续射击目标
     * @return 需要执行完攻击的动画CD
     */
    private int shootAimedBarrage(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        String attackTypeUUID = UUID.randomUUID().toString();
        float speed = 1.0f + 0.333f * difficulty;
        int delay = 14 - 2*difficulty;
        int count = 7 * difficulty; // 模式0的count
        // 三层高度：腿、身体、眼睛
        double[] heights = {
                target.getEyeY(),       // 眼睛高度
                target.getY(0.5), // 身体高度
                target.getY(0.15),           // 腿部高度
        };
        for (int i = 0; i < count; i++) {
            FlyingBone bone = createFlyingBone(attackTypeUUID,speed,delay);
            do {
                Vec3 pos = this.position().add(
                        new Vec3((this.random.nextDouble() - 0.5) * ( 12 * difficulty),
                                this.random.nextDouble() * (2 + difficulty) + this.getBbHeight() * 0.5f,
                                this.random.nextDouble() * (3 + difficulty)
                        )
                                .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                                .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                );
                LevelUtils.addFreshProjectile(this.level(),bone,pos,target.getX(),heights[this.random.nextInt(heights.length)],target.getZ());
            } while (!bone.level().noCollision(bone, bone.getBoundingBox()));
            bone.aimShoot();
            delay += 6 - difficulty;
        }
        return delay;
    }
    /**
     * 射向前方随机骨头弹幕 - 以目标碰撞高度为随机范围高度，向前方范围随机射击
     */
    private int shootForwardBarrage(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        float speed = 1.5f + difficulty * 0.2f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay = 10;
        int count = 4 + difficulty * 3;
        // Sans的视线方向（固定射击方向）
        Vec3 sansLookDirection = this.getLookAngle();
        for (int i = 0; i < count; i++) {
            FlyingBone bone = createFlyingBone(attackTypeUUID, speed, delay);
            // 随机XZ坐标，围绕目标周围
            double randomX = target.getX() + (this.random.nextDouble() - 0.5) * (8 * difficulty);
            double randomZ = target.getZ() + (this.random.nextDouble() - 0.5) * (4 * difficulty);
            double randomY = target.getY() + this.random.nextDouble() * target.getBbHeight();
            Vec3 spawnPos = new Vec3(randomX, randomY, randomZ);
            // 固定向Sans的视线方向射击（封锁前方区域）
            bone.vectorShoot(sansLookDirection);
            LevelUtils.addFreshProjectile(this.level(), bone, spawnPos, sansLookDirection);
            delay += 2;
        }
        return delay;
    }

    // 一次性召唤攻击
    /**
     * 骨环齐射 - 向目标射击
     * @return 需要执行完攻击的动画CD
     */
    private int shootBoneRingVolley(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        double[] offsetX = difficulty == Difficulty.HARD.getId() || phase == 2? new double[]{3.0, -3.0} :new double[]{3.0};
        float speed = this.random.nextFloat() * 0.2f + difficulty * 0.5f + 1.0f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay = 10;
        int count = 5 + difficulty;
        float radius = 0.4f + difficulty * 0.3f;
        float interval = 360f / count;
        for (double x : offsetX) {
            Vec3 centerPos = new Vec3(this.getX() + x,this.getY(0.5f) + 2,this.getZ());
            for (int i = 0; i < count; i++) {
                FlyingBone bone = createFlyingBone(attackTypeUUID,speed,delay);
                float angle = i * interval;
                Vec3 pos = centerPos.add(new Vec3(
                        radius * Mth.cos(angle * Mth.DEG_TO_RAD),
                        0,
                        radius * Mth.sin(angle * Mth.DEG_TO_RAD))
                        .xRot(-this.getXRot() * Mth.DEG_TO_RAD).yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                );
                bone.aimShoot();
                LevelUtils.addFreshProjectile(this.level(),bone,pos,target);
            }
        }
        return delay;
    }
    /**
     * 弧形横扫齐射 - 骨头在圆弧上朝外径向发射
     * @return 需要执行完攻击的动画CD
     */
    private int shootArcSweepVolley(LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        float speed = this.random.nextFloat() * 0.2f + difficulty * 0.5f + 1.0f;
        String attackTypeUUID = UUID.randomUUID().toString();
        double[] offsetX = (difficulty == Difficulty.HARD.getId() || phase == 2) ? new double[]{3.0, -3.0} : new double[]{3.0};
        int count = 5 + difficulty * 2;
        float interval = 180f / (count - 1);
        float rX = 0.8f + difficulty * 0.1f;
        float rZ = 1f + difficulty * 0.1f;
        for (double x : offsetX) {
            Vec3 centerPos = new Vec3(this.getX() + x, this.getY(0.5f), this.getZ());
            float startAngle = -90f; // 从-90度开始（左侧）
            for (int i = 0; i < count; i++) {
                FlyingBone bone = createFlyingBone(attackTypeUUID, speed, 0);
                float angle = startAngle + (i * interval);
                // 计算骨头在圆弧上的位置
                Vec3 pos = centerPos.add(new Vec3(
                                rX * Mth.cos(angle * Mth.DEG_TO_RAD),
                                0,
                                rZ * Mth.sin(angle * Mth.DEG_TO_RAD)
                        )
                                .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                                .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                );
                // 使用角度直接计算径向方向（更准确）
                Vec3 radialVec3 = new Vec3(
                        Mth.cos(angle * Mth.DEG_TO_RAD),
                        0,
                        Mth.sin(angle * Mth.DEG_TO_RAD)
                );
                bone.vectorShoot(radialVec3);
                LevelUtils.addFreshProjectile(this.level(),bone,pos,radialVec3);
            }
        }
        return 0;
    }
    private FlyingBone createFlyingBone(String attackTypeUUID,float speed,int delay) {
        FlyingBone bone = new FlyingBone(EntityTypeRegistry.FLYING_BONE.get(), this.level(), this, 1f, speed,delay);
        bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
        return bone;
    }




    /**
     * 在指定方向召唤前进骨墙
     */
    private int summonAdvancingGroundBoneWall(LivingEntity target, int isAqua, float addHeight, RelativeDirection direction) {
        Level level = this.level();
        int difficulty = level.getDifficulty().getId();
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = (difficulty + isAqua) * 3;
        float speed = 0.8f + difficulty * 0.5f;
        double interval = 0.375;
        double xOffset = -interval * (count - 1) / 2;
        // 获取自身视线方向
        Vec3 lookDirection = this.getLookAngle();
        Vec3 perpendicular = new Vec3(-lookDirection.z, 0, lookDirection.x); // 垂直方向
        // 计算目标到自身的距离减1
        double distance = this.distanceTo(target) - 1;
        // 根据directionType计算起始位置（都基于目标位置）
        Vec3 centerPos;
        switch (direction) {
            // 前方：目标位置 + 视线方向 * (距离-1)
            case FRONT -> centerPos = target.position().add(lookDirection.scale(distance));
            // 后方：目标位置 - 视线方向 * (距离-1)
            case BACK -> centerPos = target.position().add(lookDirection.scale(-distance));
            // 左侧：目标位置 - 垂直方向 * (距离-1)
            case LEFT -> centerPos = target.position().add(perpendicular.scale(-distance));
            // 右侧：目标位置 + 垂直方向 * (距离-1)
            case RIGHT -> centerPos = target.position().add(perpendicular.scale(distance));
            // 默认前方
            default -> centerPos = target.position().add(lookDirection.scale(distance));
        }
        // 计算骨墙朝向（从起始位置朝向目标）
        Vec3 toTarget = target.position().subtract(centerPos).normalize();
        float yRot = (float) Math.atan2(toTarget.x, toTarget.z);
        for (int i = 0; i < count; i++) {
            Vec3 pos = centerPos.add(new Vec3(xOffset, 0, 0).yRot(yRot));
            GroundBoneProjectile bone = new GroundBoneProjectile(level, this, pos.x, this.getY(), pos.z, addHeight, 1f, speed, isAqua == 1 ? ColorAttack.AQUA : ColorAttack.WHITE);
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
     * 直线骨刺波动
     */
    private int summonStraightBoneSpineWave(@NotNull LivingEntity target, int count, ColorAttack color) {
        int difficulty = this.level().getDifficulty().getId();
        int delay = 25 - difficulty * 5;
        float yawRad = (this.getYHeadRot() + 90f) * Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(Mth.cos(yawRad), 0, Mth.sin(yawRad));
        int rows = ATTACK_RANGE * 2 + difficulty/3 * 5;
        int cols = 5;
        return summonGroundBoneSpineWaveMatrix(target, rows, cols, this.position(), direction, color, delay);
    }

    /**
     * 扇形骨刺波动 - 以自身为中心，扇形发射
     */
    private int summonFanBoneSpineWave(@NotNull LivingEntity target, int waveCount, float spreadAngle, ColorAttack color) {
        int difficulty = this.level().getDifficulty().getId();
        int baseDelay = 25 - difficulty * 5;
        Vec3 toTarget = target.position().subtract(this.position()).normalize();
        int rows = ATTACK_RANGE * 2 + difficulty/3 * 5;
        int cols = 5;
        int finalDelay = baseDelay;
        for (int i = 0; i < waveCount; i++) {
            // 计算当前波的角度偏移
            float angleOffset = (i / (float)(waveCount - 1) - 0.5f) * spreadAngle;
            Vec3 direction = toTarget.yRot(angleOffset * Mth.DEG_TO_RAD);
            int waveDelay = summonGroundBoneSpineWaveMatrix(target, rows, cols, this.position(), direction, color, baseDelay + i * 5);
            finalDelay = Math.max(finalDelay, waveDelay);
        }

        return finalDelay;
    }
    /**
     * 环形骨刺波动 - 以目标为圆心，从圆环上向目标发射
     */
    private int summonRingBoneSpineWave(@NotNull LivingEntity target, int waveCount, float spreadAngle, ColorAttack color) {
        int difficulty = this.level().getDifficulty().getId();
        int baseDelay = 25 - difficulty * 5;
        double radius = this.distanceTo(target);
        int rows = (int)(radius * 2); // 根据距离调整行数
        int cols = 5;
        int finalDelay = baseDelay;
        for (int i = 0; i < waveCount; i++) {
            // 计算圆环上的起始位置
            float angle = (i / (float)waveCount) * 360f + (this.tickCount * 2); // 可以加旋转
            Vec3 circlePos = target.position().add(
                    Math.cos(angle * Mth.DEG_TO_RAD) * radius,
                    0,
                    Math.sin(angle * Mth.DEG_TO_RAD) * radius
            );
            // 从圆环位置朝向目标
            Vec3 direction = target.position().subtract(circlePos).normalize();
            int waveDelay = summonGroundBoneSpineWaveMatrix(target, rows, cols, circlePos, direction, color, baseDelay + i * 3);
            finalDelay = Math.max(finalDelay, waveDelay);
        }

        return finalDelay;
    }
    /**
     * 公用方法：召唤定向骨刺矩阵
     */
    private int summonGroundBoneSpineWaveMatrix(LivingEntity target, int rows, int cols, Vec3 startPos, Vec3 direction, ColorAttack color, int delay) {
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
                        delay, 20, 0f, color, false);
            }
            delay += rows/3; // 多少行1延迟影响波浪长度
        }
        return delay;
    }
    /**
     * 骨刺波动组合
     */
    private int summonGroundBoneSpineWaveCombo(@NotNull LivingEntity target,int[] ...params) {
        int cd = 0;
        for (int[] param : params) {
            cd = Math.max(cd,summonGroundBoneSpineWave(target,param[0],param[1],param[2]));
        }
        return cd;
    }

    /**
     * 在自身位置召唤地面骨刺扩张
     */
    private int summonGroundBoneSpineAtSelf(@NotNull LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        Vec3 pos = this.position();
        int delay = 13 - difficulty;
        for(int i = 0; i < 13 + difficulty; i++) {
            int count = 8 * ( i + 1);
            float interval = 360f / count;
            float r = 0.6f * ( i +1);
            float angle = interval/2;
            for (int j = 0; j < count; j++,angle += interval) {
                // 计算骨刺的目标位置（以目标为中心的圆形）
                createGroundBone(attackTypeUUID,
                        pos.x + r * Mth.cos(angle * Mth.DEG_TO_RAD),
                        pos.z + r * Mth.sin(angle * Mth.DEG_TO_RAD),
                        minY, maxY, delay,20, 0f,ColorAttack.WHITE,false);
            }
            delay++;
        }
        return delay;
    }
    /**
     * 在目标脚下召唤地面骨刺
     */
    private int summonGroundBoneSpineAtTarget(@NotNull LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 pos = target.position();
        int radiusCount = 3 * (difficulty + 1);
        float offsetY = 1f - (float) difficulty / 3;
        float radiusSize = (radiusCount - 1) * 0.5f;
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        Level level = this.level();
        double groundY = EntityUtils.findGroundY(this.level(), pos.x, pos.z, minY, maxY);
        if(groundY != level.getMinBuildHeight()){
            PacketDistributor.sendToPlayersTrackingEntity(this,
                    new WarningTipPacket(pos.x - radiusSize,groundY,pos.z - radiusSize,pos.x + radiusSize,groundY + offsetY,pos.z + radiusSize,
                            10, Color.RED.getRGB()));
            return summonSquareGroundBoneSpine(target,radiusCount,pos.x,pos.z,13 - difficulty,10,offsetY);
        }
        return 0;
    }
    /**
     * 在目标周围随机区域召唤地面骨刺
     */
    private int summonGroundBoneSpineAroundTarget(@NotNull LivingEntity target) {
        int difficulty = this.level().getDifficulty().getId();
        Vec3 pos = target.position();
        return summonCircleGroundBoneSpine(target,3 + difficulty,pos.x + this.random.nextDouble() * ATTACK_RANGE,pos.z + this.random.nextDouble() * ATTACK_RANGE
                ,13 - difficulty,10 + this.random.nextInt(15),0f);
    }
    /**
     * 圆形骨刺
     * @param target 目标
     * @param layer 圆环层数
     * @param x 中心坐标x
     * @param z 中心坐标z
     * @param delay 延迟
     * @param lifetime 生命周期
     * @param offsetY 减少骨刺刺出的高度
     * @return 执行CD
     */
    private int summonCircleGroundBoneSpine(LivingEntity target,int layer,double x,double z,int delay,int lifetime,float offsetY){
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        createGroundBone(attackTypeUUID,x, z, minY, maxY,delay,lifetime, offsetY,ColorAttack.WHITE,true);
        for(int i = 0; i < layer; i++) {
            int count = 8 * ( i + 1);
            float interval = 360f / count;
            float r = 0.6f * ( i +1);
            float angle = interval/2;
            for (int j = 0; j < count; j++,angle += interval) {
                // 计算骨刺的目标位置（以目标为中心的圆形）
                createGroundBone(attackTypeUUID,
                        x + r * Mth.cos(angle * Mth.DEG_TO_RAD),
                        z + r * Mth.sin(angle * Mth.DEG_TO_RAD),
                        minY, maxY, delay,lifetime, offsetY,ColorAttack.WHITE,false);
            }
        }
        return delay;
    }
    /**
     * 实心正方形骨刺
     * @param target 目标
     * @param size 正方形半径（从中心到边的格数）
     * @param x 中心坐标x
     * @param z 中心坐标z
     * @param delay 延迟
     * @param lifetime 生命周期
     * @param offsetY 减少骨刺刺出的高度
     * @return 执行CD
     */
    private int summonSquareGroundBoneSpine(LivingEntity target, int size, double x, double z, int delay, int lifetime, float offsetY) {
        String attackTypeUUID = UUID.randomUUID().toString();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        // 生成整个实心正方形区域
        for (int i = -size; i <= size; i++) {
            for (int j = -size; j <= size; j++) {
                double boneX = x + i * 0.5f;
                double boneZ = z + j * 0.5f;
                boolean isCenter = (i == 0 && j == 0);
                createGroundBone(attackTypeUUID, boneX, boneZ, minY, maxY, delay, lifetime, offsetY, ColorAttack.WHITE, isCenter);
            }
        }
        return delay;
    }
    /**
     * @param offsetY 根据难度决定骨刺上升多少，最大为自身高度1.0f
     */
    private void createGroundBone(String attackUUID,double targetX, double targetZ, double minY, double maxY, int delay,int lifetime,float offsetY,ColorAttack colorAttack,boolean isPlaySound) {
        Level level = this.level();
        double spawnY = EntityUtils.findGroundY(level, targetX, targetZ, minY, maxY);
        // 如果找到有效地面，生成骨刺
        if (spawnY != level.getMinBuildHeight()) {
            GroundBone bone = new GroundBone(level, this, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), delay, targetX, spawnY  - offsetY, targetZ,colorAttack,isPlaySound,lifetime);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackUUID, (byte) 6));
            // 设置旋转：骨刺指向圆心（目标位置）
            bone.setYRot(this.getYHeadRot());
            level.addFreshEntity(bone);
            level.gameEvent(GameEvent.ENTITY_PLACE, new Vec3(targetX, spawnY, targetZ), GameEvent.Context.of(this));
            ((ServerLevel) level).sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,colorAttack.getColor().getColor()) ,targetX, spawnY, targetZ,
                    1,  // 数量
                    0, 0, 0,  // 偏移范围
                    0  // 速度
            );
        }
    }


    /**
     * 在自身周围随机位置召唤GB
     */
    private int summonAimedGBAroundSelf(LivingEntity target,int count){
        int difficulty = this.level().getDifficulty().getId();
        for(int i = 0; i < count; i++) {
            GasterBlasterFixed gb = createGBFixed();
            Vec3 targetEyePos = target.getEyePosition();
            // 召唤在自身周围攻击目标
            // 先创建单位方向向量
            Vec3 direction = new Vec3(0, 1, 0).zRot((this.random.nextFloat() * 180 - 90) * Mth.DEG_TO_RAD);
            gb.setPos(this.getEyePosition().add(
            // 偏移可能的位置
            new Vec3(direction.x * (this.random.nextDouble() - 0.5) * 12,  // 左右
                    direction.y * (this.random.nextDouble() * 3 + 3),    // 高度
                    this.random.nextDouble() * 5
            )  // 旋转至视线方向，形成视锥
                    .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                    .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
            ));
            gb.lookAt(EntityAnchorArgument.Anchor.FEET, targetEyePos);
            this.level().addFreshEntity(gb);
        }
        return 0;
    }
    /**
     * 以目标和自身长度为半径的圆环上召唤GB，固定360度范围，根据数量自动计算角度步长
     */
    private int summonGBAroundTarget(LivingEntity target,double offsetAngle,int count) {
        return summonGBAroundTarget(target, count, offsetAngle, 360.0 / count);
    }
    /**
     * 以目标和自身长度为半径的圆环上召唤GB
     * @param target 目标实体
     * @param count GB数量
     * @param offsetAngle 初始偏移角度（度）
     * @param angleStep 角度步长（度）
     */
    private int summonGBAroundTarget(LivingEntity target, int count, double offsetAngle, double angleStep) {
        int difficulty = this.level().getDifficulty().getId();
        double baseRadius = this.distanceTo(target) * 0.75f; // 以距离为基准半径
        double currentAngle = offsetAngle; // 从指定角度开始

        for(int i = 0; i < count; i++) {
            // 固定半径保持对称性
            double radius = baseRadius + (this.random.nextDouble() * 2.0 - 1.0);
            double height = this.random.nextDouble() * 3 + 1;
            GasterBlasterFixed gb = createGBFixed();
            Vec3 targetEyePos = target.getEyePosition();
            // 计算圆形上的位置
            double xOffset = Math.sin(currentAngle * Mth.DEG_TO_RAD) * radius;
            double zOffset = Math.cos(currentAngle * Mth.DEG_TO_RAD) * radius;
            LevelUtils.addFreshEntity(this.level(),gb,targetEyePos.add(xOffset, height, zOffset),targetEyePos);
            // 按照固定角度步长递增
            currentAngle += angleStep;
        }
        return 0;
    }



    private GasterBlasterFixed createGBFixed(){
        GasterBlasterFixed gb = new GasterBlasterFixed(EntityTypeRegistry.GASTER_BLASTER_FIXED.get(), this.level(), this);
        gb.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(UUID.randomUUID().toString(), (byte) 10));
        return gb;
    }

    private byte animId;
    @Override
    public byte getAnimID() {
        return animId;
    }

    @Override
    public void setAnimID(byte id) {
        this.animId = id;
    }
    private static final String ANIM_CHARGE_FRONT = "charge.front";
    private static final String ANIM_LURKER_CROSS = "attack.spine.cross";
    private static final String ANIM_LURKER_FRONT = "attack.spine.front";
    private static final String ANIM_BONE_PROJECTILE = "attack.bone.projectile";
    private static final String ANIM_CAST = "attack.cast";
    private static final String ANIM_CAST_LEFT = "attack.cast.left";
    private static final String ANIM_CAST_ROUND = "attack.cast.round";
    private static final String ANIM_CAST_ROUND_LEFT = "attack.cast.round.left";

    private final static String[] THROW_ANIM_NAMES = new String[]{"attack.throw.up","attack.throw.down","attack.throw.left","attack.throw.right","attack.throw.front","attack.throw.back"};

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Sans> attackController = new AnimationController<>(this,"attack", state -> {
            AnimationController<Sans> controller = state.getController();

            Sans animatable = state.getAnimatable();
            Difficulty difficulty = animatable.level().getDifficulty();
            byte animID = animatable.getAnimID();
            if(controller.hasAnimationFinished()){
                animatable.setAnimID((byte) 0);
            }
            if(animID == 0) {
                return PlayState.STOP;
            }
            switch (animID){
                case 1,4,5,6,7,8 -> controller.setAnimation(RawAnimation.begin().thenPlay(difficulty == Difficulty.HARD? ANIM_CAST:ANIM_CAST_LEFT));
                case 2 -> controller.setAnimation( RawAnimation.begin().thenPlay(ANIM_BONE_PROJECTILE));
                case 3 -> controller.setAnimation(RawAnimation.begin().thenPlay(difficulty == Difficulty.HARD? ANIM_CAST_ROUND:ANIM_CAST_ROUND_LEFT));
                case 9,10,11,12,13,14 -> controller.setAnimation( RawAnimation.begin().thenPlay(THROW_ANIM_NAMES[animID - 8]));
            }
            return PlayState.CONTINUE;
        });

        controllers.add(
                DefaultAnimations.genericWalkIdleController(this),
                DefaultAnimations.genericLivingController(this),
                attackController
        );
    }

}
