package com.fanxing.fx_undertale.entity.boss.sans;

import com.fanxing.fx_undertale.entity.ai.behavior.*;
import com.fanxing.fx_undertale.entity.ai.behavior.StartAttacking;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.net.packet.AnimPacket;
import com.fanxing.fx_undertale.net.packet.GravityPacket;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.fanxing.fx_undertale.common.phys.LocalDirection;
import com.fanxing.fx_undertale.entity.ai.AttackNode;
import com.fanxing.fx_undertale.entity.ai.WeightMath;
import com.fanxing.fx_undertale.entity.ai.sensing.SensorTargeting;
import com.fanxing.fx_undertale.entity.ai.tracker.IgnoringSensorEntityTracker;
import com.fanxing.fx_undertale.entity.mechanism.ColorAttack;
import com.fanxing.fx_undertale.entity.persistentData.SoulMode;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.fx_undertale.registry.MemoryModuleTypes;
import com.fanxing.fx_undertale.registry.SoundEvnets;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
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
                        SansAi::clearTargetTag
                ),
                createOpeningBehavior(),
                new RunOneExtra<>(GateBehavior.OrderPolicy.SHUFFLED, ImmutableList.of(
                        Pair.of(createComboSkillBehavior(), 9),
                        Pair.of(RestartableTryAllBehavior.Order(ImmutableList.of(
                                Pair.of(createPersistentSkillBehavior(), 5),
                                Pair.of(new AttackSchedulerBehavior<>(createSingleSkills(),(a)->List.of(gravitySlamSinge(a)), MemoryModuleTypes.COOLDOWN_1.get()), 3)
                        )), 1)
                )) {
                    @Override
                    protected boolean checkExtraStartConditions(ServerLevel level, Sans entity) {
                        return entity.getPhaseID() == Sans.FIRST_PHASE;
                    }
                },
                new RestartableTryAllBehavior<>(GateBehavior.OrderPolicy.SHUFFLED, ImmutableList.of(
                        Pair.of(new AttackSchedulerBehavior<>(createSingleSkills(), MemoryModuleTypes.COOLDOWN_1.get()), 3),
                        Pair.of(new RunOneExtra<>(GateBehavior.OrderPolicy.SHUFFLED, ImmutableList.of(
                                Pair.of(new RunOneExtra<>(GateBehavior.OrderPolicy.SHUFFLED, ImmutableList.of(
                                        Pair.of(createSecondPhaseComboBehavior(), 9)
//                                        Pair.of(createComboSkillBehavior(), 9)
                                )), 10)
//                                Pair.of(createPersistentSkillBehavior(), 5)
                        )), 10)
                )) {
                    @Override
                    protected boolean checkExtraStartConditions(ServerLevel level, Sans entity) {
                        return entity.getPhaseID() == Sans.SECOND_PHASE;
                    }
                },
                new RunOneExtra<>(GateBehavior.OrderPolicy.ORDERED, ImmutableList.of(
                        Pair.of(createTeleportIfOutOfFollowRange(), 1),
                        Pair.of(new SpellCastingMoveInFollowRange<>(sans, CLOSE_RANGE_FACTOR, MID_RANGE_FACTOR, 1.0f) {
                            @Override
                            protected void handleUnableToMove(Sans mob, LivingEntity t) {
                                if (unableMoveTime > 100) {
                                    mob.teleportTowards(t);
                                }
                            }

                            @Override
                            protected void handleMidRange(Sans mob, LivingEntity target, double disSqr) {
                                GasterBlaster gb = mob.getControllerAimGB();
                                if (gb == null) {
                                    super.handleMidRange(mob, target, disSqr);
                                } else if (gb.isRemoved()) {
                                    mob.setControllerAimGB(null);
                                    super.handleMidRange(mob, target, disSqr);
                                } else {
                                    mob.setYRot(mob.yHeadRot);
                                    mob.getMoveControl().strafe(0, 0);
                                }
                            }

                            @Override
                            public boolean enableFrontAndBack() {
                                return false;
                            }
                        }, 1)
                ))
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

    private static AttackSchedulerBehavior<Sans> createOpeningBehavior() {
        int[] delay = new int[1];
        return new AttackSchedulerBehavior<>(new AttackNode<Sans>(100, (a, t, tick) -> {
            if (tick == 0) {
                a.timeJumpTeleport(t, 2);
                a.controlSoulMode(t, SoulMode.GRAVITY);
                a.setIsEyeBlink(true);
            } else if (tick == 2) {
                Vec3 pos = RotUtils.getWorldVec3(0f, 5f, 12f, 0, a.getYHeadRot()).add(a.position());
                t.teleportTo(pos.x, pos.y, pos.z);
            }
            return tick >= 3;
        }).then(new AttackNode<>((byte) 1, 100, (a, t, tick) -> {
                    if (tick == 3) {
                        a.gravitySlam(t, LocalDirection.DOWN, 0.5f);
                    } else if (tick >= 5 && t.onGround()) {
                        a.applyGravityControlTag(t, false);
                        int difficulty = a.level().getDifficulty().getId();
                        a.summonCircleGroundBoneSpine(t, 5 + 2 * difficulty, 4.5f, 10, 10);
                        a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                        PacketDistributor.sendToPlayersTrackingEntity(a, new AnimPacket(a.getId(), (byte) 0));
                        return true;
                    }
                    return false;
                })).then(new AttackNode<>((byte) 4, 100, (a, t, tick) -> {
                    if (tick == 10) {
                        a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
                    }
                    if (tick == 13) {
                        a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_EYE_BLINK.get(), SoundSource.HOSTILE);
                        delay[0] = a.summonLateralBoneMatrix(t, 0f);
                    }
                    if (tick >= 18 && t.onGround()) {
                        a.controlSoulMode(t, SoulMode.DEFAULT);
                    }
                    return tick >= 20 && tick >= delay[0] + 20;
                })).then(new AttackNode<>((byte) 6, 4, (a, t) -> a.summonGBAroundTarget(t, 4, 0f), 20, 100))
                .then(new AttackNode<>((byte) 6, 4, (a, t) -> a.summonGBAroundTarget(t, 4, 45f), 20, 100))
                .then(new AttackNode<>((byte) 6, 100, (a, t, tick) -> {
                    if (tick == 4) {
                        a.summonGBAroundTarget(t, 2, 0f);
                    }
                    if (tick >= 20) {
                        a.controlSoulMode(t, SoulMode.GRAVITY);
                        return true;
                    }
                    return false;
                }))
                .root(), MemoryModuleTypes.COOLDOWN_3.get()
        ) {
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

    private static AttackSchedulerBehavior<Sans> createComboSkillBehavior() {
        return new AttackSchedulerBehavior<>(List.of(
//                new AttackNode<Sans>((byte) 10, 400, (a, t, tick) -> {
//                    if (tick == 0) {
//                        a.setControllerAimGB(a.controlGBAim(t));
//                    }
//                    GasterBlaster gb = a.getControllerAimGB();
//                    return gb == null || tick > gb.getDecayTick() || gb.isRemoved();
//                }).weight((a, t) -> {
//                    double distanceWeight = WeightMath.linearPeak(a.distanceTo(t), 0, a.getFollowRange() * 0.7, a.getFollowRange() * 0.5f, 1);
//                    double speed = getTargetSpeed(t);
//                    double speedFactor;
//                    double k = 1.0, baseSpeed = 0.25;
//                    if (speed <= baseSpeed) {
//                        speedFactor = 1 + k * (baseSpeed - speed);
//                    } else {
//                        speedFactor = baseSpeed / speed;
//                    }
//                    return distanceWeight * speedFactor;
//                })
        ), MemoryModuleTypes.COOLDOWN_3.get()
        ) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                this.totalCooldown -= (int) (totalCooldown * 0.3f * mob.getPhaseFactor());
                super.stop(level, mob, gameTime);
            }
        };
    }

    private static AttackSchedulerBehavior<Sans> createPersistentSkillBehavior() {
        int[] delay = new int[2];
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
                }).weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), 0, a.getFollowRange()) * (Math.min(1, t.getDeltaMovement().length() * 4)))
        ), (a) -> List.of(), MemoryModuleTypes.COOLDOWN_2.get()
//        ), (a) -> List.of(persistentGBSkill()), MemoryModuleTypes.COOLDOWN_2.get()
        ) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                this.totalCooldown += (int) (totalCooldown * 0.3f * mob.getPhaseFactor());
                super.stop(level, mob, gameTime);
            }
        };
    }

    private static AttackSchedulerBehavior<Sans> createSecondPhaseComboBehavior() {
//        return new AttackSchedulerBehavior<>(List.of(), (a) -> List.of(timeJumpSkill(), gravitySlam(a)), MemoryModuleTypes.COOLDOWN_3.get()) {
        return new AttackSchedulerBehavior<>(List.of(), (a) -> List.of(gravitySlam(a)), MemoryModuleTypes.COOLDOWN_3.get()) {
            @Override
            protected void stop(@NotNull ServerLevel level, @NotNull Sans mob, long gameTime) {
                super.stop(level, mob, gameTime);
            }
        };
    }

    public static BehaviorControl<Sans> createTeleportIfOutOfFollowRange() {
        return BehaviorBuilder.create(instance -> instance.group(
                        instance.registered(MemoryModuleType.LOOK_TARGET),
                        instance.present(MemoryModuleType.ATTACK_TARGET)
                ).apply(instance, (lookTarget, attackTarget) -> (level, mob, time) -> {
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


    private static List<AttackNode<Sans>> createSingleSkills() {
        int[] delay = new int[1];
        boolean[] state = new boolean[]{true};
        int[] duration = new int[1];
        return List.of(
                // 飞行骨
//                new AttackNode<>((byte) 8, 3, Sans::shootBoneRingVolley, 30, 40)
//                        .weight((a,t)-> WeightMath.linearIncrease(a.distanceTo(t),0,a.getFollowRange())),
//                new AttackNode<Sans>((byte) 9, 3, (a, t) -> a.shootArcSweepVolley(), 30, 40)
//                        .weight((a,t)->WeightMath.linearDecrease(a.distanceTo(t),0,a.getFollowRange())*(1 + getTargetSpeed(t)*2)),
//                new AttackNode<Sans>((byte) 11, 100, (a, t, tick) -> {
//                    if (tick == 20) {
//                        a.shootRotationBone(t, 1F, 10F);
//                    }
//                    if (tick == 26) {
////                        a.shootRotationBone(t, -1F, 10F);
//                    }
//                    return tick >= 30;
//                }).weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), 0, a.getFollowRange()) * (1 + getTargetSpeed(t) * 2)),
//                 GB
//                new AttackNode<Sans>((byte) 6, 4, (a, t) -> {
//                    int difficulty = a.level().getDifficulty().getId();
//                    a.summonGBAroundSelf(t, 1 + a.getPhaseFactor() + difficulty / 3, 1.0f + difficulty * 0.25f);
//                }, 30, 50).weight((a, t) -> {
//                    double distanceWeight = WeightMath.linearPeak(a.distanceTo(t), 0,a.getFollowRange(), a.getFollowRange() * 0.625, 2);
//                    double speed = getTargetSpeed(t);
//                    double speedFactor;
//                    double k = 1.0, baseSpeed = 0.15;
//                    if (speed <= baseSpeed) {
//                        speedFactor = 1 + k * (baseSpeed - speed);
//                    } else {
//                        speedFactor = baseSpeed / speed;
//                    }
//                    return distanceWeight * speedFactor;
//                }),
//                new AttackNode<Sans>((byte) 6, 4, Sans::summonGBAroundTarget, 30, 50).weight((a, t) -> {
//                    double distanceWeight = 16;
//                    double speed = getTargetSpeed(t);
//                    double speedFactor;
//                    double k = 1.5, baseSpeed = 0.15;
//                    if (speed <= baseSpeed) {
//                        speedFactor = 1 + k * (baseSpeed - speed);
//                    } else {
//                        speedFactor = baseSpeed / speed;
//                    }
//                    return distanceWeight * speedFactor;
//                }),
//                new AttackNode<Sans>((byte) 6, 4, (a, t) -> {
//                    int difficulty = a.level().getDifficulty().getId();
//                    int count = 1 + a.getRandom().nextInt(1 + difficulty + a.getPhaseFactor());
//                    a.summonGBFront(t, count, 60f / count, 17);
//                }, 30, 50).weight((a, t) -> {
//                    double distanceWeight = WeightMath.linearPeak(a.distanceTo(t), 0,a.getFollowRange()*0.7, a.getFollowRange() * CLOSE_RANGE_FACTOR, 2);
//                    double speed = getTargetSpeed(t);
//                    double speedFactor;
//                    double k = 1.0, baseSpeed = 0.15;
//                    if (speed <= baseSpeed) {
//                        speedFactor = 1 + k * (baseSpeed - speed);
//                    } else {
//                        speedFactor = baseSpeed / speed;
//                    }
//                    return distanceWeight * speedFactor;
//                }),
//
                // 地面
//                new AttackNode<Sans>((byte) 6, 50, (a, t, tick) -> {
//                    if (tick == 4) {
//                        a.summonGroundBoneSpineAtSelf();
//                    }
//                    if(tick == 13 - a.getStaminaFactor() - a.getDifficulty()){
//                        a.level().playSound(null,t,SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE,1,1);
//                    }
//                    return tick >= 20;
//                }).condition((a, t) -> t.getY() - a.getY() <= 1.0f + (a.getMaxStamina() - a.getStamina()) / a.getMaxStamina() * 3.0f && a.distanceTo(t) <= (6 + 2 * (a.getPhaseFactor() + a.getDifficulty())) * 0.7f)
//                        .weight((a, t) -> WeightMath.linearDecrease(a.distanceTo(t), a.getFollowRange(), a.getFollowRange() * 1.5, 0.0, (6 + 2 * (a.getPhaseFactor() + a.getDifficulty())) * 0.7f)),
//                new AttackNode<Sans>((byte) 12, 50,(a,t,tick)->{
//                    if(tick == 0){
//                        if(a.getPhaseID() == Sans.SECOND_PHASE){
//                            a.summonHugeGroundBoneSpineWave(t);
//                        }else{
//                            a.summonGroundBoneSpineWaveAroundSelf(t);
//                        }
//                    }
//                    if(tick == 14){
//                        a.level().playSound(null,t,SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE,1,1);
//                    }
//                    return tick>=22;
//                }).condition((a, t) -> t.getY()-a.getY() <= 1.0f+(a.getMaxStamina() - a.getStamina())/a.getMaxStamina()*3.0f)
//                        .weight((a,t)-> WeightMath.linearPeak(a.distanceTo(t), 0,a.getFollowRange()*0.8f, a.getFollowRange()*0.5f,1)),

//                new AttackNode<Sans>((byte) 6, 100, (a, t, tick) -> {
//                    if (tick == 4) {
//                        a.summonHugeThreadGroundBoneSpineWave(t);
//                    }
//                    if (tick == 20) {
//                        a.level().playSound(null, t, SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE, 1, 1);
//                    }
//                    return tick >= 40;
//                }).condition((a, t) -> t.getY() - a.getY() <= 1.0f + (a.getMaxStamina() - a.getStamina()) / a.getMaxStamina() * 3.0f)
//                        .weight((a,t)-> WeightMath.linearPeak(a.distanceTo(t), 0,a.getFollowRange()*0.8f, a.getFollowRange()*0.5f,1)),
//                new AttackNode<Sans>((byte) 4,40, (a, t, tick) -> {
//                    if (tick == 0) {
//                        delay[0] = a.summonLateralBoneMatrix(t,0.5f);
//                    }
//                    return tick >= delay[0];
//                }).condition((a, t) -> t.onGround())
        );
    }

    private static AttackNode<Sans> gravitySlamSinge(Sans mob) {
        LocalDirection[] directions = LocalDirection.values();
        float factor = 1f - mob.getStamina() * 2f / mob.getMaxStamina();
        int difficulty = mob.level().getDifficulty().getId();
        int index = mob.getRandom().nextInt(directions.length);
        boolean[] state = new boolean[]{true};
        int[] duration = new int[1];
        return new AttackNode<Sans>((byte) index, 50, (a, t, tick) -> {
            if (tick == 3) {
                a.gravitySlam(t, LocalDirection.values()[4], 0.8f + factor * 0.1f);
            }
            if (tick > 3 && state[0] && t.onGround()) {
                state[0] = false;
                if (factor >= 0.3F && a.getRandom().nextFloat() < factor * 0.5f) {
                    a.summonGBAtTargetHeight(t, 4, a.getRandom().nextInt(4) * 22.5f, a.getJumpBoostPower() + 5.5f);
                }
                a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f + factor * 3f, 7, 10);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                duration[0] = tick;
                a.applyGravityControlTag(t, false);
            }
            if (!state[0] && tick == duration[0] + 10) {
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
            }
            return tick >= duration[0] + 12 - difficulty && !state[0];
        }).weight(1);
    }

    private static AttackNode<Sans> persistentGBSkill() {
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


    private static AttackNode<Sans> timeJumpSkill() {
        int[] counter = new int[]{0};
        int[] delay = new int[1];
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
                        delay[0] = a.summonLateralBoneMatrix(t, 0.5f);
                    }
                    return tick >= delay[0];
                }).condition((a, t) -> t.onGround()).child(root),
                new AttackNode<Sans>(100, (a, t, tick) -> {
                    if (tick == 0) {
                        a.summonGroundBoneWallAroundTarget(t, ColorAttack.AQUA, 2f);
                    }
                    if (tick == 10) {
                        a.summonGroundBoneWallAroundTarget(t, ColorAttack.WHITE, 1f);
                    }
                    return tick >= 40;
                }).condition((a, t) -> t.onGround()).child(root),
                new AttackNode<Sans>(100, (a, t, tick) -> {
                    if (tick == 0) {
                        a.summonGroundBoneMatrix(t, 1.0f);
                    }
                    return tick >= 40;
                }).condition((a, t) -> t.onGround()).child(root));
        root.children(children);
        return root;
    }

    private static AttackNode<Sans> gravitySlam(Sans mob) {
        LocalDirection[] directions = LocalDirection.values();
        float factor = 1f - mob.getStamina() * 2f / mob.getMaxStamina();
        int difficulty = mob.level().getDifficulty().getId();
        AttackNode<Sans> root = new AttackNode<Sans>(0, (a, t, tick) -> tick >= 0)
                .weight((a, t) -> WeightMath.linearDecrease(a.getStamina(), 1, 5, 0, a.getMaxStamina() * 0.5f));
        AttackNode<Sans> curr = root;
        for (int i = 0; i < 8; i++) {
            int index = mob.getRandom().nextInt(directions.length);
            boolean[] state = new boolean[]{true};
            int[] duration = new int[1];
            curr = curr.then(new AttackNode<>((byte) index, 50, (a, t, tick) -> {
                if (tick == 3) {
                    a.gravitySlam(t, LocalDirection.values()[4], 0.8f + factor * 0.1f);
                }
                if (tick > 3 && state[0] && t.onGround()) {
                    state[0] = false;
                    if (factor >= 0.3F && a.getRandom().nextFloat() < factor * 0.5f) {
                        a.summonGBAtTargetHeight(t, 4, a.getRandom().nextInt(4) * 22.5f, a.getJumpBoostPower() + 5.5f);
                    }
                    a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f + factor * 3f, 7, 10);
                    a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                    duration[0] = tick;
                    a.applyGravityControlTag(t, false);
                }
                if (!state[0] && tick == duration[0] + 10) {
                    a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
                }
                return tick >= duration[0] + 12 - difficulty && !state[0];
            }));
        }
        boolean[] state = new boolean[]{true};
        int[] duration = new int[1];
        curr.then(new AttackNode<>((byte) 1, (a, t, tick) -> {
            if (tick == 3) {
                a.gravitySlam(t, LocalDirection.DOWN, 0.8f + factor * 0.1f);
            }
            if (tick > 3 && state[0] && t.onGround()) {
                state[0] = false;
                if (factor >= 0.3F && a.getRandom().nextFloat() < factor * 0.5f) {
                    a.summonGBAtTargetHeight(t, 4, a.getRandom().nextInt(4) * 22.5f, a.getJumpBoostPower() + 5.5f);
                }
                a.summonCircleGroundBoneSpine(t, 4 * difficulty, 1f + factor * 3f, 7, 10);
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_SLAM.get(), SoundSource.HOSTILE);
                duration[0] = tick;
                a.applyGravityControlTag(t, false);
            }
            if (!state[0] && tick == duration[0] + 10) {
                a.level().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvnets.SANS_BONE_SPINE.get(), SoundSource.HOSTILE);
            }
            return tick >= duration[0] + 12 - difficulty && !state[0];
        })).then(new AttackNode<>(0, (a, t, tick) -> t.onGround()));
        return root;
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
        Thread.dumpStack();
        mob.controlSoulMode(target, SoulMode.DEFAULT);
        mob.setTargetId(-1);
        mob.applyKarma(target, false);
        mob.applyGravityControlTag(target, false);
        Gravity.applyGravity(target, Direction.DOWN);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new GravityPacket(target.getId(), Direction.DOWN, 0));
    }
}