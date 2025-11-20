package com.sakpeipei.mod.undertale.entity.boss;

import com.sakpeipei.mod.undertale.entity.AnimID;
import com.sakpeipei.mod.undertale.entity.attachment.GravityData;
import com.sakpeipei.mod.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.mod.undertale.entity.common.AttackComboType;
import com.sakpeipei.mod.undertale.entity.common.AttackUnit;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.mod.undertale.entity.projectile.GroundBoneProjectile;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import com.sakpeipei.mod.undertale.entity.summon.GroundBone;
import com.sakpeipei.mod.undertale.mechanism.ColorAttack;
import com.sakpeipei.mod.undertale.network.WarningTipPacket;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import com.sakpeipei.mod.undertale.registry.SoundRegistry;
import com.sakpeipei.mod.undertale.utils.EntityUtils;
import com.sakpeipei.mod.undertale.utils.RotUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
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
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Sans extends Monster implements NeutralMob, GeoEntity, AnimID {
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
    public void tick() {
        super.tick();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0,new GravityControlCollisionDetectionGoal());
        // 远程攻击，需要实现performRangedAttack，然后通过goal去调用
        this.goalSelector.addGoal(1, new TransitionPhaseGoal());
        // 远程攻击，需要实现performRangedAttack，然后通过goal去调用
        this.goalSelector.addGoal(1, new AttackExecutorGoal());
        this.goalSelector.addGoal(2, new AttackSelectorGoal(1.0, 16.0F));


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
    private int mainAttack;

    class TransitionPhaseGoal extends Goal{
        public TransitionPhaseGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return physicalStrength == maxPhysicalStrength / 2;
        }
    }

    private final AttackComboType[] attackTypes = {
            new AttackComboType(new AttackUnit[]{ // 飞行骨持续攻击
                    new AttackUnit(1, 30, new int[]{4} )
            }),
            new AttackComboType(new AttackUnit[]{  // 飞行骨一次性攻击
                    new AttackUnit(2, 30, new int[]{10},  0),
                    new AttackUnit(2, 30, new int[]{10},  1),
                    new AttackUnit(2, 30, new int[]{10}, 2)
            }),
            new AttackComboType(new AttackUnit[]{ // 地面骨运动
                    new AttackUnit(3, 20, new int[]{10}, true,  0, 0, 0),
                    new AttackUnit(3, 40, new int[]{10}, false, 1, 1, 0)
            }),
            new AttackComboType(new AttackUnit[]{ // 骨刺波动，朝向目标
                    new AttackUnit(4, 0, new int[]{10}, true, 0, 1, 1),
                    new AttackUnit(4, 30, new int[]{10}, false,  0, 2, 1)
            }),
            new AttackComboType(new AttackUnit[]{ // 骨刺波动，自身周围
                    new AttackUnit(5, 30, new int[]{10}, true,  0, 1, 1)
            }),
            new AttackComboType(3, new AttackUnit[]{ // 目标骨刺
                    new AttackUnit(6, 30, new int[]{10}, true),
                    new AttackUnit(6, 30, new int[]{10}, false)
            }),
            new AttackComboType(new AttackUnit[]{  // 目标周围随机骨刺
                    new AttackUnit(7, 30, new int[]{10})
            }),
            new AttackComboType(new AttackUnit[]{ // GB炮阵列
                    new AttackUnit(8, 30, new int[]{10})
            }),
            new AttackComboType(new AttackUnit[]{ // 重力控制
                    new AttackUnit(9, 30, new int[]{10})
            })
    };
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

    class AttackSelectorGoal extends Goal {
        private final double speedModifier;
        private final float attackRadiusSqr;
        private final float backRadiusSqr;
        private final float pursuitRadiusSqr;
        private int seeTime;

        private int showFlag = 0; // 教学演示攻击

        public AttackSelectorGoal(double speedModifier, float attackRadius) {
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

                Sans.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
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
                        return;
                    }

                    if(this.seeTime >= 20 && !Sans.this.isSpecial){
                        boolean onGround =  target.onGround();
                        boolean canFlying = target instanceof FlyingMob || target instanceof FlyingAnimal || target.hasEffect(MobEffects.LEVITATION);
                        boolean inAir = target.isFallFlying() || (!onGround && ( canFlying || target.onClimbable()));
                        // 全局CD冷却结束，没有执行的攻击序列，则选择新的攻击序列，触发动画
                        if (Sans.this.globalCD == 0 && Sans.this.mainAttack == 0){
                            int showFlag0 = 0;
                            List<Integer> availableList = new ArrayList<>(stateSequences.getFirst());
                            showFlag0 |= 7;
                            if(onGround){
                                showFlag0 |= 8; // 激活地刺
                                availableList.addAll(stateSequences.get(1));
                                if(target.getOnPos().getY() == Sans.this.getOnPos().getY()){
                                    showFlag0 |= 16;  // 激活地面移动骨头
                                    availableList.addAll(stateSequences.get(2));
                                }
                            }
                            if(inAir || Sans.this.physicalStrength <= maxPhysicalStrength / 2 ) {
                                showFlag0 |= 32; // 激活重力控制
                                availableList.addAll(stateSequences.get(3));
                            }
                            if(showFlag < showFlag0){
                                // 找到第一个不同的位（第一个可用的未展示攻击）
                                int diff = showFlag ^ showFlag0;
                                Sans.this.mainAttack = Integer.numberOfTrailingZeros(diff);
                                showFlag |= (1 << Sans.this.mainAttack);
                            }else{
                                // 没有教学序列就用标记数组来随机选
                                Sans.this.mainAttack = availableList.get(Sans.this.random.nextInt(availableList.size()));
                            }
                            Sans.this.mainAttack = 5;
                        }
                    }
                }else if(seeTime == 0){
                    Vec3 pos = target.position();
                    if(disSqr <= pursuitRadiusSqr){
                        Sans.this.getNavigation().moveTo(pos.x,pos.y,pos.z,this.speedModifier);
                    }else{
                        teleportTowards(target);
                    }
                }
            }
        }
    }
    private class AttackExecutorGoal extends Goal{
        private int animTick;
        private int step;
        private int cd;
        private int round;
        public AttackExecutorGoal() {
            super();
        }

        @Override
        public void start() {
            animTick = 0;
            step = 0;
            cd = -1;
            round = attackTypes[Sans.this.mainAttack].getRound();
        }

        @Override
        public void stop() {
            cd = -1;
        }

        @Override
        public boolean canUse() {
            return Sans.this.mainAttack != 0;
        }

        @Override
        public void tick() {
            LivingEntity target = Sans.this.getTarget();
            if (target == null) {
                return;
            }
            log.info("animTick:{},mainAttack:{}",animTick,Sans.this.mainAttack);
            AttackComboType attackType = Sans.this.attackTypes[Sans.this.mainAttack];
            AttackUnit[] steps = attackType.getSteps();
            if(step < steps.length){
                if(animTick == 0) {
                    if(steps[step].isTriggerAnim()){
                        if (steps[step].getId() == 8) {
                            int direction = Sans.this.random.nextInt(6);
                            steps[step].setParams(direction);
                            Sans.this.sendAnimId((byte) (mainAttack + direction));
                        } else {
                            Sans.this.sendAnimId((byte) mainAttack);
                        }
                    }
                }
                animTick++;
                int[] hitTick = steps[step].getHitTick();
                for (int i : hitTick) {
                    if(animTick == i) {
                        do{
                            cd = executeAttack(target, steps[step]);
                            step++;
                        }while( cd <=0 );
                    }
                }
                if(cd-- == 0){
                    animTick = 0;
                }
            }else{
                round--;
                if(round == 0 ){
                    Sans.this.globalCD = 120;
                    Sans.this.mainAttack = 0;
                }
                animTick = 0;
            }
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
            }else return false;
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

    private int executeAttack(LivingEntity target,AttackUnit unit){
        int difficulty = this.level().getDifficulty().getId();
        int[] params = unit.getParams();
        return switch (unit.getId()) {
            case 1 -> Sans.this.continueFlyingBone(target, difficulty) + unit.getCd() ;
            case 2 -> Sans.this.onceFlyingBone(target, difficulty, params[0]) + unit.getCd();
            case 3 -> {
                Sans.this.groundBoneProjectileAttack(target, difficulty, params[0], params[1], params[2]);
                yield unit.getCd() - (params[0] == 0 ? difficulty * 5 : difficulty * 10);
            }
            case 4 -> {
                // 需要播放音效的那次攻击
                if(unit.getCd() == 0){
                    level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
                }
                yield Sans.this.groundBoneWaveSpineTargetAttack(target, difficulty, params[0], params[1], params[2]) + unit.getCd();
            }
            case 5 -> {
                // 需要播放音效的那次攻击
                level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
                yield Sans.this.selfSpikeAttack(target, difficulty) + unit.getCd();
            }
            case 6 -> {
                level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.ENEMY_ENCOUNTER_ATTACK_TIP.get(), SoundSource.HOSTILE);
                yield  unit.getCd() + Sans.this.targetSpineAttack(target, difficulty);
            }
            case 7 -> unit.getCd() + Sans.this.randomAreaSpineAttack(target, difficulty);
            case 8 -> {
                Sans.this.gbAttack(target, difficulty, params[0]);
                yield  unit.getCd() ;
            }
            case 9 -> {
                GravityData.applyRelativeGravity(Sans.this, target, GravityData.RelativeDirection.values()[params[0]]);
                yield  unit.getCd();
            }
            default -> throw new IllegalStateException("Unexpected value: " + unit.getId());
        };
    }

    /**
     * 飞行骨持续射击
     * @return 需要执行完攻击的总tick
     */
    private int continueFlyingBone(LivingEntity target, int difficulty) {
        String attackTypeUUID = UUID.randomUUID().toString();
        float speed = 1.0f + 0.333f * difficulty;
        int delay = 14 - 2*difficulty;
        int count = 7 * difficulty; // 模式0的count
        for (int i = 0; i < count; i++) {
            FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
            do {
                Vec3 pos = this.position().add(
                        new Vec3((this.random.nextDouble() - 0.5) * ( 12 * difficulty),
                                this.random.nextDouble() * (2 + difficulty) + this.getBbHeight() * 0.5f,
                                this.random.nextDouble() * (3 + difficulty)
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
        return delay;
    }
    /**
     * 飞行骨一次性射击
     * @return 需要执行完攻击的总tick
     */
    private int onceFlyingBone(LivingEntity target, int difficulty,int type) {
        float speed = this.random.nextFloat() * 0.2f + difficulty * 0.5f + 1.0f;
        String attackTypeUUID = UUID.randomUUID().toString();
        int delay = 10;
        Vec3 centerPos = new Vec3(this.getX(),this.getY(0.5f),this.getZ());
        type = type == -1 ? this.random.nextInt(3):type;
        switch (type) {
            // 圆
            case 0 -> {
                // 根据难度确定层数和每层数量
                for (int layer = 0; layer < 3; layer++) {
                    // 当前层的数量和半径
                    int count = (5 + difficulty) * (layer + 1);
                    float radius = 0.4f + layer * 0.3f;
                    float interval = 360f / count;
                    float layerOffset = interval / 2 * layer;
                    for (int i = 0; i < count; i++) {
                        FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
                        // 计算当前角度，层偏移角度 实现错位
                        float angle = i * interval + layerOffset;
                        Vec3 pos = centerPos.add(new Vec3(
                                radius * Mth.cos(angle * Mth.DEG_TO_RAD),
                                radius * Mth.sin(angle * Mth.DEG_TO_RAD),
                                (2 - layer) * 0.5)
                                .xRot(-this.getXRot() * Mth.DEG_TO_RAD).yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                        );
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
            // 椭圆
            case 1 -> {
                for (int layer = 0; layer < 3; layer++) {
                    int count = 5 + difficulty*2;
                    float interval = 180f / (count - 1);
                    float rX = 0.8f + difficulty*0.1f;
                    float rZ = 1f+difficulty * 0.1f;
                    float rotate = 0f;
                    int l = 1;
                    if(layer == 0){
                        rotate = 90f;
                    }else if(layer == 2){
                        l = 2;
                        rotate = 45f;
                    }
                    float angle = 0;
                    for (int j = 0; j < l; j++,rotate =-rotate) {
                        for (int i = 0; i < count; i++,angle+=interval) {
                            FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
                            Vec3 pos = centerPos.add(new Vec3(
                                            rX * Mth.cos(angle * Mth.DEG_TO_RAD),
                                            0,
                                            rZ* Mth.sin(angle * Mth.DEG_TO_RAD)
                                    ).zRot(rotate * Mth.DEG_TO_RAD)
                                            .xRot(-this.getXRot() * Mth.DEG_TO_RAD).yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                            );
                            bone.absMoveTo(pos.x, pos.y, pos.z);
                            // 射击逻辑
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
            case 3 -> {
                int count = 6 * difficulty;
                float interval = 180f / (count - 1);
                float angle =0;
                for (int i = 0; i < count; i++,angle+=interval) {
                    FlyingBone bone = createFlyingBone(speed, attackTypeUUID);
                    // 椭圆参数方程，从-90度到90度（上半椭圆）
                    float r = 1.3f + difficulty * 0.1f;
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
     * 地面骨墙直线运动攻击
     */
    private void groundBoneProjectileAttack(@NotNull LivingEntity target,int difficulty,int isAqua,float addHeight,int type) {
        Level level = this.level();
        String attackTypeUUID = UUID.randomUUID().toString();
        int count = (difficulty + isAqua ) * 3;
        float speed = 0.8f + difficulty * 0.5f;
        double interval = 0.375;
        double xOffset = -interval * (count - 1) / 2;
        Vec3 centerPos;
        float yRot;
        if (type == 0) {
            centerPos = this.position();
            yRot = -this.getYHeadRot() * Mth.DEG_TO_RAD;
        } else {
            centerPos = target.position().add(new Vec3(16f, 0, 0));
            yRot = 90f * type * Mth.DEG_TO_RAD;
        }
        for (int i = 0; i < count; i++) {
            Vec3 pos = centerPos.add(new Vec3(xOffset, 0, 1f).yRot(yRot));
            GroundBoneProjectile bone = new GroundBoneProjectile(level, this,pos.x, this.getY(), pos.z,addHeight, 1f, speed,isAqua == 1?ColorAttack.AQUA:ColorAttack.WHITE);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
            Vec3 motion = target.position().subtract(new Vec3(centerPos.x, target.getY() ,centerPos.z));
            bone.delayShoot(10, motion);
            bone.setYRot(RotUtils.shootYRot(motion));
            level.addFreshEntity(bone);
            xOffset += interval;
        }
    }

    /**
     * 地面骨向目标直线波动攻击
     */
    private int groundBoneWaveSpineTargetAttack(@NotNull LivingEntity target, int difficulty,int count,int type, int isAqua ) {
        int delay = 25 - difficulty * 5,iDelay = 10;
        String attackTypeUUID = UUID.randomUUID().toString();
        Vec3 position = this.position();
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        ColorAttack colorAttack = isAqua == 0 ? ColorAttack.WHITE : ColorAttack.AQUA;

        int rows = ATTACK_RANGE * 2 + difficulty/3 * 5;
        int cols = 5;
        float colSpacing = 0.375f;
        float rowSpacing = 0.6f;

        float[] colOffsets = new float[cols];
        float centerOffset = (cols - 1) * 0.5f;
        for (int col = 0; col < cols; col++) {
            colOffsets[col] = (col - centerOffset) * colSpacing;
        }
        for (int i = 0; i < count; i++) {
            iDelay = delay;
            Vec3 startPos;
            // 使用yHeadRot的角度来计算固定方向
            double xUnit,zUnit,perpX,perpZ;  // 前进方向的X,Z分量，垂直X,Z分量
            float offset = (i - (count - 1) * 0.5f) * (colSpacing * (cols - 1) + 2.1f);
            startPos = position.add(new Vec3(offset, 0, 0).yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD));
            if (type == 1) {
                float yawRad = (this.getYHeadRot() + 90f) * Mth.DEG_TO_RAD;
                xUnit = Mth.cos(yawRad);  // 前进方向的X分量
                zUnit = Mth.sin(yawRad);  // 前进方向的Z分量
            }else{
                Vec3 attackDir = target.position().subtract(startPos);
                double horDis = attackDir.horizontalDistance();
                xUnit = attackDir.x / horDis;
                zUnit = attackDir.z / horDis;
            }
            perpX = -zUnit;
            perpZ = xUnit;
            // 生成攻击矩阵
            for (int row = 0; row < rows; row++) {
                double baseX = startPos.x + row * rowSpacing * xUnit;
                double baseZ = startPos.z + row * rowSpacing * zUnit;
                for (int col = 0; col < cols; col++) {
                    double spawnX = baseX + colOffsets[col] * perpX;
                    double spawnZ = baseZ + colOffsets[col] * perpZ;
                    createGroundBone(attackTypeUUID, spawnX, spawnZ, minY, maxY,iDelay, 20,0f, colorAttack, false);
                }
                if(row%3 == 0) {
                    iDelay++;
                }
            }
        }
        return iDelay;
    }

    /**
     * 地面骨自身范围扩张波动攻击
     */
    private int selfSpikeAttack(@NotNull LivingEntity target,int difficulty) {
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
     * 在目标脚下直接生成地面骨刺 - 圆形生成
     */
    private int targetSpineAttack(@NotNull LivingEntity target,int difficulty) {
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
        //  return circleSpineAttack(target,3*(difficulty+1),pos.x,pos.z,13 - difficulty,10,1f - (float) difficulty / 3);
            return squareSpineAttack(target,radiusCount,pos.x,pos.z,13 - difficulty,10,offsetY);
        }
        return 0;
    }
    /**
     * 在目标周围随机生成地面骨刺
     */
    private int randomAreaSpineAttack(@NotNull LivingEntity target,int difficulty) {
        Vec3 pos = target.position();
        return circleSpineAttack(target,3 + difficulty,pos.x + this.random.nextDouble() * ATTACK_RANGE,pos.z + this.random.nextDouble() * ATTACK_RANGE
                ,13 - difficulty,10 + this.random.nextInt(15),0f);
    }
    /**
     * 圆形骨刺攻击
     * @param target 目标
     * @param layer 圆环层数
     * @param x 中心坐标x
     * @param z 中心坐标z
     * @param delay 延迟
     * @param lifetime 生命周期
     * @param offsetY 减少骨刺刺出的高度
     * @return 执行CD
     */
    private int circleSpineAttack(LivingEntity target,int layer,double x,double z,int delay,int lifetime,float offsetY){
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
     * 实心正方形骨刺攻击
     * @param target 目标
     * @param size 正方形半径（从中心到边的格数）
     * @param x 中心坐标x
     * @param z 中心坐标z
     * @param delay 延迟
     * @param lifetime 生命周期
     * @param offsetY 减少骨刺刺出的高度
     * @return 执行CD
     */
    private int squareSpineAttack(LivingEntity target, int size, double x, double z, int delay, int lifetime, float offsetY) {
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

    private void gbAttack(LivingEntity target,int count,int type){
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
