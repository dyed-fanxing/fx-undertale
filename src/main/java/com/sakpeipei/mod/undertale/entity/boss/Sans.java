package com.sakpeipei.mod.undertale.entity.boss;

import com.sakpeipei.mod.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.mod.undertale.entity.projectile.GroundBoneProjectile;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import com.sakpeipei.mod.undertale.entity.summon.GroundBone;
import com.sakpeipei.mod.undertale.mechanism.ColorAttack;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.mod.undertale.utils.RotUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;


public class Sans extends Monster implements NeutralMob, GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final Logger log = LoggerFactory.getLogger(Sans.class);
    private final RawAnimation THROW_UP_ANIM = RawAnimation.begin().thenLoop("throw.up");
    private final RawAnimation THROW_DOWN_ANIM = RawAnimation.begin().thenLoop("throw.down");
    private final RawAnimation THROW_LEFT_ANIM = RawAnimation.begin().thenLoop("throw.left");
    private final RawAnimation THROW_RIGHT_ANIM = RawAnimation.begin().thenLoop("throw.right");
    private final RawAnimation THROW_FRONT_ANIM = RawAnimation.begin().thenLoop("throw.front");
    private final RawAnimation THROW_BACK_ANIM = RawAnimation.begin().thenLoop("throw.back");
    private final RawAnimation CHARGE_FRONT_ANIM = RawAnimation.begin().thenLoop("charge.front");
    private final RawAnimation ATTACK_GB_LIFT_SWING_ANIM = RawAnimation.begin().thenLoop("attack.gb.lift.swing");
    private final RawAnimation ATTACK_GB_LIFT_CIRCLE_ANIM = RawAnimation.begin().thenLoop("attack.gb.lift.circle");
    private final RawAnimation ATTACK_LURKER_CROSS_ANIM = RawAnimation.begin().thenLoop("attack.lurker.cross");
    private final RawAnimation ATTACK_LURKER_FRONT_ANIM = RawAnimation.begin().thenLoop("attack.lurker.front");
    private final RawAnimation ATTACK_BONE_PROJECTILE_ANIM = RawAnimation.begin().thenLoop("attack.bone.projectile");
    private final RawAnimation ATTACK_BONE_ROTATE_ANIM = RawAnimation.begin().thenLoop("attack.bone.rotate");


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
    public void tick() {
        super.tick();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 远程攻击，需要实现performRangedAttack，然后通过goal去调用
        this.goalSelector.addGoal(1, new TransitionPhaseGoal());
        // 远程攻击，需要实现performRangedAttack，然后通过goal去调用
        this.goalSelector.addGoal(2, new MainAttackGoal(1.0, 16.0F));


        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomStrollGoal(this, 0.5f));

        this.targetSelector.addGoal(0, new LookforAngerGoal());
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                DefaultAnimations.genericWalkIdleController(this),
                DefaultAnimations.genericLivingController(this)
        );
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
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    class LookforAngerGoal extends Goal{
        public LookforAngerGoal() {
            super();
        }

        @Override
        public boolean canUse() {
            return Sans.this.getTarget() == null && Sans.this.getPersistentAngerTarget() != null;
        }

        @Override
        public void start() {
            if (Sans.this.level() instanceof ServerLevel level) {
                if(level.getEntity(Sans.this.getPersistentAngerTarget()) instanceof LivingEntity entity){
                    Sans.this.setTarget(entity);
                }
            }
        }
    }

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

    // 攻击步骤配置
    class AttackStepConfig{
        private final int type;
        private final int baseCD;
        private IntSupplier supplier;


        public AttackStepConfig(int type, int baseCD) {
            this.type = type;
            this.baseCD = baseCD;
        }
    }
    class GroundBoneProjectileStepConfig extends AttackStepConfig{
        int count;
        int speed;
        ColorAttack colorAttack;
        float addHeight; //增加的高度
        public GroundBoneProjectileStepConfig(int type, int baseCD) {
            super(type, baseCD);
        }
        public GroundBoneProjectileStepConfig(int type, int baseCD,int count,int speed, ColorAttack colorAttack, float addHeight) {
            super(type, baseCD);
            this.count = count;
            this.speed = speed;
            this.colorAttack = colorAttack;
            this.addHeight = addHeight;
        }
    }
    // 攻击任务接口
    @FunctionalInterface
    interface AttackAction {
        int execute(LivingEntity target);
    }
    class MainAttackGoal extends Goal {
        private int cd;
        private final double speedModifier;
        private int seeTime;
        private final float attackRadiusSqr;
        private final float backRadiusSqr;
        private final float pursuitRadiusSqr;
        private Vec3 lastTargetPos; //丢失视线后目标最后一次位置
        private List<AttackStepConfig> steps = new ArrayList<>(); //攻击步骤序列

        public MainAttackGoal(double speedModifier, float attackRadius) {
            this.cd = -1;
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
            globalCD = 40;
            // 必须要初始化lastTargetPos的位置
            // 因为如果第一次攻击，Sans传送至方块后方，没有视线，isContinueSee 和hasSeeSight 均是false
            // 那么就导致激怒后直接进入没有视线的分支，又因为在追击范围内，会向lastTargetPos移动，如果lastTargetPos是null，就会报错
            lastTargetPos = Sans.this.getTarget().position();
            Sans.this.setAggressive(true);
        }

        @Override
        public void stop() {
            this.seeTime = 0;
            this.cd = 0;
            this.steps.clear();
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
                boolean isContinueSee = this.seeTime > 0;
                if (hasSeeSight != isContinueSee) {
                    this.seeTime = 0;
                    if(!hasSeeSight) {
                        lastTargetPos = target.position();
                    }
                }
                if (hasSeeSight) {
                    ++this.seeTime;
                } else {
                    --this.seeTime;
                }
                Sans.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if(this.seeTime > 0){
                    //                    ++this.strafingTime;
                    if (disSqr <= backRadiusSqr){
                        Sans.this.getNavigation().stop();
                        Sans.this.getMoveControl().strafe(-0.75f,0.0f);
                        Sans.this.setYRot(Mth.rotateIfNecessary(Sans.this.getYRot(), Sans.this.yHeadRot, 0.0F));
                    }else if(disSqr > backRadiusSqr && disSqr <= attackRadiusSqr){
                        Sans.this.getNavigation().stop();
                    }else{
                        Sans.this.getNavigation().moveTo(target, speedModifier);
                        // 超出追击范围
                        if(disSqr > pursuitRadiusSqr){
                            // 计算从自身指向目标的向量
                            teleportTowards(target);
                        }
                    }

                    if(this.seeTime >= 20 && !Sans.this.isSpecial){
                        boolean onGround =  target.onGround();
                        boolean canFlying = target instanceof FlyingMob || target instanceof FlyingAnimal || target.hasEffect(MobEffects.LEVITATION);
                        boolean inAir = target.isFallFlying() || (!onGround && ( canFlying || target.onClimbable()));
                        boolean isEmpty = steps.isEmpty();
                        // 检查是否需要中断当前攻击（放在CD检查之前）
                        if(!isEmpty){
                            AttackStepConfig first = this.steps.getFirst();
                            if (first.type == 2) {
                                if((onGround && target.getOnPos().getY() != Sans.this.getOnPos().getY()) || inAir){
                                    int sum = 0;
                                    for (AttackStepConfig step : steps) {
                                        sum += step.baseCD;
                                    }
                                    Sans.this.globalCD = 120 - sum ;
                                    steps.clear();
                                }
                            }
                        }

                        int difficulty = Sans.this.level().getDifficulty().getId();
                        // 全局CD冷却结束，且当前步骤为-1，则选择新的攻击类型
                        if (Sans.this.globalCD == 0){
                            if(isEmpty){
                                cd = 0;
                                // 定义基础权重
                                int[] weights = {0, 0,1, 0,0, 0,0}; // [飞行骨一次性,飞行骨持续性, 地面运动骨, 地刺波动骨, 地刺召唤骨, Gaster炮阵列, 重力控制]
                                if(inAir){
                                    weights[5] = 5;
                                }else{
//                                    weights[3] = 3; // 激活地刺权重
                                    // 根据条件激活权重
                                    if(target.getOnPos().getY() == Sans.this.getOnPos().getY()){
                                        weights[2] = 3; // 激活地面移动骨头权重
                                    }
                                }
                                if(Sans.this.physicalStrength <= maxPhysicalStrength / 2 ) {
                                    weights[6] = 5;
                                }
                                // 计算总权重
                                int totalWeight = weights[0] + weights[1] + weights[2] + weights[3];

                                // 权重随机选择
                                int random = Sans.this.random.nextInt(totalWeight);
                                int current = 0;
                                for (int i = 0; i < weights.length; i++) {
                                    current += weights[i];
                                    if (random < current) {
                                        for (int j = 0; j < 3 + 2 * (difficulty - 1); j++) {
                                            steps.add(new GroundBoneProjectileStepConfig(i,30,3,1,ColorAttack.AQUA,1.0f));
                                        }
                                        break;
                                    }
                                }
                            }
                            if(cd-- == 0){
                                // 统一的攻击执行
                                AttackStepConfig step = steps.removeFirst();
                                switch (step.type) {
                                    case 0 -> {
                                        cd = Sans.this.continueFlyingBone(target,difficulty) + step.baseCD - 10 * difficulty ;
                                    }
                                    case 1 -> {
                                        cd = Sans.this.onceFlyingBone(target,difficulty) + step.baseCD - 10 * difficulty ;
                                    }
                                    case 2 -> {
                                        float speed = Sans.this.random.nextFloat() * 0.4f + difficulty * 0.5f;
                                        GroundBoneProjectileStepConfig gbpStep = (GroundBoneProjectileStepConfig) step;
                                        Sans.this.groundBoneProjectileAttack(target, gbpStep.count, speed,gbpStep.colorAttack,gbpStep.addHeight);
                                        cd = step.baseCD - 10 * difficulty;
                                    }
                                    case 3 -> {
                                        // 其他攻击
                                        cd = 25;
                                    }
                                    case 4 -> {
                                        Sans.this.gbAttack(target, 5, 0);
                                        cd = 40;
                                    }
                                }
                                if(steps.isEmpty()) {
                                    Sans.this.globalCD = 120;
                                }
                            }
                        }
                    }
                }else{
                    if(disSqr <= pursuitRadiusSqr){
                        Sans.this.getNavigation().moveTo(lastTargetPos.x,lastTargetPos.y,lastTargetPos.z,this.speedModifier);
                    }else{
                        teleportTowards(target);
                    }
                }
            }
        }
    }
    /**
     * 飞行骨一次性射击
     * @return 需要执行完攻击的总tick
     */
    private int onceFlyingBone(LivingEntity target, int difficulty) {
        float speed = this.random.nextFloat() * 0.2f + difficulty * 0.5f + 0.5f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay = 10;
        Vec3 centerPos = new Vec3(this.getX(),this.getY(0.5f),this.getZ());
        switch (this.random.nextInt(3)) {
            // 圆
            case 0 -> {
                // 根据难度确定层数和每层数量
                for (int layer = 0; layer < 3; layer++) {
                    // 当前层的数量和半径
                    int count = (5 + difficulty) * (layer + 1);
                    float radius = 0.4f + layer * 0.3f;
                    int angleStep = 360 / count;
                    int layerOffset = angleStep / 2 * layer;
                    for (int i = 0; i < count; i++) {
                        FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
                        // 计算当前角度，层偏移角度 实现错位
                        int angle = i * angleStep + layerOffset;
                        Vec3 pos = centerPos.add(new Vec3(
                                radius * Mth.cos(angle * Mth.DEG_TO_RAD),
                                radius * Mth.sin(angle * Mth.DEG_TO_RAD),
                                (2-layer)*0.5)
                                .xRot(-this.getXRot() * Mth.DEG_TO_RAD).yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD));
                        bone.absMoveTo(pos.x, pos.y, pos.z);

                        // 射击逻辑
                        Vec3 movement = target.getEyePosition().subtract(centerPos);
                        bone.setXRot(RotUtils.shootXRot(movement));
                        bone.setYRot(RotUtils.shootYRot(movement));

                        bone.delayShoot(delay, bone.getEyePosition().add(movement));
                        this.level().addFreshEntity(bone);
                    }
                    delay += 6 - difficulty;
                }
            }
            // 长方形
            case 1 -> {
                for (int layer = 0; layer < 3; layer++) {
                    int cols = 3 * (layer * 2 + 1) + difficulty;
                    float startX = -(cols - 1) * 0.2f;
                    float startY = -layer * 0.3f;
                    for (int row = 0; row < layer + 1; row++) {
                        for (int col = 0; col < cols; col++) {
                            FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
                            Vec3 pos = centerPos.add(new Vec3(startX + col * 0.4f, startY + row * 0.6f, 1)
                                    .xRot(-this.getXRot() * Mth.DEG_TO_RAD).yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD));
                            bone.absMoveTo(pos.x, pos.y, pos.z);
                            Vec3 movement = target.getEyePosition().subtract(centerPos);
                            bone.setXRot(RotUtils.shootXRot(movement));
                            bone.setYRot(RotUtils.shootYRot(movement));

                            bone.delayShoot(delay, bone.getEyePosition().add(movement));
                            this.level().addFreshEntity(bone);
                        }
                    }
                    delay += 6 - difficulty;
                }
            }
            // 倒三角
            case 2 -> {
                for (int layer = 0; layer < 3; layer++) {
                    for (int repeat = 0; repeat < layer + 1; repeat++) {
                        int rows = 2 + difficulty;
                        float startY = - (rows - 1) * 0.15f;
                        float offset = (repeat - layer * 0.5f) * (rows * 0.375f + 0.375f);
                        // 奇数层：正三角形（顶点在上，底边在下）
                        for (int row = 0; row < rows; row++) {
                            int cols = row + 1; // 正：倒
                            float startX = -(cols - 1) * 0.1875f + offset;
                            for (int col = 0; col < cols; col++) {
                                FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
                                Vec3 pos = centerPos.add(new Vec3(startX + col * 0.375f, startY + row * 0.3f, 1)
                                        .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                                        .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD));
                                bone.absMoveTo(pos.x, pos.y, pos.z);

                                Vec3 movement = target.getEyePosition().subtract(centerPos);
                                bone.setXRot(RotUtils.shootXRot(movement));
                                bone.setYRot(RotUtils.shootYRot(movement));
                                bone.delayShoot(delay, bone.getEyePosition().add(movement));
                                this.level().addFreshEntity(bone);
                            }
                        }
                    }
                    delay += 6 - difficulty;
                }
            }
        }
        return delay;
    }
    /**
     * 飞行骨持续射击
     * @return 需要执行完攻击的总tick
     */
    private int continueFlyingBone(LivingEntity target, int difficulty) {
        float speed = this.random.nextFloat() * 0.2f + difficulty * 0.5f + 0.5f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay = 10;
        switch (this.random.nextInt(2)) {
            case 0 -> {
                int count = 6 * difficulty; // 模式0的count
                for (int i = 0; i < count; i++) {
                    FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
                    do {
                        Vec3 pos = this.position().add(
                                new Vec3((this.random.nextDouble() - 0.5) * 12,
                                        this.random.nextDouble() * 3 + this.getBbHeight() * 0.5f,
                                        this.random.nextDouble() * 3
                                )
                                        .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                                        .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                        );
                        bone.absMoveTo(pos.x, pos.y, pos.z);
                    } while (!bone.level().noCollision(bone, bone.getBoundingBox()));

                    RotUtils.lookAtByShoot(bone, target);
                    bone.delayTrackShoot(delay);
                    delay += 6 - difficulty;
                    this.level().addFreshEntity(bone);
                }
            }
            case 1 -> {
                int count = 6 * difficulty;
                float avg = 180f / (count - 1);
                for (int i = 0; i < count; i++) {
                    FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
                    // 椭圆参数方程，从-90度到90度（上半椭圆）
                    float angle =  i * avg;
                    float r = 1.3f + difficulty * 0.1f;
                    // 椭圆上的点：x = a*cos(θ), y = b*sin(θ)
                    Vec3 pos = new Vec3(getX(),getY(0.5f),getZ()).add(
                            new Vec3( r * Mth.cos(angle * Mth.DEG_TO_RAD), r * Mth.sin(angle * Mth.DEG_TO_RAD), 0)
                                    .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                                    .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                    );
                    bone.absMoveTo(pos.x, pos.y, pos.z);

                    RotUtils.lookAtByShoot(bone, target);
                    bone.delayTrackShoot(delay);
                    delay += 6 - difficulty;
                    this.level().addFreshEntity(bone);
                }
            }
        }
        return delay;

    }
    private FlyingBone createFlyingBone(float speed, String attackTypeUUID) {
        FlyingBone bone = new FlyingBone(EntityTypeRegistry.FLYING_BONE.get(), this.level(), this, 1f, speed);
        bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
        return bone;
    }

    /**
     * 地面骨直线运动攻击
     * @param count 基础倍数
     * @param speed 速度
     * @param colorAttack 颜色攻击
     * @param addHeight 增加高度
     */
    public void groundBoneProjectileAttack(@NotNull LivingEntity target,int count,float speed,ColorAttack colorAttack,float addHeight) {
        Level level = this.level();
        String attackTypeUUID = UUID.randomUUID().toString();
        count = count * 3;
        Vec3 position = this.position();
        double interval = 0.375;
        double xOffset = -interval * (count - 1) / 2;
        for (int i = 0; i < count; i++) {
            Vec3 pos = position.add(new Vec3(xOffset, 0, 1f)
                    .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
            );
            GroundBoneProjectile bone = new GroundBoneProjectile(level, this,addHeight, 1f, speed,pos.x, this.getY(), pos.z,colorAttack);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
            Vec3 motion = target.position().subtract(new Vec3(position.x, target.getY() ,position.z));
            bone.delayShoot(10, motion);
            bone.setYRot(RotUtils.shootYRot(motion));
            level.addFreshEntity(bone);
            xOffset += interval;
        }
    }

    /**
     * 从自身发出地面骨刺 - 6格内圆形，6格外直线
     */
    public void groundBoneSpursAttack(@NotNull LivingEntity target, int count, int delay) {
        String attackTypeUUID = UUID.randomUUID().toString();
        Vec3 position = this.position();
        Vec3 targetPos = target.position();

        double distance = position.distanceToSqr(targetPos);
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        float baseAngle = (float)Mth.atan2(targetPos.z - position.z, targetPos.x - position.x) * Mth.RAD_TO_DEG;

        for (int i = 0; i < count; i++) {
            // 根据距离选择攻击模式
            double targetX = position.x, targetZ = position.z;
            float boneRotation;

            if (distance <= 36.0) {
                // 圆形模式
                float angle = i * (360.0f / count) * Mth.DEG_TO_RAD;
                targetX += Mth.cos(angle) * 3.0;
                targetZ += Mth.sin(angle) * 3.0;
                boneRotation = i * (360.0f / count) + 90.0f;
            } else {
                // 直线模式
                float distanceFromMob = 2.0f + i * 2.0f;
                targetX += Mth.cos(baseAngle * Mth.DEG_TO_RAD) * distanceFromMob;
                targetZ += Mth.sin(baseAngle * Mth.DEG_TO_RAD) * distanceFromMob;
                boneRotation = baseAngle + 90.0f;
            }

            createGroundBone(targetX, targetZ, minY, maxY, boneRotation, delay, attackTypeUUID);
        }
    }

    /**
     * 在目标脚下直接生成地面骨刺 - 圆形生成
     */
    public void groundBoneAreaSpursAttack(@NotNull LivingEntity target, int count, float radius, int delay) {
        String attackTypeUUID = UUID.randomUUID().toString();
        Vec3 targetPos = target.position();

        // 计算施法者和目标的高度范围（参考幻魔者设计）
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;

        // 计算每个骨刺的角度间隔（完整圆形）
        float angleIncrement = 360.0f / count;

        for (int i = 0; i < count; i++) {
            // 计算当前骨刺的角度（均匀分布在360度圆环上）
            float angle = i * angleIncrement;
            float radian = angle * Mth.DEG_TO_RAD;

            // 计算骨刺的目标位置（以目标为中心的圆形）
            double targetX = targetPos.x + Mth.cos(radian) * radius;
            double targetZ = targetPos.z + Mth.sin(radian) * radius;

            // 使用地面检测方法生成骨刺
            createGroundBone(targetX, targetZ, minY, maxY, angle, delay, attackTypeUUID);
        }
    }

    private void createGroundBone(double targetX, double targetZ, double minY, double maxY, float rotation, int delay, String attackUUID) {
        Level level = this.level();
        // 从最高Y坐标开始搜索地面
        BlockPos searchPos = BlockPos.containing(targetX, maxY, targetZ);
        boolean foundValidGround = false;
        double collisionHeight = 0.0;
        // 向下搜索直到找到固体地面
        do {
            BlockPos posBelow = searchPos.below();
            BlockState blockBelow = level.getBlockState(posBelow);
            // 检查下方方块的上表面是否坚固（可以站立）
            if (blockBelow.isFaceSturdy(level, posBelow, Direction.UP)) {
                // 如果当前位置有方块，计算其碰撞箱高度
                if (!level.isEmptyBlock(searchPos)) {
                    BlockState blockAtPos = level.getBlockState(searchPos);
                    VoxelShape collisionShape = blockAtPos.getCollisionShape(level, searchPos);
                    if (!collisionShape.isEmpty()) {
                        collisionHeight = collisionShape.max(Direction.Axis.Y);
                    }
                }
                foundValidGround = true;
                break;
            }
            // 继续向下搜索
            searchPos = searchPos.below();
        } while(searchPos.getY() >= Mth.floor(minY) - 1);
        // 如果找到有效地面，生成骨刺
        if (foundValidGround) {
            double spawnY = searchPos.getY() + collisionHeight;
            // 创建骨刺实体
            GroundBone bone = new GroundBone(level, this, 1f, 1f, delay, targetX, spawnY, targetZ);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackUUID, (byte) 6));
            // 设置旋转：骨刺指向圆心（目标位置）
            bone.setYRot(rotation);
            level.addFreshEntity(bone);
            level.gameEvent(GameEvent.ENTITY_PLACE, new Vec3(targetX, spawnY, targetZ), GameEvent.Context.of(this));
        }
    }

    public void gbAttack(LivingEntity target,int count,int type){
        String attackTypeUUID = UUID.randomUUID().toString();
        int angle = 0;
        int avg = 0;
        if(count == 1) {
            angle= this.random.nextInt() * 360;
        }else{
            avg = 180 / (count - 1);
        }
        for(int i = 0; i < count; i++) {
            GasterBlasterFixed gb = new GasterBlasterFixed(EntityTypeRegistry.GASTER_BLASTER_FIXED.get(), this.level(), this);
            gb.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 10));
            Vec3 targetEyePos = target.getEyePosition();
            switch (type) {
                // 召唤在自身周围攻击目标
                case 0 -> {
                    // 先创建单位方向向量
                    Vec3 direction = new Vec3(0, 1, 0)
                            .zRot((this.random.nextFloat() * 180 - 90) * Mth.DEG_TO_RAD);
                    gb.setPos(this.getEyePosition().add(
//                             偏移可能的位置
                            new Vec3(direction.x * (this.random.nextDouble() - 0.5) * 12,  // 左右
                                    direction.y * (this.random.nextDouble() * 3 + 3),    // 高度
                                    this.random.nextDouble() * 5
                            )
                                    // 旋转至视线方向，形成视锥
                                    .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                                    .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                    ));
                    angle += avg;
                }
                // 召唤在目标周围攻击目标
                case 1 -> {
                    double radius = this.random.nextDouble() * 4.0 + (double) ATTACK_RANGE / 2; // 半径
                    double height = this.random.nextDouble() * 4; // 0 - 4格随机高度
                    gb.setPos(targetEyePos.add(Math.sin(angle * Mth.DEG_TO_RAD) * radius, height, Math.cos(angle * Mth.DEG_TO_RAD) * radius));
                    angle += avg;
                }
            }
            gb.lookAt(EntityAnchorArgument.Anchor.FEET, targetEyePos);
            this.level().addFreshEntity(gb);
        }
    }


    class GasterBlasterAttackRoundGoal extends AttackRoundGoal{
        private int offHandCD = 100;
        public GasterBlasterAttackRoundGoal(int round, int cd, int count) {
            super(round, cd, count);
        }

        @Override
        public boolean canUse() {
            return Sans.this.physicalStrength > Sans.this.maxPhysicalStrength / 2;
        }


        @Override
        protected void execute(LivingEntity target) {
            Sans.this.gbAttack(target,5,0);
        }

    }

    class AttackRoundGoal extends Goal {
        protected int round;
        protected int count;
        protected int cd;
        public AttackRoundGoal(int round,int cd,int count) {
            this.round = round;
            this.cd = cd;
            this.count = count;
        }

        @Override
        public boolean canUse() {
            return round > 0;
        }

        @Override
        public void tick() {
            if(--cd ==0) {
                if(--round > 0 ){
                    LivingEntity target = Sans.this.getTarget();
                    execute(target);
                }
            }
        }

        protected void execute(LivingEntity target) {
        }
    }
}
