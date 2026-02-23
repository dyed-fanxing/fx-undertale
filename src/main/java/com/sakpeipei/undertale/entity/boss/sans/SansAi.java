package com.sakpeipei.undertale.entity.boss.sans;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.sakpeipei.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.undertale.common.phys.LocalDirection;
import com.sakpeipei.undertale.entity.ai.AttackNode;
import com.sakpeipei.undertale.entity.ai.WeightMath;
import com.sakpeipei.undertale.entity.ai.behavior.*;
import com.sakpeipei.undertale.entity.ai.behavior.RunOneExtra;
import com.sakpeipei.undertale.entity.ai.sensing.SensorTargeting;
import com.sakpeipei.undertale.entity.ai.tracker.IgnoringSensorEntityTracker;
import com.sakpeipei.undertale.entity.attachment.PersistentDataDict;
import com.sakpeipei.undertale.registry.MemoryModuleTypes;
import com.sakpeipei.undertale.registry.SoundEvnets;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.sakpeipei.undertale.entity.boss.sans.Sans.FIRST_PHASE;
import static com.sakpeipei.undertale.entity.boss.sans.Sans.SECOND_PHASE;

public class SansAi {
    private static final Logger log = LoggerFactory.getLogger(SansAi.class);

    public static final float CLOSE_RANGE_FACTOR = 0.33333334f;
    public static final float MID_RANGE_FACTOR = 0.6666667f;
    public static final float FAR_RANGE_FACTOR = 1f;

    public static Brain<Sans> initBrain(Sans sans, Brain<Sans> brain) {
        // 核心活动
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
                new Swim(0.8F),
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink()
        ));

        // 空闲活动
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(
                StartAttacking.create(a -> true, SansAi::findNearestValidAttackTarget),
                new net.minecraft.world.entity.ai.behavior.RunOne<>(ImmutableList.of(
                        Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(8.0F), 1),
                        Pair.of(new DoNothing(30, 60), 1)
                )),
                new net.minecraft.world.entity.ai.behavior.RunOne<>(ImmutableList.of(
                        Pair.of(RandomStroll.stroll(0.6F), 2),
                        Pair.of(new DoNothing(30, 60), 1)
                ))
        ));
        // 按优先级顺序添加攻击行为（高权重在前）
//        brain.addActivity(Activity.FIGHT, 1, ImmutableList.of(
//                createOpeningBehavior(),
//                new RunOneExtra<>(GateBehavior.OrderPolicy.SHUFFLED, ImmutableList.of(
//                        Pair.of(createComboSkillBehavior(),9),
//                        Pair.of(createPersistentSkillBehavior(), 5),
//                        Pair.of(new AttackSchedulerBehavior<>(createSingleSkills(), MemoryModuleTypes.COOLDOWN_1.get()), 3)
//                )) {
//                    @Override
//                    protected boolean checkExtraStartConditions(ServerLevel level, Sans entity) {
//                        return entity.getPhaseID() == FIRST_PHASE;
//                    }
//                },
//                new RunOneExtra<>(GateBehavior.OrderPolicy.SHUFFLED, ImmutableList.of(
//                        Pair.of(createSecondPhaseComboBehavior(),9),
//                        Pair.of(createComboSkillBehavior(),9),
//                        Pair.of(RestartableTryAllBehavior.Order(ImmutableList.of(
//                                Pair.of(createPersistentSkillBehavior(), 5),
//                                Pair.of(new AttackSchedulerBehavior<>(createSingleSkills(), MemoryModuleTypes.COOLDOWN_1.get()), 3)
//                        )), 1)
//                )) {
//                    @Override
//                    protected boolean checkExtraStartConditions(ServerLevel level, Sans entity) {
//                        return entity.getPhaseID() == SECOND_PHASE;
//                    }
//                },
//                new RunOneExtra<>(GateBehavior.OrderPolicy.ORDERED, ImmutableList.of(
//                        Pair.of(new BackUpAndStrafeInCloseRange<Sans>(CLOSE_RANGE_FACTOR) {
//                            @Override
//                            protected void handleUnableToMove(Sans mob, LivingEntity t) {
//                                if (unableMoveTime > 100) {
//                                    mob.teleportTowards(t);
//                                }
//                            }
//                        }, 1),
//                        Pair.of(ClearWalkTargetInMidRange.create(MID_RANGE_FACTOR), 1),
//                        Pair.of(SetWalkTargetInFarRange.create(1f), 1),
//                        Pair.of(createTeleportIfOutOfFollowRange(), 1)
//                ))
//        ));
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();

        return brain;
    }

    public static void updateActivity(Sans sans) {
        sans.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
    }

    private static AttackSchedulerBehavior<Sans> createOpeningBehavior() {
        return new AttackSchedulerBehavior<>(new AttackNode<Sans>(100, (a, t, tick) -> {
            if (tick == 0) {
                a.timeJumpTeleport(t, 2);
                a.soulPattern(t, PersistentDataDict.GRAVITY);
                a.setIsEyeBlink(true);
            } else if (tick == 2) {
                Vec3 pos = RotUtils.getWorldPos(0f, 5f, 12f, 0, a.getYHeadRot()).add(a.position());
                t.teleportTo(pos.x, pos.y, pos.z);
            }
            return tick >= 5;
        }).then(new AttackNode<>((byte) 1, 100, (a, t, tick) -> {
                    if (tick == 3) {
                        a.gravitySlam(t, LocalDirection.DOWN, 0.5f);
                    } else if (tick >= 5 && t.onGround()) {
                        int difficulty = a.level().getDifficulty().getId();
                        a.summonCircleGroundBoneSpine(t, 5 + 2 * difficulty, 4.5f, 10, 10);
                        a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                        return true;
                    }
                    return false;
                })).then(new AttackNode<>((byte) 4, 100, (a, t, tick) -> {
                    if (tick == 3) {
                        a.summonLateralBoneMatrix(t, 0);
                        a.setIsEyeBlink(false);
                    }
                    if (tick >= 5 && t.onGround()) {
                        a.soulPattern(t, PersistentDataDict.DETERMINATION);
                    }
                    return tick >= 90;
                })).then(new AttackNode<>((byte) 6, 4, (a, t) -> a.summonGBAroundTarget(t, 4, 0f), 20, 100))
                .then(new AttackNode<>((byte) 6, 4, (a, t) -> a.summonGBAroundTarget(t, 4, 45f), 20, 100))
                .then(new AttackNode<>((byte) 6, 4, (a, t) -> a.summonGBAroundTarget(t, 2, 0f), 20, 100))
                .root(), MemoryModuleTypes.COOLDOWN_3.get()
        ) {
            @Override
            protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull Sans mob) {
                return super.checkExtraStartConditions(level, mob) && mob.getPhaseID() == Sans.OPENING_ATTACK;
            }

            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                super.stop(level, mob, gameTime);
                mob.setPhaseID(FIRST_PHASE);
            }
        };
    }

    private static AttackSchedulerBehavior<Sans> createComboSkillBehavior() {
        int[] delay = new int[1];
        return new AttackSchedulerBehavior<>(List.of(
                new AttackNode<Sans>((byte) 6, 400, (a, t, tick) -> {
                    if (tick == 0) {
                        a.soulPattern(t, PersistentDataDict.GRAVITY);
                    }
                    if (tick == 4) {
                        int difficulty = a.level().getDifficulty().getId();
                        delay[0] = 1;
                        for (int i = 0; i < 5; i++) {
                            a.summonGroundBoneWall(t, ColorAttack.WHITE, 1.0f, LocalDirection.FRONT, delay[0], a.getFollowRange() * 0.5f);
                            delay[0] += 10 - difficulty - a.getFactor();
                            a.summonGroundBoneWall(t, ColorAttack.AQUA, 5.0f, LocalDirection.FRONT, delay[0], a.getFollowRange() * 0.5f);
                            delay[0] += 14 - difficulty - a.getFactor();
                        }
                    }
                    if (tick >= delay[0] + 10) {
                        a.soulPattern(t, PersistentDataDict.DETERMINATION);
                        return true;
                    }
                    return false;
                }).condition((a, t) -> t.onGround() && t.closerThan(a, a.getFollowRange() * 0.7f))
        ), MemoryModuleTypes.COOLDOWN_3.get()
        ) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                this.totalCooldown -= (int) (totalCooldown * 0.3f * mob.getFactor());
                super.stop(level, mob, gameTime);
            }
        };
    }

    private static List<AttackNode<Sans>> createSingleSkills() {
        return List.of(
                // 飞行骨
                new AttackNode<>((byte) 8, 3, Sans::shootBoneRingVolley, 30, 40)
                        .weight((a,t)-> WeightMath.linearIncrease(a.distanceTo(t),0,a.getFollowRange())),
                new AttackNode<Sans>((byte) 9, 3, (a, t) -> a.shootArcSweepVolley(), 30, 40)
                        .weight((a,t)->WeightMath.linearDecrease(a.distanceTo(t),0,a.getFollowRange())*(1 + getTargetSpeed(t)*2)),

                // GB
                new AttackNode<Sans>((byte) 6, 4, (a, t) -> {
                    int difficulty = a.level().getDifficulty().getId();
                    a.summonGBAroundSelf(t, 1 + a.getFactor() + difficulty / 3, 1.0f + difficulty * 0.25f);
                }, 30, 50).weight((a, t) -> {
                    double distanceWeight = WeightMath.linearPeak(a.distanceTo(t), 0,a.getFollowRange(), a.getFollowRange() * 0.625, 2);
                    double speed = getTargetSpeed(t);
                    double speedFactor;
                    double k = 1.0, baseSpeed = 0.25;
                    if (speed <= baseSpeed) {
                        speedFactor = 1 + k * (baseSpeed - speed);
                    } else {
                        speedFactor = baseSpeed / speed;
                    }
                    return distanceWeight * speedFactor;
                }),
                new AttackNode<Sans>((byte) 6, 4, Sans::summonGBAroundTarget, 30, 50).weight((a, t) -> {
                    double distanceWeight = 16;
                    double speed = getTargetSpeed(t);
                    double speedFactor;
                    double k = 1.5, baseSpeed = 0.25;
                    if (speed <= baseSpeed) {
                        speedFactor = 1 + k * (baseSpeed - speed);
                    } else {
                        speedFactor = baseSpeed / speed;
                    }
                    return distanceWeight * speedFactor;
                }),
                new AttackNode<Sans>((byte) 6, 4, (a, t) -> {
                    int difficulty = a.level().getDifficulty().getId();
                    int count = 1 + a.getRandom().nextInt(1 + difficulty + a.getFactor());
                    a.summonGBFront(t, count, 60f / count, 17);
                }, 30, 50).weight((a, t) -> {
                    double distanceWeight = WeightMath.linearPeak(a.distanceTo(t), 0,a.getFollowRange()*0.7, a.getFollowRange() * CLOSE_RANGE_FACTOR, 2);
                    double speed = getTargetSpeed(t);
                    double speedFactor;
                    double k = 1.0, baseSpeed = 0.25;
                    if (speed <= baseSpeed) {
                        speedFactor = 1 + k * (baseSpeed - speed);
                    } else {
                        speedFactor = baseSpeed / speed;
                    }
                    return distanceWeight * speedFactor;
                }),

//                // 地面
                new AttackNode<Sans>((byte) 6,50,(a,t,tick)->{
                    if(tick == 0){
                        a.soulPattern(t, PersistentDataDict.GRAVITY);
                    }else if(tick == 4){
                        a.summonGroundBoneSpineAtSelf();
                    }
                    if(tick == 20){
                        a.soulPattern(t, PersistentDataDict.DETERMINATION);
                        return true;
                    }
                    return false;
                }).condition((a, t) -> t.getY()-a.getY() <= 1.0f+(a.getMaxStamina() - a.getStamina())/a.getMaxStamina()*3.0f && a.distanceTo(t) <= (6 + 2 * (a.getFactor() + a.getDifficulty()))*0.7f)
                        .weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), a.getFollowRange(), a.getFollowRange() * 1.5, 0.0, (6 + 2 * (a.getFactor() + a.getDifficulty()))*0.7f)),
                new AttackNode<Sans>((byte) 6, 50,(a,t,tick)->{
                    if(tick == 0){
                        a.soulPattern(t, PersistentDataDict.GRAVITY);
                    }else if(tick == 4){
                        a.summonGroundBoneSpineWaveAroundSelf(t);
                    }
                    if(tick == 20){
                        a.soulPattern(t, PersistentDataDict.DETERMINATION);
                        return true;
                    }
                    return false;
                }).condition((a, t) -> t.getY()-a.getY() <= 1.0f+(a.getMaxStamina() - a.getStamina())/a.getMaxStamina()*3.0f)
                    .weight((a,t)-> WeightMath.linearPeak(a.distanceTo(t), 0,a.getFollowRange()*0.7f, a.getFollowRange()*0.5f,1))
        );
    }

    private static AttackSchedulerBehavior<Sans> createPersistentSkillBehavior() {
        int[] delay = new int[3];
        return new AttackSchedulerBehavior<>(List.of(
                new AttackNode<Sans>((byte) 6, 200, (a, t, tick) -> {
                    if (tick == 4) {
                        delay[0] = a.shootAimedBarrage(t);
                    }
                    return tick > 30 + delay[0];
                }).weight((a, t) -> WeightMath.linearIncrease(a.distanceTo(t), 0, a.getFollowRange()) * (Math.min(1, t.getDeltaMovement().length() * 4))),
                new AttackNode<Sans>((byte) 6, 200, (a, t, tick) -> {
                    if (tick == 4) {
                        delay[1] = a.shootForwardBarrage(t);
                    }
                    return tick > 30 + delay[1];
                }).weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), 0, a.getFollowRange()) * (Math.min(1, t.getDeltaMovement().length() * 4))),
                new AttackNode<Sans>((byte) 10, 200, (a, t, tick) -> {
                    if (tick == 0) {
                        delay[2] = a.controlGBAim(t);
                    }
                    return tick > delay[2];
                }).weight((a, t) -> {
                    double distanceWeight = WeightMath.linearPeak(a.distanceTo(t), 0,a.getFollowRange()*0.7, a.getFollowRange() * 0.5f, 1);
                    double speed = getTargetSpeed(t);
                    double speedFactor;
                    double k = 1.0, baseSpeed = 0.25;
                    if (speed <= baseSpeed) {
                        speedFactor = 1 + k * (baseSpeed - speed);
                    } else {
                        speedFactor = baseSpeed / speed;
                    }
                    return distanceWeight * speedFactor;
                })
        ), (a) -> List.of(persistentGBSkill()), MemoryModuleTypes.COOLDOWN_2.get()
        ) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                this.totalCooldown += (int) (totalCooldown * 0.3f * mob.getFactor());
                super.stop(level, mob, gameTime);
            }
        };
    }

    public static AttackNode<Sans> persistentGBSkill() {
        int[] counter = new int[]{0};
        AttackNode<Sans> root = new AttackNode<Sans>((byte) 7, 45, (a, t, tick) -> {
            int difficulty = a.level().getDifficulty().getId();
            int factor = a.getRandom().nextInt(2);
            if (tick == 5) {
                a.summonGBAroundSelf(t, 1, 0.5f + (difficulty + factor) * 0.25f + factor * 0.5f);
            }
            return tick >= 15 - 2 * difficulty - 5 * factor + factor * 10;
        }).condition((a, t) -> counter[0]++ < 7)
                .weight((a, t) -> 16 * (Math.min(1, 0.5 / t.getDeltaMovement().length())));
        root.then(root);
        return root;
    }


    private static AttackSchedulerBehavior<Sans> createSecondPhaseComboBehavior() {
        return new AttackSchedulerBehavior<>(List.of(), (a) -> List.of(timeJumpSkill(), gravitySlam(a)), MemoryModuleTypes.COOLDOWN_3.get()) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                super.stop(level, mob, gameTime);
                mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(t -> t.getPersistentData().putByte(PersistentDataDict.SOUL_PATTERN, PersistentDataDict.DETERMINATION));
            }
        };
    }

    public static AttackNode<Sans> timeJumpSkill() {
        int[] counter = new int[]{0};
        AttackNode<Sans> root = new AttackNode<Sans>(20, (a, t, tick) -> {
            if (tick == 0) {
                a.timeJumpTeleport(t, 5);
            }
            return t.onGround() && tick >= 10;
        }).condition((a, t) -> counter[0]++ < 5).weight(3);
        List<AttackNode<Sans>> children = List.of(
                new AttackNode<Sans>(100, (a, t, tick) -> {
                    if (tick == 0) {
                        a.summonGBAroundTarget(t);
                    }
                    return tick >= 40;
                }).child(root),
                new AttackNode<Sans>(100, (a, t, tick) -> {
                    if (tick == 0) {
                        a.summonLateralBoneMatrix(t, -1);
                    }
                    return tick >= 80;
                }).condition((a, t) -> t.onGround()).child(root),
                new AttackNode<Sans>(100, (a, t, tick) -> {
                    if (tick == 0) {
                        a.soulPattern(t, PersistentDataDict.GRAVITY);
                        a.summonGroundBoneWallAroundTarget(t, ColorAttack.AQUA, 2f);
                    }
                    if (tick == 10) {
                        a.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 1f);
                    }
                    if (tick >= 40) {
                        a.soulPattern(t, PersistentDataDict.DETERMINATION);
                        return true;
                    }
                    return false;
                }).condition((a, t) -> t.onGround()).child(root),
                new AttackNode<Sans>(100, (a, t, tick) -> {
                    if (tick == 0) {
                        a.soulPattern(t, PersistentDataDict.GRAVITY);
                        a.summonGroundBoneMatrix(t, 1.0f);
                    }
                    if (tick >= 40) {
                        a.soulPattern(t, PersistentDataDict.DETERMINATION);
                        return true;
                    }
                    return false;
                }).condition((a, t) -> t.onGround()).child(root));
        root.children(children);
        return root;
    }

    private static AttackNode<Sans> gravitySlam(Sans mob) {
        LocalDirection[] directions = LocalDirection.values();
        float factor = 1f - mob.getStamina() * 2f / mob.getMaxStamina();
        int difficulty = mob.level().getDifficulty().getId();
        AttackNode<Sans> root = new AttackNode<Sans>(0, (a, t, tick) -> {
            if (tick == 0) {
                a.soulPattern(t, PersistentDataDict.GRAVITY);
                return true;
            }
            return false;
        })
                .weight((a, t) -> WeightMath.linearDecrease(a.getStamina(), 1, 5, 0, a.getMaxStamina() * 0.5f));
        AttackNode<Sans> curr = root;
        for (int i = 0; i < 8; i++) {
            int index = mob.getRandom().nextInt(directions.length);
            boolean[] state = new boolean[]{true};
            int[] duration = new int[1];
            curr = curr.then(new AttackNode<>((byte) index, 50, (a, t, tick) -> {
                if (tick == 3) {
                    a.gravitySlam(t, directions[index], 0.8f + factor * 0.5f);
                }
                if (tick > 3 && state[0] && t.onGround()) {
                    state[0] = false;
                    a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f + factor * 3f, 8 + (int) (factor - 1f) / 10, 10);
                    a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                    duration[0] = tick;
                }
                return tick >= duration[0] + 12 - difficulty && !state[0];
            }));
        }
        boolean[] state = new boolean[]{true};
        int[] duration = new int[1];
        curr.then(new AttackNode<>((byte) 1, (a, t, tick) -> {
            if (tick == 3) {
                a.gravitySlam(t, LocalDirection.DOWN, 1.0f + factor * 0.5f);
            }
            if (tick > 3 && state[0] && t.onGround()) {
                state[0] = false;
                a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f + factor * 3f, 8 + (int) (factor - 1f) / 10, 10);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                duration[0] = tick;
            }
            return tick >= duration[0] + 12 - difficulty && !state[0];
        })).then(new AttackNode<>(0, (a, t, tick) -> {
            if (t.onGround()) {
                a.soulPattern(t, PersistentDataDict.DETERMINATION);
                return true;
            }
            return false;
        }));
        return root;
    }


    public static BehaviorControl<Sans> createTeleportIfOutOfFollowRange() {
        return BehaviorBuilder.create(
                instance -> instance.group(
                        instance.registered(MemoryModuleType.WALK_TARGET),
                        instance.registered(MemoryModuleType.LOOK_TARGET),
                        instance.present(MemoryModuleType.ATTACK_TARGET),
                        instance.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                ).apply(instance, (walkTarget, lookTarget, attackTarget, visibleEntities) -> (level, mob, time) -> {
                    LivingEntity t = instance.get(attackTarget);
                    double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
                    if (mob.distanceToSqr(t) > range * range) {
                        lookTarget.set(new IgnoringSensorEntityTracker(t, true));
                        mob.teleportTowards(t);
                        return true;
                    } else {
                        return false;
                    }
                })
        );
    }

    public static double getTargetSpeed(LivingEntity target) {
        if (target instanceof Player player) {
            return player.getKnownMovement().length();
        } else {
            return target.getDeltaMovement().length();
        }
    }



    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(Sans mob) {
        Optional<LivingEntity> optional = BehaviorUtils.getLivingEntityFromUUIDMemory(mob, MemoryModuleType.ANGRY_AT);
        if (optional.isPresent() && SensorTargeting.isEntityAttackableIgnoringLineOfSightByFollowRange(mob, optional.get())) {
            return optional;
        }else{
            return Optional.empty();
        }
    }
}