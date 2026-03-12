package com.fanxing.fx_undertale.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class StartAttacking {
    private static final Logger log = LoggerFactory.getLogger(StartAttacking.class);

    public static <E extends Mob> BehaviorControl<E> create(Function<E, Optional<? extends LivingEntity>> targetFinder) {
        return create((mob) -> true, targetFinder,(mob,target)->{});
    }
    public static <E extends Mob> BehaviorControl<E> create(Function<E, Optional<? extends LivingEntity>> targetFinder, BiConsumer<E, LivingEntity> onAttackTarget) {
        return create((mob) -> true, targetFinder,onAttackTarget);
    }
    public static <E extends Mob> BehaviorControl<E> create(Predicate<E> shouldRun, Function<E, Optional<? extends LivingEntity>> targetFinder, BiConsumer<E, LivingEntity> onAttackTarget) {
        return BehaviorBuilder.create((instance) -> instance.group(
                instance.absent(MemoryModuleType.ATTACK_TARGET),
                instance.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
        ).apply(instance, (attackTarget, cantReach) -> (level, mob, time) -> {
            if (!shouldRun.test(mob)) {
                return false;
            } else {
                Optional<? extends LivingEntity> optional = targetFinder.apply(mob);
                if (optional.isEmpty()) {
                    return false;
                } else {
                    LivingEntity livingentity = optional.get();
                    if (!mob.canAttack(livingentity)) {
                        return false;
                    } else {
                        LivingChangeTargetEvent changeTargetEvent = CommonHooks.onLivingChangeTarget(mob, livingentity, LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET);
                        LivingEntity newAboutToBeSetTarget = changeTargetEvent.getNewAboutToBeSetTarget();
                        if (!changeTargetEvent.isCanceled() && newAboutToBeSetTarget != null) {
                            onAttackTarget.accept(mob, newAboutToBeSetTarget);
                            attackTarget.set(newAboutToBeSetTarget);
                            cantReach.erase();
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
        }));
    }
}
