package com.sakpeipei.mod.undertale.entity.boss;

import com.mojang.logging.LogUtils;
import com.sakpeipei.mod.undertale.entity.attachment.KaramAttackData;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class Sans extends PathfinderMob implements Enemy, RangedAttackMob, NeutralMob, GeoEntity,Karma {
    private static final Logger log = LoggerFactory.getLogger(Sans.class);

    private final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("move.idle");
    private final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("move.walk");
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


    private final static short ATTACK_RANGE = 16; // 攻击距离

    private short misses; // miss次数

    private int targetChangeTime;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(30, 60);;
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    public Sans(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        switch ( level.getDifficulty()){
            case PEACEFUL,EASY -> misses = 5;
            case NORMAL -> misses = 15;
            case HARD -> misses = 10;
        }
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
        this.goalSelector.addGoal(11,new RandomStrollGoal(this,0.5f));
        // 远程攻击，需要实现performRangedAttack，然后通过goal去调用
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0,  16.0F));



        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

    }

    /**
     */
    @Override
    public void performRangedAttack(@NotNull LivingEntity target, float power) {
    }


    public void flyBoneAttack(@NotNull LivingEntity target,int direction,int count,int speed,int type) {
        String attackTypeUUID = UUID.randomUUID().toString();
        switch (type) {
            case 1 -> {
                int angle = 0;
                int avg = 0;
                if (count == 1) {
                    angle = random.nextInt() * 180;
                } else {
                    avg = 180 / (count - 1);
                }
                for (int i = 0; i < count; i++) {
                    FlyingBone bone = new FlyingBone(EntityTypeRegistry.FLYING_BONE.get(), this.level(), this, 1f);
                    bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
                    // 生成扇形，不包含下方180度扇形区域， -90 对齐 MC坐标系
                    Vec3 relation = new Vec3(0, 1, 0)
                            .zRot((angle - 90) * Mth.DEG_TO_RAD)
                            .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                            .xRot(-this.getXRot() * Mth.DEG_TO_RAD);
                    bone.delayShoot(20, target, relation);
//            bone.shoot(target.getX() - bone.getX(), target.getY(0.5f) - bone.getY(), target.getZ() - bone.getZ(), 1F, 0F);
                    this.level().addFreshEntity(bone);
                    angle += avg;
                }
            }
            case 2 -> {
                int angle = 0;
                int avg = 0;
                if (count == 1) {
                    angle = random.nextInt() * 180;
                } else {
                    avg = 180 / (count - 1);
                }
                for (int i = 0; i < count; i++) {
                    FlyingBone bone = new FlyingBone(EntityTypeRegistry.FLYING_BONE.get(), this.level(), this, 1f);
                    bone.setData(AttachmentTypeRegistry.KARMA_ATTACK, new KaramAttackData(attackTypeUUID, (byte) 6));
                    // 生成扇形，不包含下方180度扇形区域， -90 对齐 MC坐标系
                    Vec3 relation = new Vec3(0, 1, 0)
                            .zRot((angle - 90) * Mth.DEG_TO_RAD)
                            .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                            .xRot(-this.getXRot() * Mth.DEG_TO_RAD);
//                    bone.delayRotateShoot(20, target, relation);
                    this.level().addFreshEntity(bone);
                    angle += avg;
                }
            }
        }
    }

    public void gbAttack(@NotNull LivingEntity target,int count,int type) {
        // 攻击物UUID -> 本次攻击类型UUID，最终存储的用于判重的是 召唤者UUID + 攻击类型UUID
        // 攻击物UUID -> 攻击类型
        // 攻击类型 -> KR值
        String attackTypeUUID = UUID.randomUUID().toString();
        switch (type){
            case 1 -> {
                GasterBlasterFixed gasterBlasterFixed = new GasterBlasterFixed(EntityTypeRegistry.GASTER_BLASTER_FIXED.get(), this.level(), this);
                gasterBlasterFixed.setData(AttachmentTypeRegistry.KARMA_ATTACK,new KaramAttackData(attackTypeUUID, (byte) 10));
                int angle = this.random.nextInt() * 180;
                gasterBlasterFixed.setPos(this.getEyePosition().add(new Vec3(0,2,0)
                        .zRot(( angle  - 90) * Mth.DEG_TO_RAD)
                        .yRot(-this.getYHeadRot() * Mth.DEG_TO_RAD)
                        .xRot(-this.getXRot() * Mth.DEG_TO_RAD)
                ));
                gasterBlasterFixed.lookAt(EntityAnchorArgument.Anchor.FEET,target.position().add(0,0.5d,0));
                this.level().addFreshEntity(gasterBlasterFixed);
            }
            default -> {

            }
        }
    }


    @Override
    public boolean hurt(@NotNull DamageSource source, float power) {
        if(isInvulnerableTo( source)){
            return false;
        }
//        if(misses > 0) {
//            if(source.getEntity() instanceof LivingEntity livingEntity){
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
        controllers.add(new AnimationController<>(this, "controller", state -> {
            AnimationController<Sans> controller = state.getController();
            if(state.isMoving()){
                controller.setAnimation(WALK_ANIM);
            }else {
                controller.setAnimation(IDLE_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }


    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if(getTarget() == null ){
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





    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return GeckoLibUtil.createInstanceCache(this);
    }



    private static class RangedAttackGoal extends Goal{
        private final Sans mob;
        private final RangedAttackMob rangedAttackMob;
        @Nullable
        private LivingEntity target;
        private int attackTime;
        private final double speedModifier;
        private int seeTime;
        private final float attackRadius;
        private final float attackRadiusSqr;
        private int attackCount; //本次招式攻击次数

        public RangedAttackGoal(Sans entity, double speedModifier, float attackRadius) {
            this.attackTime = 20;
            this.rangedAttackMob = entity;
            this.mob = entity;
            this.speedModifier = speedModifier;
            this.attackRadius = attackRadius;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null && livingentity.isAlive()) {
                this.target = livingentity;
                return true;
            } else {
                return false;
            }
        }



        @Override
        public boolean canContinueToUse() {
            return this.canUse() || this.target.isAlive() && !this.mob.getNavigation().isDone();
        }

        @Override
        public void stop() {
            this.target = null;
            this.seeTime = 0;
            this.attackTime = -1;
        }

        @Override
        public void tick() {
            if(target != null){
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


                if ( disSqr <= this.attackRadiusSqr && this.seeTime >= 20) {
                    this.mob.getNavigation().stop();
//                    ++this.strafingTime;
                } else {
                    this.mob.getNavigation().moveTo(target, this.speedModifier);
//                    this.strafingTime = -1;
                }

                int type = mob.random.nextInt(2);
                if(--attackTime == 0){
                    if(this.mob.misses >= 0 && this.mob.misses <=30){
                        switch (type) {
                            case 0 -> {
                                mob.flyBoneAttack(target,0,1,1,1);
                            }
                            case 1 -> {
                                mob.gbAttack(target,1,1);
                            }
                        }
                    }else if(this.mob.misses > 30 && this.mob.misses <= 70){

                    }else{

                    }
                    attackTime = 20;
                }
            }
        }
    }
}
