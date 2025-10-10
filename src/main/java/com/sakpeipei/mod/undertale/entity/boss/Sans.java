package com.sakpeipei.mod.undertale.entity.boss;

import com.mojang.logging.LogUtils;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
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

import java.util.EnumSet;
import java.util.UUID;


public class Sans extends Monster implements Enemy,NeutralMob, GeoEntity {
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
    private static final short MAX_MISSES = 50;    // 最大默认闪避次数nm

    private short misses; // miss次数

    private int targetChangeTime;

    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(30, 60);
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    public Sans(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        misses = (short) (level.getDifficulty().getId() / 2 * MAX_MISSES);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomStrollGoal(this, 0.5f));
        // 远程攻击，需要实现performRangedAttack，然后通过goal去调用
        this.goalSelector.addGoal(2, new MainAttackGoal(this, 1.0, 16.0F));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
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
        super.customServerAiStep();

    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float power) {
        if (isInvulnerableTo(source)) {
            return false;
        }
//        if (misses > 0) {
//            if (source.getEntity() instanceof LivingEntity livingEntity) {
//                this.setLastHurtByMob(livingEntity); // 核心：设置仇恨目标
//            }
//            LogUtils.getLogger().info("misses:{}", misses);
//            misses--;
//            teleport();
//            return false;
//        }
        return super.hurt(source, power);
    }

    private void teleport() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }

        RandomSource random = this.random; // 使用自身的随机源
        double radius = ATTACK_RANGE;
        int maxAttempts = 5; // 最大尝试次数

        // 优先尝试平面（XZ轴）传送
        for (int i = 0; i < maxAttempts; i++) {
            double angle = random.nextDouble() * 2 * Math.PI; // 随机角度
            double dx = Math.cos(angle) * radius * (0.75 + random.nextDouble() * 0.25); // 75%~100%半径
            double dz = Math.sin(angle) * radius * (0.75 + random.nextDouble() * 0.25);
            double dy = 0; // 平面高度不变

            if (tryTeleportTo(target.getX() + dx, target.getY() + dy, target.getZ() + dz)) {
                return; // 传送成功则退出
            }
        }

        // 平面尝试失败后，尝试球体范围内其他位置（包括Y轴）
        for (int i = 0; i < maxAttempts; i++) {
            // 球坐标系随机点（θ为极角，φ为方位角）
            double theta = random.nextDouble() * Math.PI; // [0, π]
            double phi = random.nextDouble() * 2 * Math.PI; // [0, 2π]
            double r = radius * (0.75 + random.nextDouble() * 0.25); // 75%~100%半径

            double dx = r * Math.sin(theta) * Math.cos(phi);
            double dy = r * Math.cos(theta);
            double dz = r * Math.sin(theta) * Math.sin(phi);

            if (tryTeleportTo(target.getX() + dx, target.getY() + dy, target.getZ() + dz)) {
                return; // 传送成功则退出
            }
        }

        // 所有尝试失败，默认传送到目标正上方（保底逻辑）
        this.teleportTo(target.getX(), target.getY() + 3, target.getZ());
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
            boolean success = this.randomTeleport(
                    event.getTargetX(),
                    event.getTargetY(),
                    event.getTargetZ(),
                    true // 生成粒子效果
            );

            // 4. 传送成功后的处理
            if (success) {
                // 发送游戏事件（用于红石/侦测器）
                level.gameEvent(GameEvent.TELEPORT, this.position(), GameEvent.Context.of(this));
                // 播放音效（除非Sans是静音的）
                if (!this.isSilent()) {
                    level.playSound(null, this.xo, this.yo, this.zo,
                            SoundEvents.ENDERMAN_TELEPORT, // 使用末影人音效或自定义
                            this.getSoundSource(), 1.0F, 1.0F
                    );
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


    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (getTarget() == null) {
            this.targetChangeTime = 0;
        } else {
            this.targetChangeTime = this.tickCount;
        }

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
        tag.putShort("misses",this.misses);
        this.addPersistentAngerSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag){
        super.readAdditionalSaveData(tag);
        if(tag.contains("misses")){
            this.misses = tag.getShort("misses");
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

    private int globalCD = 0;       //全局CD

    private Goal roundGoal;
    private Goal gbOffHandGoal;
    private Goal gravityControlGoal;

    private static class MainAttackGoal extends Goal {
        private final Sans mob;
        private int cd;
        private final double speedModifier;
        private int seeTime;
        private final float attackRadiusSqr;
        private final float backRadiusSqr;
        private final float pursuitRadiusSqr;
        private int lastAttackType = -1;    //上一次外部攻击类型
        private int round = 0; //轮次

        public MainAttackGoal(Sans entity, double speedModifier, float attackRadius) {
            this.cd = -1;
            this.mob = entity;
            this.speedModifier = speedModifier;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.backRadiusSqr = this.attackRadiusSqr / 4;
            this.pursuitRadiusSqr = this.attackRadiusSqr + this.backRadiusSqr;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = this.mob.getTarget();
            return livingentity != null && livingentity.isAlive();
        }


        @Override
        public boolean canContinueToUse() {
            return this.canUse() || !this.mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            mob.globalCD = 40;
            this.mob.setAggressive(true);
        }

        @Override
        public void stop() {
            this.seeTime = 0;
            this.cd = -1;
            this.mob.setAggressive(false);
        }
        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target != null) {
                double disSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
                boolean hasSeeSight = this.mob.getSensing().hasLineOfSight(target);
                boolean isContinueSee = this.seeTime > 0;
                if (hasSeeSight != isContinueSee) {
                    this.seeTime = 0;
                }
                if (hasSeeSight) {
                    ++this.seeTime;
                } else {
                    --this.seeTime;
                }
                if(this.seeTime > 0){
                    //                    ++this.strafingTime;
                    if (disSqr <= backRadiusSqr){
                        this.mob.getNavigation().stop();
                        this.mob.getMoveControl().strafe(-0.75f,0.0f);
                        this.mob.setYRot(Mth.rotateIfNecessary(this.mob.getYRot(), this.mob.yHeadRot, 0.0F));
                    }else if(disSqr > backRadiusSqr && disSqr <= this.attackRadiusSqr){
                        this.mob.getNavigation().stop();
                    }else if(disSqr <= pursuitRadiusSqr){
                        this.mob.getNavigation().moveTo(target, this.speedModifier);
                    }else{
                        this.mob.teleport();
                    }
                    if(this.seeTime >= 20){
                        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                        int difficulty = mob.level().getDifficulty().getId();
                        float diffFactor = (float) mob.level().getDifficulty().getId() / 2;
                        int count = Mth.ceil(( MAX_MISSES - this.mob.misses + 1 ) * diffFactor);
                        if(--round > 0 && --cd == 0){
                            switch (lastAttackType) {
                                case 0 -> {
                                    float speed = mob.random.nextFloat() * 0.4f + diffFactor * 0.8f;
                                    mob.flyBoneTrackAttack(target,count,speed);
                                }
                                case 1 -> {
                                    float speed = mob.random.nextFloat() * 0.4f + diffFactor * 0.8f;
                                    mob.groundBoneProjectileAttack(target,count,speed);
                                    cd = Mth.ceil(40 * (2 - diffFactor));
                                }
                                case 2 -> {

                                }
                                case 3 -> {
                                    mob.gbAttack(target, count,0);
                                    round = 3;
                                }
                            }
                        }else if(mob.globalCD == 0){
                            // 不同实体攻击类型
//                    int attackType = mob.random.nextInt(3);
                            int attackType = 1;
                            LogUtils.getLogger().info("攻击类型{}",attackType);
                            switch (attackType) {
                                case 0 -> {
                                    float speed = mob.random.nextFloat() * 0.4f + diffFactor * 0.8f;
                                    mob.flyBoneTrackAttack(target,count,speed);
                                }
                                case 1 -> {
                                    float speed = mob.random.nextFloat() * 0.4f + diffFactor * 0.8f;
                                    mob.groundBoneProjectileAttack(target,count,speed);
                                    lastAttackType = attackType;
                                    round = 3 +  2 * (difficulty - 1);
                                    cd = Mth.ceil(40 * (2 - diffFactor));
                                }
                                case 2 -> {

                                }
                                case 3 -> {
                                    mob.gbAttack(target, count,0);
                                    round = 3;
                                }
                            }
                            mob.globalCD = 120;
                        }
                    }
                }else{
                    if(disSqr <= pursuitRadiusSqr){
                        this.mob.getNavigation().moveTo(target, this.speedModifier);
                    }else{
                        this.mob.teleport();
                    }
//                    this.strafingTime = -1;
                    this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
            }
        }

        /**
         * 从自身发出地面骨刺 - 6格内圆形，6格外直线
         */
        public void groundBoneSpursAttack(@NotNull LivingEntity target, int count, int delay) {
            String attackTypeUUID = UUID.randomUUID().toString();
            Vec3 mobPos = mob.position();
            Vec3 targetPos = target.position();

            double distance = mobPos.distanceToSqr(targetPos);
            double minY = Math.min(target.getY(), mob.getY());
            double maxY = Math.max(target.getY(), mob.getY()) + 1.0;
            float baseAngle = (float)Mth.atan2(targetPos.z - mobPos.z, targetPos.x - mobPos.x) * Mth.RAD_TO_DEG;

            for (int i = 0; i < count; i++) {
                // 根据距离选择攻击模式
                double targetX = mobPos.x, targetZ = mobPos.z;
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
            double minY = Math.min(target.getY(), mob.getY());
            double maxY = Math.max(target.getY(), mob.getY()) + 1.0;

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
            Level level = mob.level();
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
                GroundBone bone = new GroundBone(level, mob, 1f, 1f, delay, targetX, spawnY, targetZ);
                bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackUUID, (byte) 6));
                // 设置旋转：骨刺指向圆心（目标位置）
                bone.setYRot(rotation);
                level.addFreshEntity(bone);
                level.gameEvent(GameEvent.ENTITY_PLACE, new Vec3(targetX, spawnY, targetZ), GameEvent.Context.of(mob));
            }
        }

    }

    /**
     * 飞行骨持续射击
     */
    private void flyBoneTrackAttack(LivingEntity target,int count,float speed) {
        String attackTypeUUID = UUID.randomUUID().toString();
        int angle = 0;
        int avg = 0;
        int delay = 10;
        int type = this.random.nextInt(2);
        for (int i = 0; i < count; i++) {
            FlyingBone bone = new FlyingBone(EntityTypeRegistry.FLYING_BONE.get(), this.level(), this, 1f, speed);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
            //位置
            Vec3 relation = new Vec3(0, 1, 0).zRot((angle - 90) * Mth.DEG_TO_RAD);
            Vec3 pos = target.position().add(relation
                    .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                    .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
            );
            bone.absMoveTo(pos.x,pos.y,pos.z);
            switch (type) {
                // 一次性射击
                case 0 -> {
                    RotUtils.lookAtByShoot(bone,target);
                    bone.delayShoot(10,relation);
                }
                // 持续射击
                case 1 -> {
                    bone.delayShoot(delay, relation);
                    delay += 5;
                }
            }
            this.level().addFreshEntity(bone);
            angle += avg;
        }
    }
    /**
     * 地面骨直线运动攻击
     */
    public void groundBoneProjectileAttack(@NotNull LivingEntity target,int count,float speed) {
        Level level = this.level();
        String attackTypeUUID = UUID.randomUUID().toString();

        count = count * 3;
        ColorAttack colorAttack;
        if(this.random.nextBoolean()){
            count *= 2;
            colorAttack = ColorAttack.AQUA;
        } else {
            colorAttack = ColorAttack.WHITE;
        }
        Vec3 position = this.position();
        double interval = 0.375;
        double xOffset = -interval * (count - 1) / 2;
        for (int i = 0; i < count; i++) {
            Vec3 pos = position.add(new Vec3(xOffset, 0, 1f)
                    .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
            );
            GroundBoneProjectile bone = new GroundBoneProjectile(level, this,2f, 1f, speed,pos.x, this.getY(), pos.z,colorAttack);
            bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
            Vec3 subtract = target.position().subtract(new Vec3(position.x, target.getY() ,position.z));
            bone.delayShoot(10, bone.position().add(subtract));
            bone.setYRot(RotUtils.shootYRot(subtract));
            level.addFreshEntity(bone);
            xOffset += interval;
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


    public static class GasterBlasterAttackRoundGoal extends AttackRoundGoal{
        private int offHandCD = 100;
        public GasterBlasterAttackRoundGoal(Sans entity, int round, int cd, int count) {
            super(entity, round, cd, count);
        }

        @Override
        public boolean canUse() {
            return offHandCD == 0 && mob.misses <= MAX_MISSES / 2;
        }


        @Override
        protected void execute(LivingEntity target) {
            mob.gbAttack(target,5,0);
        }

    }

    private static class AttackRoundGoal extends Goal {
        protected Sans mob;
        protected int round;
        protected int count;
        protected int cd;
        public AttackRoundGoal(Sans entity,int round,int cd,int count) {
            this.mob = entity;
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
                    LivingEntity target = mob.getTarget();
                    execute(target);
                }
            }
        }

        protected void execute(LivingEntity target) {
        }
    }
}
