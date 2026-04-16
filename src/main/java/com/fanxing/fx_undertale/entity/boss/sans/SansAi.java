package com.fanxing.fx_undertale.entity.boss.sans;

import com.fanxing.fx_undertale.common.phys.LocalDirection;
import com.fanxing.fx_undertale.entity.ai.AttackNode;
import com.fanxing.fx_undertale.entity.ai.WeightMath;
import com.fanxing.fx_undertale.entity.ai.behavior.*;
import com.fanxing.fx_undertale.entity.ai.behavior.StartAttacking;
import com.fanxing.fx_undertale.entity.ai.sensing.SensorTargeting;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.entity.persistentData.SoulMode;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.fx_undertale.entity.summon.RotationBone;
import com.fanxing.fx_undertale.net.packet.AnimPacket;
import com.fanxing.fx_undertale.net.packet.GravityPacket;
import com.fanxing.fx_undertale.net.packet.TimeJumpTeleportPacket;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.registry.EntityTypes;
import com.fanxing.fx_undertale.registry.MemoryModuleTypes;
import com.fanxing.fx_undertale.registry.SoundEvnets;
import com.fanxing.fx_undertale.utils.EntitySelector;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class SansAi {
    private static final Logger log = LoggerFactory.getLogger(SansAi.class);

    public static final float CLOSE_RANGE_FACTOR = 0.333334f;
    public static final float MID_RANGE_FACTOR = 0.6666667f;

    public static Brain<Sans> initBrain(Sans sans, Brain<Sans> brain) {
        // 核心活动
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
                new Swim(0.8F),
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                StopBeingAngryIfTargetDead.create()
        ));

        // 空闲活动
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(
                StartAttacking.create(SansAi::findNearestValidAttackTarget, SansAi::applyTargetTag),
                new RunOne<>(ImmutableList.of(
                        Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(8.0F), 1),
                        Pair.of(new DoNothing(30, 60), 1)
                )),
                new RunOne<>(ImmutableList.of(
                        Pair.of(RandomStroll.stroll(0.6F), 2),
                        Pair.of(new DoNothing(30, 60), 1)
                ))
        ));

        // 按优先级顺序添加攻击行为（高权重在前）
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(
                StopAndSwitchTargetIfTargetInvalidAndSelfNoAttacking.create(
                        SansAi::findNearestValidAttackTarget, SansAi::applyTargetTag,
                        SansAi::clearTargetTag, false
                ),
                SetEntityLookAtAttackTarget.createIgnoringSensor(),
                createTeleportIfOutOfFollowRange(),
                createMovingBehavior(sans),

                createOpeningBehavior(),
                new MercyAttack(),
                new RecoverDownGravity(),

                new SpecialAttack(),
                createSpecialPhaseSingleSkills(),
                createAllTest()
//                new RestartableTryAllBehavior<>(GateBehavior.OrderPolicy.SHUFFLED, ImmutableList.of(
//                        Pair.of(createFirstComboSkillBehavior(), 15),
//                        Pair.of(createContinuousOffHandSkillBehavior(), 5),
//                        Pair.of(createContinuousSkillBehavior(), 1),
//                        Pair.of(createFirstPhaseSingleSkills(), 9)
//                )) {
//                    @Override
//                    protected boolean checkExtraStartConditions(ServerLevel level, Sans entity) {
//                        return entity.getPhaseID() == Sans.FIRST_PHASE;
//                    }
//                },
//                new RestartableTryAllBehavior<>(GateBehavior.OrderPolicy.SHUFFLED, ImmutableList.of(
//                        Pair.of(createSecondComboSkillBehavior(), 15),
//                        Pair.of(createContinuousOffHandSkillBehavior(), 5),
//                        Pair.of(createContinuousSkillBehavior(), 1),
//                        Pair.of(createSecondPhaseSingleSkills(), 9)
//                )) {
//                    @Override
//                    protected boolean checkExtraStartConditions(ServerLevel level, Sans entity) {
//                        return entity.getPhaseID() == Sans.SECOND_PHASE;
//                    }
//                }


        ), MemoryModuleType.ATTACK_TARGET);


        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    public static void updateActivity(Sans sans) {
        Brain<Sans> brain = sans.getBrain();
        sans.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
    }

    public static BehaviorControl<Sans> createTeleportIfOutOfFollowRange() {
        return BehaviorBuilder.create(instance -> instance.group(
                        instance.registered(MemoryModuleType.LOOK_TARGET),
                        instance.present(MemoryModuleType.ATTACK_TARGET)
                ).apply(instance, (lookTarget, attackTarget) -> (level, mob, time) -> {
                    LivingEntity t = instance.get(attackTarget);
                    double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
                    if (mob.distanceToSqr(t) > range * range && (mob.getPhaseID() != Sans.MERCY_PHASE || mob.isMercyTriggered()) && mob.getBrain().getMemory(MemoryModuleTypes.MOVE_LOCKING.get()).isEmpty() && mob.getPhaseID() != Sans.END_PHASE) {
                        mob.teleportTowards(t);
                        return true;
                    } else {
                        return false;
                    }
                })
        );
    }
    public static BehaviorControl<Sans> createMovingBehavior(Sans sans) {
        return new SpellCastingMoveInFollowRange<>(sans, CLOSE_RANGE_FACTOR, MID_RANGE_FACTOR, 1.0f) {
            @Override
            protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Sans a) {
                return super.checkExtraStartConditions(level, a) && (a.getPhaseID() != Sans.MERCY_PHASE || a.isMercyTriggered()) && a.getPhaseID() < Sans.SPECIAL_ATTACK;
            }

            @Override
            protected void handleUnableToMove(Sans mob, LivingEntity t) {
                if (unableMoveTime > 100) {
                    mob.teleportTowards(t);
                }
            }

            @Override
            protected void handleMidRange(Sans mob, LivingEntity target, double disSqr) {
                GasterBlaster gb = mob.getControllerAimGB();
                if (gb == null) super.handleMidRange(mob, target, disSqr);
                else if (gb.isRemoved()) {
                    mob.setControllerAimGB(null);
                    super.handleMidRange(mob, target, disSqr);
                } else super.markingTime(mob);
            }

            @Override
            public boolean enableFrontAndBack() {
                return false;
            }
        };
    }

    public static final String OPENING_SKILL = "opening_skill";
    public static final String PARAMETRIC_GROUND_BONE = "parametric_ground_bone_spine";
    public static final String GRAVITY_SLAM = "gravity_slam";
    public static final String TIME_JUMP = "time_jump";
    public static final String AIMED_BARRAGE_BONE = "aimed_barrage_bone";
    public static final String FORWARD_BARRAGE_BONE = "forward_barrage_bone";
    public static final String WHITE_AQUA_GROUND_BONE_WALL = "white_aqua_ground_bone_wall";
    public static final String WHITE_MULTIPLE_GROUND_BONE_WALL = "white_multiple_ground_bone_wall";

    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createOpeningBehavior() {
        int[] delay = new int[1];
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(new AttackNode<Sans>(OPENING_SKILL, 20, (a, t, tick) -> {
            if (tick == 0) {
                a.timeJumpTeleport(t, 2);
                a.teleportOrigin(a,a.getFollowRange() * 0.5F, 0, 0);
                t.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
                t.lookAt(EntityAnchorArgument.Anchor.EYES, a.getEyePosition());
                a.controlSoulMode(t, SoulMode.GRAVITY);
                a.setIsEyeBlink(true);
            } else if (tick == 2) t.teleportRelative(0, 5, 0);
            return tick >= 3;
        }).then(new AttackNode<>(OPENING_SKILL, 1, 20, (a, t, tick) -> {
                    if (tick == 3) a.gravitySlam(t, LocalDirection.DOWN, 0.5f);
                    else if (tick >= 5 && t.onGround()) {
                        a.applyGravityControlAcc(t, 0F);
                        int difficulty = a.level().getDifficulty().getId();
                        a.summonCircleGroundBoneSpine(t, 5 + 2 * difficulty, 4.5f, 10, 10, -1);
                        a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                        PacketDistributor.sendToPlayersTrackingEntity(a, new AnimPacket(a.getId(), (byte) 0));
                        return true;
                    }
                    return false;
                })).then(new AttackNode<>(OPENING_SKILL, 4, 20, (a, t, tick) -> {
                    if (tick == 10)
                        a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
                    if (tick == 13) {
                        a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_EYE_BLINK.get(), SoundSource.HOSTILE);
                        delay[0] = a.summonTunnelBoneMatrix(t, 0f);
                    }
                    if (tick >= 18 && t.onGround()) a.controlSoulMode(t, SoulMode.DEFAULT);
                    return tick >= 20 && tick >= delay[0] + 20;
                })).then(new AttackNode<>(OPENING_SKILL, 6, 4, (a, t) -> a.summonGBAroundTarget(t, 4, 0f), 20, 100))
                .then(new AttackNode<>(OPENING_SKILL, 6, 4, (a, t) -> a.summonGBAroundTarget(t, 4, 45f), 20, 100))
                .then(new AttackNode<>(OPENING_SKILL, 6, 20, (a, t, tick) -> {
                    if (tick == 4) a.summonGBAroundTarget(t, 2, 0f);
                    if (tick >= 20) {
                        a.controlSoulMode(t, SoulMode.GRAVITY);
                        return true;
                    }
                    return false;
                }))
                .root()), MemoryModuleTypes.COOLDOWN_3.get(), 0) {
            @Override
            protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Sans mob) {
                return super.checkExtraStartConditions(level, mob) && mob.getPhaseID() == Sans.OPENING_ATTACK;
            }

            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                super.stop(level, mob, gameTime);
                mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> mob.controlSoulMode(target, SoulMode.GRAVITY));
                mob.setIsEyeBlink(false);
                mob.setPhaseID(Sans.FIRST_PHASE);
            }
        };
    }

    // 单击
    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createFirstPhaseSingleSkills() {
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(
                BONE_RING_VOLLEY, ARC_SWEEP_VOLLEY, DOUBLE_SPIN_BONE, SELF_GB, CROSS_GB, RANDOM_GB, SELF_GROUND_BONE_SPINE, GROUND_BONE_SPINE_WAVE, PARAMETRIC_GROUND_BONE_SPINE
        ), (a,t) -> List.of(gravitySlam(a)), MemoryModuleTypes.COOLDOWN_1.get(), 10) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                super.stop(level, mob, gameTime);
            }
        };
    }
    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createSecondPhaseSingleSkills() {
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(
                BONE_RING_VOLLEY, ARC_SWEEP_VOLLEY, DOUBLE_SPIN_BONE, SELF_GB, CROSS_GB, RANDOM_GB, SELF_GROUND_BONE_SPINE, GROUND_BONE_SPINE_WAVE
        ), (a,t) -> List.of(gravitySlam(a,t, true)), MemoryModuleTypes.COOLDOWN_1.get(), 0) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                innerCooldown -= (int) (innerCooldown * 0.3F * mob.getPhaseFactor());
                super.stop(level, mob, gameTime);
            }
        };

    }
    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createSpecialPhaseSingleSkills() {
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(
                BONE_RING_VOLLEY.copy(), ARC_SWEEP_VOLLEY, SPECIAL_CROSS_GB
        ), (a,t) -> List.of(gravitySlam(a,t, true)), MemoryModuleTypes.COOLDOWN_1.get(),0, 10) {
            @Override
            protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Sans mob) {
                return super.checkExtraStartConditions(level, mob) && mob.getPhaseID() == Sans.SPECIAL_ATTACK;
            }
        };
    }


    // 可连续的可脱手的攻击
    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createContinuousOffHandSkillBehavior() {
        int[] delay = new int[2];
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(
                new AttackNode<Sans>(AIMED_BARRAGE_BONE, 6, 200, (a, t, tick) -> {
                    if (tick == 4) delay[0] = a.shootAimedBarrage(t);
                    return tick > 30 + delay[0];
                }).weight((a, t) -> WeightMath.linearIncrease(a.distanceTo(t), 12, a.getFollowRange()))
                        .allowConcurrent().priority(1),
                new AttackNode<Sans>(FORWARD_BARRAGE_BONE, 6, 200, (a, t, tick) -> {
                    if (tick == 4) delay[1] = a.shootForwardBarrage(t);
                    return tick > 30 + delay[1];
                }).weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), 12, a.getFollowRange()))
                        .allowConcurrent().priority(1)
        ), (a,t) -> List.of(continuousGBSkill()), MemoryModuleTypes.COOLDOWN_2.get(), 40) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                this.innerCooldown += (int) (innerCooldown * 0.3F * mob.getPhaseFactor());
                super.stop(level, mob, gameTime);
            }
        };
    }
    private static AttackNode<Sans> continuousGBSkill() {
        int[] counter = new int[]{0};
        AttackNode<Sans> root = new AttackNode<Sans>("continuous_gb", 7, 45, (a, t, tick) -> {
            int difficulty = a.level().getDifficulty().getId();
            int factor = a.getRandom().nextInt(2);
            if (tick == 5) {
                a.summonGBAroundSelf(t, 1, 0.5f + (difficulty + factor) * 0.25f + factor * 0.5f);
            }
            return tick >= 15 - 2 * difficulty - 5 * factor + factor * 10;
        }).condition((a, t) -> counter[0]++ < 8)
                .weight((a, t) -> 24.0)
                .allowConcurrent(BONE_RING_VOLLEY, ARC_SWEEP_VOLLEY, DOUBLE_SPIN_BONE, SELF_GB, CROSS_GB, RANDOM_GB, SELF_GROUND_BONE_SPINE, GROUND_BONE_SPINE_WAVE, CONTROL_GB)
                .addAllowConcurrent(WHITE_AQUA_GROUND_BONE_WALL, WHITE_MULTIPLE_GROUND_BONE_WALL)
                .priority(1);
        root.then(root);
        return root;
    }

    // 连击
    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createFirstComboSkillBehavior() {
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(whiteAquaWall(),whiteMultipleWall()), MemoryModuleTypes.COOLDOWN_4.get(), 40) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                this.innerCooldown -= (int) (innerCooldown * 0.3f * mob.getPhaseFactor());
                super.stop(level, mob, gameTime);
            }
        };
    }
    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createSecondComboSkillBehavior() {
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(), (a,t) -> List.of(timeJumpSkill(), gravitySlam(a,t, 8)), MemoryModuleTypes.COOLDOWN_3.get(), 30,10){
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                super.stop(level, mob, gameTime);
                // 对主动控制GB进行额外冷却，阻止执行连击，就立马接持续GB
                if(!mob.getBrain().hasMemoryValue(MemoryModuleTypes.COOLDOWN_4.get())){
                    mob.getBrain().setMemoryWithExpiry(MemoryModuleTypes.COOLDOWN_4.get(), Unit.INSTANCE, (long) (innerCooldown*0.2));
                }
            }
        };
    }
    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createContinuousSkillBehavior() {
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(CONTROL_GB), MemoryModuleTypes.COOLDOWN_4.get(), 10) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                if (mob.getPhaseFactor() == Sans.SECOND_PHASE) innerCooldown += (int) (innerCooldown * 0.2f);
                super.stop(level, mob, gameTime);
            }
        };
    }

    public static AttackNode<Sans> whiteAquaWall() {
        int[] delay = new int[1];
        return new AttackNode<Sans>(WHITE_AQUA_GROUND_BONE_WALL, 6, 400, (a, t, tick) -> {
            if (tick == 4) {
                int difficulty = a.level().getDifficulty().getId();
                delay[0] = 0;
                for (int i = 0; i < 6; i++) {
                    a.summonGroundBoneWall(t, ColorAttack.WHITE, 1.1F, 1f, LocalDirection.FRONT, delay[0], 5, 12.0);
                    delay[0] += 10 - difficulty - a.getStaminaFactor();
                    a.summonGroundBoneWall(t, ColorAttack.AQUA, 1.3f, 5f, LocalDirection.FRONT, delay[0], 5, 12.0);
                    delay[0] += 18 - difficulty - a.getStaminaFactor();
                }
                a.summonGroundBoneArrange(t, 1.3f, 12.0, delay[0]);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
            }
            return tick >= delay[0] + 10;
        }).mutex().condition(SansAi::isSameGravity).weight(0.4F);
    }
    public static AttackNode<Sans> whiteMultipleWall() {
        int[] delay = new int[1];
        return new AttackNode<Sans>(WHITE_MULTIPLE_GROUND_BONE_WALL, 6, 400, (a, t, tick) -> {
            if (tick == 4) {
                delay[0] = 0;
                for (int i = 0; i < 6; i++) {
                    float growScale = 1f + 2.1F * a.getRandom().nextFloat();
                    int count = 1 + Mth.ceil(4F - growScale);
                    for (int j = 0; j < count; j++) {
                        a.summonGroundBoneWall(t, ColorAttack.WHITE, 1.5F, growScale, LocalDirection.FRONT, delay[0], 7, 12.0 + j);
                    }
                    delay[0] += 15 + Mth.ceil(growScale * 5) - 2 * a.getStaminaFactor();
                }
                a.summonGroundBoneArrange(t, 1.3f, 12.0, delay[0]);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
            }
            return tick >= delay[0] + 10;
        }).mutex().condition(SansAi::isSameGravity).weight(0.4F);
    }
    public static AttackNode<Sans> gravitySlam(Sans mob,LivingEntity target, int count) {
        AttackNode<Sans> root = new AttackNode<Sans>(GRAVITY_SLAM, 0, (a, t, tick) -> tick >= 0).weight((a,t) -> (double) (3 + a.getStaminaFactor()*3+ (a.getStamina()/a.getMaxStamina() < 0.1?3:0)));
        AttackNode<Sans> curr = root;
        for (int i = 0; i < count; i++) curr = curr.then(gravitySlam(mob, target,true));
        curr.then(mob.getRandom().nextFloat() <= 0.7f ? gravitySlam(mob, target,true) : gravitySlam(mob, target,false));
        return root;
    }
    public static AttackNode<Sans> timeJumpSkill() {
        int[] counter = new int[]{0};
        int[] delay = new int[5];
        AttackNode<Sans> root = new AttackNode<Sans>(TIME_JUMP, 20, (a, t, tick) -> {
            if (tick == 0) {
                a.timeJumpTeleport(t, 5);
                t.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
            }
            return tick >= 10;
        }).condition((a, t) -> SansAi.isSameGravity(a, t) && counter[0]++ < 5).weight(6).mutex();
        List<AttackNode<Sans>> children = List.of(
                new AttackNode<Sans>(TIME_JUMP, 80, (a, t, tick) -> {
                    if (tick == 0) delay[0] = a.summonTunnelBoneMatrix(t, 0.5f);
                    return tick >= delay[0];
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 80, (a, t, tick) -> {
                    if (tick == 0) a.summonGBAroundTarget(t);
                    return tick >= 50;
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 80, (a, t, tick) -> {
                    if (tick == 0) a.summonGroundBoneWallAroundTarget(t, ColorAttack.AQUA, 5f);
                    if (tick == 8) delay[1] = tick + a.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 1f);
                    return tick >= 10 + delay[1];
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 80, (a, t, tick) -> {
                    if (tick == 0) a.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 1f);
                    if (tick == 8) delay[2] = tick + a.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 3f);
                    return tick >= 10 + delay[2];
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 80, (a, t, tick) -> {
                    if (tick == 0) delay[3] = a.summonGroundBoneMatrix(t, 1.0f);
                    return tick >= delay[3];
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 80, (a, t, tick) -> {
                    if (tick == 0) delay[4] = a.summonHugeParametricGroundBoneSpineWave(t);
                    if (tick == 16) a.level().playSound(null, t, SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE, 1, 1);
                    return tick >= 30 + delay[4];
                }).mutex().child(root)
        );
        root.children(children);
        return root;
    }
    public static AttackNode<Sans> gravitySlam(Sans mob,LivingEntity target, boolean isRandom) {
        LocalDirection[] directions = LocalDirection.values();
        float factor = 1f - mob.getStamina() * 2f / (mob.getMaxStamina() + Mth.EPSILON);
        int difficulty = mob.level().getDifficulty().getId();
        double[] h = new double[1];

        Direction[] direction = new Direction[1];
        boolean[] state = new boolean[]{true};
        int[] duration = new int[1];
        return new AttackNode<Sans>(GRAVITY_SLAM, -1, 80, (a, t, tick) -> {
            if(tick == 0){
                int count = 8;
                int index;
                do{
                    index = isRandom ? mob.getRandom().nextInt(directions.length - 1) : 1;
                    direction[0] = GravityUtils.applyRelativeGravity(mob, t, LocalDirection.values()[index]);
                    h[0] = GravityUtils.findGroundHeight(mob.level(), target.position(), direction[0]);
                }while (h[0] < 0.1F&& count -- >0);
                PacketDistributor.sendToPlayersTrackingEntity(a,new AnimPacket(a.getId(),index));
            }
            if (tick == 3) a.gravitySlamDirect(t, direction[0], (float) (h[0] *0.1f*(1f + factor*4.5F + (a.getPhaseID() == Sans.SPECIAL_ATTACK ? 1f : 0F))));
            if (tick > 3 && state[0] && t.onGround()) {
                state[0] = false;
                if (factor >= 0.3F && a.getRandom().nextFloat() < factor * 0.5f) {
                    a.summonGBAtTargetHeight(t, 4, a.getRandom().nextInt(4) * 22.5f, 5.5f);
                }
                a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f + factor * 3f, 7, 10, -1);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                duration[0] = tick;
                a.applyGravityControlAcc(t, 0f);
            }
            if (!state[0] && tick == duration[0] + 10) {
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
            }
            return tick >= duration[0] + 12 - difficulty && (!state[0] || tick > 100);
        }).allowConcurrent(SELF_GB, RANDOM_GB);
    }
    public static AttackNode<Sans> specialParametricGroundBoneSpineWaves() {
        int[] delay = new int[1];
        return new AttackNode<Sans>(
                PARAMETRIC_GROUND_BONE, 6, 40, (a, t, tick) -> {
            if (tick == 4) {
                if(t.position().subtract(a.originPos).lengthSqr() >= 256){
                    t.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
                    a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_EYE_BLINK.get(), SoundSource.HOSTILE);
                }
                delay[0] = a.summonHugeParametricGroundBoneSpineWave(t);
            }
            if (tick == 20) a.level().playSound(null, t, SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE, 1, 1);
            return tick >= 20 + delay[0];
        }).mutex().weight(2);
    }


    public static class RecoverDownGravity extends StrategyAttackBehavior<Sans> {
        protected int targetNotDownwardGravityTick;
        public RecoverDownGravity() {
            super((a,t) -> List.of(gravitySlam(a,t,false)));
        }
        @Override
        protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Sans mob) {
            Brain<Sans> brain = mob.getBrain();
            mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
                Direction gravity = target.getData(AttachmentTypes.GRAVITY);
                if (gravity == Direction.DOWN) targetNotDownwardGravityTick = 0;
                else targetNotDownwardGravityTick++;
            });
            return super.checkExtraStartConditions(level, mob) && targetNotDownwardGravityTick >= 200 + mob.getPhaseFactor()*200 && !brain.hasMemoryValue(MemoryModuleTypes.ATTACKING.get());
        }
    }
    public static class MercyAttack extends Behavior<Sans> {
        protected int tick;
        private int slamTick;
        private boolean isSlam;
        public static final int DURATION = 400;

        public MercyAttack() {
            super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), DURATION);
        }

        @Override
        protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Sans mob) {
            return mob.getPhaseID() == Sans.MERCY_PHASE && mob.isMercyTriggered();
        }

        @Override
        protected boolean canStillUse(@NotNull ServerLevel p_22545_, @NotNull Sans p_22546_, long p_22547_) {
            return true;
        }

        @Override
        protected void tick(@NotNull ServerLevel level, @NotNull Sans a, long time) {
            a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(t -> {
                if ((isSlam || tick > 10) && t.position().subtract(a.originPos).lengthSqr() > 0.25) {
                    a.setIsEyeBlink(true);
                    a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_EYE_BLINK.get(), SoundSource.HOSTILE);
                    t.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
                } else {
                    a.setIsEyeBlink(false);
                }
                if (tick == 0) {
                    PacketDistributor.sendToPlayersTrackingEntity(a, new TimeJumpTeleportPacket(t.getId(), 2));
                    t.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
                    a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_TELEPORT_TIME_JUMP.get(), SoundSource.HOSTILE);
                    a.setIsEyeBlink(true);
                    PacketDistributor.sendToPlayersTrackingEntity(a, new AnimPacket(a.getId(), 1));
                } else if (tick == 2) {
                    t.teleportTo(a.originPos.x, a.originPos.y + 5, a.originPos.z);
                } else if (tick == 3) {
                    a.gravitySlam(t, LocalDirection.DOWN, 10f);
                } else if (tick >= 5 && t.onGround() && !isSlam) {
                    a.applyGravityControlAcc(t, 0F);
                    a.summonCircleGroundBoneSpine(t, 10, 8f, DURATION, 10, 0.999999999f, 50f);
                    a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                    slamTick = tick;
                    isSlam = true;
                } else if (tick == slamTick + 10 && isSlam) {
                    a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
                    a.summonGBAroundTarget(t, 8, 16, DURATION);
                }
            });
            tick++;
        }

        @Override
        protected void stop(@NotNull ServerLevel level, @NotNull Sans a, long time) {
            PacketDistributor.sendToPlayersTrackingEntity(a,new AnimPacket(a.getId(), -1));
            a.setPhaseID(Sans.SECOND_PHASE);
        }
    }

    public static class SpecialAttack extends AttackSchedulerWithoutBuiltlnCoolingBehavior<Sans> {
        public SpecialAttack() {
            super(List.of(), (a,t) -> List.of(createSpecialAttack(a,t)), 30000);
        }

        @Override
        protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Sans mob) {
            return super.checkExtraStartConditions(level, mob) && mob.getBrain().getMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get()).isEmpty() && mob.getPhaseID() == Sans.SPECIAL_ATTACK;
        }

        @Override
        protected void start(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
            super.start(level, mob, gameTime);
        }

        @Override
        protected void tick(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
            super.tick(level, mob, gameTime);
            mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent((t->{

            }));
        }

        @Override
        protected void stop(@NotNull ServerLevel level, @NotNull Sans a, long time) {
            super.stop(level, a, time);
            a.setPhaseID(Sans.END_PHASE);
            a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(t-> {
                a.applyGravityControlAcc(t, 0F);
                a.controlSoulMode(t,SoulMode.DEFAULT);
            });
            PacketDistributor.sendToPlayersTrackingEntity(a,new AnimPacket(a.getId(), -1));
        }
    }
    public static AttackNode<Sans> createSpecialAttack(Sans mob,LivingEntity target) {
        AttackNode<Sans> root = gravitySlam(mob, target,true);
        AttackNode<Sans> curr = root;
        for (int i = 0; i < 3; i++) {
            curr = curr.then(gravitySlam(mob,target, true).mutex());
        }
        curr = curr.then(gravitySlam(mob,target, false).mutex());
        curr = curr.then(new AttackNode<Sans>(TIME_JUMP, 20, (a, t, tick) -> {
            if (tick == 0) {
                a.timeJumpTeleport(t, 5);
                t.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
                a.teleportOrigin(a,-39, 0, 0);
            }
            return tick >= 10;
        }).controlMove().mutex());
        for (int i = 0; i < 5; i++) {
            curr = curr.then(specialParametricGroundBoneSpineWaves().controlMove().mutex());
        }
        curr = curr.then(new AttackNode<Sans>("bone_ocean", 6, 10).tick((a, t, tick) -> {
            if (tick == 0) {
                a.timeJumpTeleport(t, 5);
                a.teleportOrigin(t,33, 0, 0);
                a.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
            }
            if (tick == 4) a.summonSpecialGlobalGroundBoneMatrix(t);
            if (tick > 20000 || (tick> 4 && a.position().subtract(t.position()).lengthSqr() <= 48)) {
                a.timeJumpTeleport(t, 3);
                t.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
                clearSpecialSummons(a);
                return true;
            }
            return false;
        }).controlMove().allowConcurrent(SPECIAL_CROSS_GB, ARC_SWEEP_VOLLEY, BONE_RING_VOLLEY));
        curr = curr.then(new AttackNode<>("",6,0,(a,t)->{},10,4)).mutex().controlMove();
        for (int i = 0; i < 5; i++) {
            int count = 40;
            float step = 360f/count;
            for (int j = 0; j < count; j++) {
                int finalJ = j;
                curr = curr.then(new AttackNode<Sans>("windmill_gb",0,(a, t)->{
                    if (target.position().subtract(a.originPos).lengthSqr() >= 256) {
                        target.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
                        t.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvnets.SANS_EYE_BLINK.get(), SoundSource.HOSTILE);
                    }
                    a.summonGBAimOriginPos(t,finalJ *step,1.25f);
                },i<3?2:1,10).mutex().controlMove());
            }
        }
        curr = curr.then(new AttackNode<>("",6,0,(a,t)->{},10,4)).mutex().controlMove();
        for (int i = 0; i < 30; i++) {
            curr = curr.then(specialGravitySlam(mob,target,8f,true));
        }
        for (int i = 0; i < 5; i++) {
            curr = curr.then(specialGravitySlam(mob,target,0.1f*(5-i),true));
        }
        curr.then(specialGravitySlam(mob,target,0f,false));
        return root;
    }
    public static AttackNode<Sans> specialGravitySlam(Sans mob,LivingEntity target,float acc,boolean isRandom) {
        LocalDirection[] directions = LocalDirection.values();
        double[] h = new double[1];
        Direction[] direction = new Direction[1];
        return new AttackNode<Sans>(GRAVITY_SLAM, -2, 30, (a, t, tick) -> {
            if(tick == 0){
                int count = 8;
                int index;
                do{
                    index = isRandom ? mob.getRandom().nextInt(directions.length - 1) : 1;
                    direction[0] = GravityUtils.applyRelativeGravity(mob, t, LocalDirection.values()[index]);
                    h[0] = GravityUtils.findGroundHeight(mob.level(), target.position(), direction[0]);
                }while (h[0] < 0.1F&& count -- >0);
                PacketDistributor.sendToPlayersTrackingEntity(a,new AnimPacket(a.getId(),index));
            }
            if(tick == 3) a.gravitySlamDirect(t, direction[0], (float) (acc*h[0]*0.15f));
            if(tick > 3 && t.onGround()){
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                t.hurt(a.damageSources().fall(),1f);
                return true;
            }
            return false;
        }).mutex();
    }



    //#######测试#########start

    private static AttackSchedulerWithBuiltInCoolingBehavior<Sans> createAllTest() {
        int[] delay = new int[10];
        return new AttackSchedulerWithBuiltInCoolingBehavior<>(List.of(
                BONE_RING_VOLLEY.copy().condition((a,t)->a.testAttackId == 1).cooldown(60),
                ARC_SWEEP_VOLLEY.copy().condition((a,t)->a.testAttackId == 2).cooldown(60),
                SELF_GB.copy().condition((a,t)->a.testAttackId == 3).cooldown(60),
                CROSS_GB.copy().condition((a,t)->a.testAttackId == 4).cooldown(60),
                RANDOM_GB.copy().condition((a,t)->a.testAttackId == 5).cooldown(60),
                SELF_GROUND_BONE_SPINE.copy().condition((a,t)->a.testAttackId == 6).cooldown(60),
                GROUND_BONE_SPINE_WAVE.copy().condition((a,t)->a.testAttackId == 7).cooldown(60),
                DOUBLE_SPIN_BONE.copy().condition((a,t)->a.testAttackId == 8).cooldown(60),
                PARAMETRIC_GROUND_BONE_SPINE.copy().condition((a,t)->a.testAttackId == 9).cooldown(60),
                CONTROL_GB.copy().condition((a,t)->a.testAttackId == 40).cooldown(60),
                tunnelBone().condition((a,t)->a.testAttackId == 36).cooldown(60),
                // 持续攻击
                new AttackNode<Sans>(AIMED_BARRAGE_BONE, 6, 200, (a, t, tick) -> {
                    if (tick == 4) delay[0] = a.shootAimedBarrage(t);
                    return tick > 30 + delay[0];
                }).copy().condition((a,t)->a.testAttackId == 21).cooldown(60),
                new AttackNode<Sans>(FORWARD_BARRAGE_BONE, 6, 200, (a, t, tick) -> {
                    if (tick == 4) delay[1] = a.shootForwardBarrage(t);
                    return tick > 30 + delay[1];
                }).copy().condition((a,t)->a.testAttackId == 22).cooldown(60),
                // 连击
                whiteAquaWall().copy().condition((a,t)->a.testAttackId == 31).cooldown(60),
                whiteMultipleWall().copy().condition((a,t)->a.testAttackId == 32).cooldown(60)
        ), (aa,tt) -> List.of(
                // 单击
                gravitySlam(aa).condition((a,t)->a.testAttackId == 10),
                // 持续攻击
                continuousGBSkillTest(),
                // 连击
                timeJumpSkillTest(),
                gravitySlamTest(aa,tt,8),
                gravitySlamTestShowAnim(aa,tt,16)
        ), MemoryModuleTypes.COOLDOWN_1.get(), 10) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                super.stop(level, mob, gameTime);
            }
        };
    }
    private static AttackNode<Sans> continuousGBSkillTest() {
        int[] counter = new int[]{0};
        AttackNode<Sans> root = new AttackNode<Sans>("continuous_gb", 7, 6, (a, t, tick) -> {
            int difficulty = a.level().getDifficulty().getId();
            int factor = a.getRandom().nextInt(2);
            if (tick == 5) {
                a.summonGBAroundSelf(t, 1, 0.5f + (difficulty + factor) * 0.25f + factor * 0.5f);
            }
            return tick >= 15 - 2 * difficulty - 5 * factor + factor * 10;
        }).condition((a, t) -> (counter[0]++ < 8) && a.testAttackId == 23)
                .weight((a, t) -> 24.0)
                .allowConcurrent(BONE_RING_VOLLEY, ARC_SWEEP_VOLLEY, DOUBLE_SPIN_BONE, SELF_GB, CROSS_GB, RANDOM_GB, SELF_GROUND_BONE_SPINE, GROUND_BONE_SPINE_WAVE, CONTROL_GB)
                .addAllowConcurrent(WHITE_AQUA_GROUND_BONE_WALL, WHITE_MULTIPLE_GROUND_BONE_WALL)
                .priority(1);
        root.then(root);
        return root;
    }
    public static AttackNode<Sans> gravitySlamTest(Sans mob,LivingEntity target, int count) {
        AttackNode<Sans> root = new AttackNode<Sans>(GRAVITY_SLAM, 0, (a, t, tick) -> tick >= 0).condition((a,t)->a.testAttackId == 34);
        AttackNode<Sans> curr = root;
        for (int i = 0; i < count; i++) curr = curr.then(gravitySlamTest(mob, target,true));
        curr.then(mob.getRandom().nextFloat() <= 0.7f ? gravitySlamTest(mob, target,true) : gravitySlamTest(mob, target,false));
        return root;
    }
    public static AttackNode<Sans> gravitySlamTest(Sans mob,LivingEntity target, boolean isRandom) {
        LocalDirection[] directions = LocalDirection.values();
        float factor = 1f - mob.getStamina() * 2f / (mob.getMaxStamina() + Mth.EPSILON);
        int difficulty = mob.level().getDifficulty().getId();
        double[] h = new double[1];

        Direction[] direction = new Direction[1];
        boolean[] state = new boolean[]{true};
        int[] duration = new int[1];
        return new AttackNode<Sans>(GRAVITY_SLAM, -1, 6, (a, t, tick) -> {
            if(tick == 0){
                int count = 8;
                int index;
                do{
                    index = isRandom ? mob.getRandom().nextInt(directions.length - 1) : 1;
                    direction[0] = GravityUtils.applyRelativeGravity(mob, t, LocalDirection.values()[index]);
                    h[0] = GravityUtils.findGroundHeight(mob.level(), target.position(), direction[0]);
                }while (h[0] < 0.1F&& count -- >0);
                PacketDistributor.sendToPlayersTrackingEntity(a,new AnimPacket(a.getId(),index));
            }
            if (tick == 3) a.gravitySlamDirect(t, direction[0], (float) (h[0] *0.1f*(1f + factor*4.5F + (a.getPhaseID() == Sans.SPECIAL_ATTACK ? 1f : 0F))));
            if (tick > 3 && state[0] && t.onGround()) {
                state[0] = false;
                if (factor >= 0.3F && a.getRandom().nextFloat() < factor * 0.5f) {
                    a.summonGBAtTargetHeight(t, 4, a.getRandom().nextInt(4) * 22.5f, 5.5f);
                }
                a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f + factor * 3f, 7, 10, -1);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                duration[0] = tick;
                a.applyGravityControlAcc(t, 0f);
            }
            if (!state[0] && tick == duration[0] + 10) {
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
            }
            return tick >= duration[0] + 12 - difficulty && (!state[0] || tick > 100);
        }).allowConcurrent(SELF_GB, RANDOM_GB).addAllowConcurrent(AIMED_BARRAGE_BONE, FORWARD_BARRAGE_BONE);
    }
    public static AttackNode<Sans> timeJumpSkillTest() {
        int[] counter = new int[]{0};
        int[] delay = new int[5];
        AttackNode<Sans> root = new AttackNode<Sans>(TIME_JUMP, 6, (a, t, tick) -> {
            if (tick == 0) {
                a.timeJumpTeleport(t, 5);
                t.teleportTo(a.originPos.x, a.originPos.y, a.originPos.z);
            }
            return tick >= 10;
        }).condition((a, t) -> SansAi.isSameGravity(a, t) && (counter[0]++ < 5) && a.testAttackId == 33).weight(6).mutex();
        List<AttackNode<Sans>> children = List.of(
                new AttackNode<Sans>(TIME_JUMP, 5, (a, t, tick) -> {
                    if (tick == 0) delay[0] = a.summonTunnelBoneMatrix(t, 0.5f);
                    return tick >= delay[0];
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 5, (a, t, tick) -> {
                    if (tick == 0) a.summonGBAroundTarget(t);
                    return tick >= 50;
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 5, (a, t, tick) -> {
                    if (tick == 0) a.summonGroundBoneWallAroundTarget(t, ColorAttack.AQUA, 5f);
                    if (tick == 8) delay[1] = tick + a.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 1f);
                    return tick >= 10 + delay[1];
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 5, (a, t, tick) -> {
                    if (tick == 0) a.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 1f);
                    if (tick == 8) delay[2] = tick + a.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 3f);
                    return tick >= 10 + delay[2];
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 5, (a, t, tick) -> {
                    if (tick == 0) delay[3] = a.summonGroundBoneMatrix(t, 1.0f);
                    return tick >= delay[3];
                }).mutex().child(root),
                new AttackNode<Sans>(TIME_JUMP, 5, (a, t, tick) -> {
                    if (tick == 0) delay[4] = a.summonHugeParametricGroundBoneSpineWave(t);
                    if (tick == 16) a.level().playSound(null, t, SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE, 1, 1);
                    return tick >= 30 + delay[4];
                }).mutex().child(root)
        );
        root.children(children);
        return root;
    }
    public static AttackNode<Sans> gravitySlamTestShowAnim(Sans mob,LivingEntity target, int count) {
        AttackNode<Sans> root = new AttackNode<Sans>(GRAVITY_SLAM, 0, (a, t, tick) -> tick >= 0).condition((a,t)->a.testAttackId == 35);
        AttackNode<Sans> curr = root;
        for (int i = 0; i < count; i++) curr = curr.then(gravitySlamTestShowAnim(mob, target));
        curr.then(gravitySlamTestShowAnim(mob, target));
        return root;
    }
    public static AttackNode<Sans> gravitySlamTestShowAnim(Sans mob,LivingEntity target) {
        LocalDirection[] directions = LocalDirection.values();
        float factor = 1f - mob.getStamina() * 2f / (mob.getMaxStamina() + Mth.EPSILON);
        int difficulty = mob.level().getDifficulty().getId();
        double[] h = new double[1];

        Direction[] direction = new Direction[1];
        boolean[] state = new boolean[]{true};
        int[] duration = new int[1];
        return new AttackNode<Sans>(GRAVITY_SLAM, -1, 6, (a, t, tick) -> {
            if(tick == 0){
                int count = 8;
                int index;
                do{
                    index = 1;
                    direction[0] = GravityUtils.applyRelativeGravity(mob, t, LocalDirection.values()[index]);
                    h[0] = GravityUtils.findGroundHeight(mob.level(), target.position(), direction[0]);
                }while (h[0] < 0.1F&& count -- >0);
                PacketDistributor.sendToPlayersTrackingEntity(a,new AnimPacket(a.getId(),mob.getRandom().nextInt(directions.length - 1)));
            }
            if (tick == 3) a.gravitySlamDirect(t, direction[0], (float) (h[0] *0.1f*(1f + factor*4.5F)));
            if (tick > 3 && state[0] && t.onGround()) {
                state[0] = false;
                a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f + factor * 3f, 7, 10, -1);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                duration[0] = tick;
                a.applyGravityControlAcc(t, 0f);
            }
            if (!state[0] && tick == duration[0] + 10) {
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
            }
            return tick >= duration[0] + 12 - difficulty && (!state[0] || tick > 100);
        }).allowConcurrent(SELF_GB, RANDOM_GB).addAllowConcurrent(AIMED_BARRAGE_BONE, FORWARD_BARRAGE_BONE);
    }
    public static AttackNode<Sans> tunnelBone(){
        int[] delay = new int[1];
        return new AttackNode<Sans>(TIME_JUMP, 5, (a, t, tick) -> {
            if (tick == 0) delay[0] = a.summonTunnelBoneMatrix(t, 0.5f);
            return tick >= delay[0];
        }).mutex();
    }
    //#######测试#########end



    // 飞行骨
    public static final AttackNode<Sans> BONE_RING_VOLLEY = new AttackNode<>(
            "bone_ring_volley", 8, 3, Sans::shootBoneRingVolley, 30, 30
    ).weight((a, t) -> WeightMath.linearIncrease(a.distanceTo(t), 0, 32));

    // 弧扫弹幕
    public static final AttackNode<Sans> ARC_SWEEP_VOLLEY = new AttackNode<>(
            "arc_sweep_volley", 9, 12, Sans::shootArcSweepVolley, 30, 30
    ).weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), 10, 32) * (1 + SansAi.getTargetSpeed(t) * 2));

    // 双旋转骨
    public static final AttackNode<Sans> DOUBLE_SPIN_BONE = new AttackNode<Sans>(
            "double_spin_bone", 11, 50, (a, t, tick) -> {
        if (tick == 20) a.shootRotationBone(t, 1F, 10F);
        if (tick == 26) a.shootRotationBone(t, -1F, -10F);
        return tick >= 30;
    }).condition((a, t) -> t.level().getEntitiesOfClass(RotationBone.class, t.getBoundingBox().inflate(8)).isEmpty())
            .weight((a, t) -> 20.0);

    // 自身GB
    public static final AttackNode<Sans> SELF_GB = new AttackNode<Sans>(
            "self_gb", 6, 4, (a, t) -> {
        int difficulty = a.level().getDifficulty().getId();
        int count = 1 + a.getRandom().nextInt(1 + difficulty + a.getPhaseFactor());
        a.summonGBFront(t, count, 60f / count, 17);
    }, 30, 40
    ).weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), 0, a.getFollowRange() * CLOSE_RANGE_FACTOR, 0, 16) - a.getPhaseFactor() * 5);

    // 十字 GB
    public static final AttackNode<Sans> CROSS_GB = new AttackNode<Sans>(
            "cross_gb", 6, 4, Sans::summonGBAroundTarget, 30, 40
    ).weight((a, t) -> 8 + 12 * Math.pow(getTargetSpeed(t), 0.25f));
    // 一字 GB
//    public static final AttackNode<Sans> SPECIAL_CROSS_GB = new AttackNode<Sans>(
//            "cross_gb", 6, 4, (a, t) -> {
//        boolean b = a.getRandom().nextBoolean();
//        a.summonGBAroundTarget(t, b ? 2 : 4, b ? (a.getRandom().nextInt(4) * 45f) : 0f);
//    }, 30, 40
//    ).weight((a, t) -> 8 + 12 * Math.pow(getTargetSpeed(t), 0.25f));
    // 一字 GB
    public static final AttackNode<Sans> SPECIAL_CROSS_GB = new AttackNode<Sans>(
            "cross_gb", 6, 4, (a, t) -> {
        boolean b = a.getRandom().nextBoolean();
        a.summonGBAroundTarget(t, 2, b ? 0 : 90f);
    }, 30, 40
    ).weight((a, t) -> 8 + 12 * Math.pow(getTargetSpeed(t), 0.25f));
    // 随机 GB
    public static final AttackNode<Sans> RANDOM_GB = new AttackNode<Sans>(
            "random_gb", 6, 4, (a, t) -> {
        int difficulty = a.level().getDifficulty().getId();
        a.summonGBAroundSelf(t, 1 + a.getPhaseFactor() + difficulty / 3, 1.0f + difficulty * 0.25f);
    }, 30, 40
    ).weight((a, t) -> 12.0 - a.getPhaseFactor() * 5);

    // 自身地面骨刺
    public static final AttackNode<Sans> SELF_GROUND_BONE_SPINE = new AttackNode<Sans>(
            "self_ground_bone_spine", 6, 30, (a, t, tick) -> {
        if (tick == 4) a.summonGroundBoneSpineAtSelf();
        if (tick == 13 - a.getStaminaFactor() - a.getDifficulty())
            a.level().playSound(null, t, SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE, 1, 1);
        return tick >= 20;
    }).condition((a, t) -> t.getY() - a.getY() <= 1.0f + (a.getMaxStamina() - a.getStamina()) / a.getMaxStamina() * 3.0f && a.distanceTo(t) <= (6 + 2 * (a.getPhaseFactor() + a.getDifficulty())) * 0.7f)
            .weight(50.0);
    // 地面骨刺波
    public static final AttackNode<Sans> GROUND_BONE_SPINE_WAVE = new AttackNode<Sans>(
            "ground_bone_spine_wave", 12, 30, (a, t, tick) -> {
        if (tick == 0) {
            if (a.getPhaseID() == Sans.SECOND_PHASE) a.summonHugeGroundBoneSpineWave(t);
            else a.summonGroundBoneSpineWaveAroundSelf(t);
        }
        if (tick == 14) a.level().playSound(null, t, SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE, 1, 1);
        return tick >= 22;
    }).condition((a, t) -> {
        if (a.getPhaseID() == Sans.FIRST_PHASE) return isSameGravity(a, t) && t.getY() - a.getY() <= (0.9f + 0.1f * a.getDifficulty() + 0.2f * a.getPhaseFactor()) * (1.5f + a.getStaminaFactor() * 0.5f);
        if (a.getPhaseID() == Sans.SECOND_PHASE) return isSameGravity(a, t) && t.getY() - a.getY() <= (1.7f + 0.3f * a.getStaminaFactor()) * 0.8f * (2.0f + a.getStaminaFactor() * 0.5f);
        return false;
    }).weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), 0, 32, a.getFollowRange() * CLOSE_RANGE_FACTOR, a.getFollowRange() * MID_RANGE_FACTOR)).controlMove();
    // 参数化地面骨刺
    public static final AttackNode<Sans> PARAMETRIC_GROUND_BONE_SPINE = new AttackNode<Sans>(
            PARAMETRIC_GROUND_BONE, 6, 40, (a, t, tick) -> {
        if (tick == 4) a.summonHugeParametricGroundBoneSpineWave(t);
        if (tick == 20) a.level().playSound(null, t, SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE, 1, 1);
        return tick >= 40;
    }).weight((a, t) -> WeightMath.linearIncrease(a.distanceTo(t), 0, 24, a.getFollowRange() * 0.5f, a.getFollowRange())).mutex();
    // 用于单击
    public static AttackNode<Sans> gravitySlam(Sans mob) {
        LocalDirection[] directions = LocalDirection.values();
        int difficulty = mob.level().getDifficulty().getId();
        double[] h = new double[1];
        Direction[] direction = new Direction[1];
        boolean[] state = new boolean[]{true};
        int[] duration = new int[1];
        return new AttackNode<Sans>(GRAVITY_SLAM, -1, 40, (a, t, tick) -> {
            if(tick == 0){
                int count = 8;
                int index;
                do{
                    if(a.distanceToSqr(t) <= 36){
                        int[] indexs = {0,2,3,4};
                        index = indexs[mob.getRandom().nextInt(indexs.length)];
                    }else index = mob.getRandom().nextInt(directions.length - 1);
                    direction[0] = GravityUtils.applyRelativeGravity(mob, t, LocalDirection.values()[index]);
                    h[0] = GravityUtils.findGroundHeight(mob.level(), t.position(), direction[0]);
                }while (h[0] < 0.1F&& count -- >0);
                PacketDistributor.sendToPlayersTrackingEntity(a,new AnimPacket(a.getId(),mob.getRandom().nextInt(directions.length - 1)));
            }
            if (tick == 3) a.gravitySlamDirect(t, direction[0], 1f);
            if (tick > 3 && state[0] && t.onGround()) {
                state[0] = false;
                a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f, 7, 10, -1);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                duration[0] = tick;
                a.applyGravityControlAcc(t, 0f);
            }
            if (!state[0] && tick == duration[0] + 10) a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
            return tick >= duration[0] + 12 - difficulty && (!state[0] || tick > 100);
        }).weight((a,t)->6.0 + ((a.distanceToSqr(t) <= 36)?10:0) + (EntitySelector.isFlying(t)?20:0))
                .allowConcurrent(SELF_GB, RANDOM_GB).addAllowConcurrent(AIMED_BARRAGE_BONE, FORWARD_BARRAGE_BONE);
    }

    // 持续攻击
    public static final AttackNode<Sans> CONTROL_GB = new AttackNode<Sans>("control_gb", 10, 1000, (a, t, tick) -> {
        if (tick == 0) a.setControllerAimGB(a.controlGBAim(t));
        GasterBlaster gb = a.getControllerAimGB();
        return gb == null || tick > gb.getDecayTick() || gb.isRemoved();
    }).weight(0.2F).allowConcurrent(SELF_GB, CROSS_GB, RANDOM_GB).priority(15);




    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(Sans mob) {
        Optional<LivingEntity> optional = BehaviorUtils.getLivingEntityFromUUIDMemory(mob, MemoryModuleType.ANGRY_AT);
        if (optional.isPresent() && SensorTargeting.isEntityAttackableIgnoringLineOfSightByFollowRange(mob, optional.get())) {
            return optional;
        } else {
            return Optional.empty();
        }
    }

    public static void applyTargetTag(Sans mob, LivingEntity target) {
        mob.controlSoulMode(target, SoulMode.GRAVITY);
        mob.setTargetId(target.getId());
        mob.applyKarma(target, true);
    }

    public static void clearTargetTag(Sans mob, LivingEntity target) {
        mob.controlSoulMode(target, SoulMode.DEFAULT);
        mob.setTargetId(-1);
        mob.applyKarma(target, false);
        mob.applyGravityControlAcc(target, 0F);
        GravityUtils.applyGravity(target, Direction.DOWN);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new GravityPacket(target.getId(), Direction.DOWN, 0));
        if(mob.getPhaseID() == Sans.SPECIAL_ATTACK) clearSpecialSummons(mob);

    }
    public static void clearSpecialSummons(Sans a){
        Vec3 min = a.originRela(-40, -1, -20);
        Vec3 max = a.originRela(40, 25, 20);
        AABB aabb = new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
        a.level().getEntities(a, aabb, p -> p.getType() == EntityTypes.PLATFORM_BLOCK_ENTITY.get() || p.getType() == EntityTypes.GROUND_BONE.get() || p.getType() == EntityTypes.ROTATION_BONE.get())
                .forEach(Entity::discard);
    }

    public static double getTargetSpeed(LivingEntity target) {
        if (target instanceof Player player) return player.getKnownMovement().length();
        else return target.getDeltaMovement().length();
    }

    public static boolean isSameGravity(LivingEntity a, LivingEntity t) {
        return a.getData(AttachmentTypes.GRAVITY) == t.getData(AttachmentTypes.GRAVITY);
    }
}