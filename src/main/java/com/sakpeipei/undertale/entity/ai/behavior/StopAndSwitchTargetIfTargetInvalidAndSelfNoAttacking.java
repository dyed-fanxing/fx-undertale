package com.sakpeipei.undertale.entity.ai.behavior;

import com.sakpeipei.undertale.registry.MemoryModuleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriPredicate;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class StopAndSwitchTargetIfTargetInvalidAndSelfNoAttacking {
    private static final int TIMEOUT = 200;
    private static final Logger log = LoggerFactory.getLogger(StopAndSwitchTargetIfTargetInvalidAndSelfNoAttacking.class);

    public static <E extends Mob> BehaviorControl<E> create(Function<E, Optional<? extends LivingEntity>> targetFinder) {
        return create((mob)->true, targetFinder,(mob,target)->{}, (mob,target,newTargetOptional)->false, (mob,target)->{}, true,(mob,target)->{});
    }
    public static <E extends Mob> BehaviorControl<E> create(
            Function<E, Optional<? extends LivingEntity>> targetFinder,
            TriPredicate<E, LivingEntity,Optional<? extends LivingEntity>> extraInvalidCondition) {
        return create((mob)->true, targetFinder,(mob,target)->{}, extraInvalidCondition, (mob,target)->{}, true,(mob,target)->{});
    }

    public static <E extends Mob> BehaviorControl<E> create(
            Function<E, Optional<? extends LivingEntity>> targetFinder,
            BiConsumer<E, LivingEntity> onAttackTarget,BiConsumer<E, LivingEntity> onInvalid) {
        return create((mob)->true, targetFinder,onAttackTarget,  (mob,target,newTargetOptional)->false,onInvalid, true,(mob,target)->{});
    }

    public static <E extends Mob> BehaviorControl<E> create(
            Function<E, Optional<? extends LivingEntity>> targetFinder,
            BiConsumer<E, LivingEntity> onAttackTarget,
            TriPredicate<E, LivingEntity,Optional<? extends LivingEntity>> extraInvalidCondition,
            BiConsumer<E, LivingEntity> onInvalid) {
        return create((mob)->true, targetFinder,onAttackTarget, extraInvalidCondition, onInvalid, true,(mob,target)->{});
    }

    public static <E extends Mob> BehaviorControl<E> create(
            Predicate<E> shouldRun,
            Function<E, Optional<? extends LivingEntity>> targetFinder,
            BiConsumer<E, LivingEntity> onAttackTarget,
            TriPredicate<E, LivingEntity,Optional<? extends LivingEntity>> extraInvalidCondition,
            BiConsumer<E, LivingEntity> onInvalid) {
        return create(shouldRun, targetFinder,onAttackTarget, extraInvalidCondition, onInvalid, true,(mob,target)->{});
    }
    /**
     * 创建一个行为：持续检查当前攻击目标的有效性，若目标无效、超时或满足额外无效条件，则尝试寻找新目标。
     * 找到新目标则立即切换，并重置疲劳计时；找不到新目标时，若技能正在进行（ATTACKING 存在）则保留当前目标，
     * 否则清除目标。
     *
     * @param shouldRun 是否运行函数
     * @param targetFinder 用于寻找新目标的函数
     * @param extraInvalidCondition <self,currAttackTarget,newAttackTargetOptional> 额外的无效条件，返回 true 表示目标视为无效（即使基础有效）
     * @param onInvalid 无效回调函数
     * @param checkTired 是否检查超时（CANT_REACH_WALK_TARGET_SINCE）
     */
    public static <E extends Mob> BehaviorControl<E> create(
            Predicate<E> shouldRun,
            Function<E, Optional<? extends LivingEntity>> targetFinder,
            BiConsumer<E, LivingEntity> onAttackTarget,
            TriPredicate<E, LivingEntity,Optional<? extends LivingEntity>> extraInvalidCondition,
            BiConsumer<E, LivingEntity> onInvalid,
            boolean checkTired,
            BiConsumer<E, LivingEntity> onTimeout) {
        return BehaviorBuilder.create(instance -> instance.group(
                instance.present(MemoryModuleType.ATTACK_TARGET),
                instance.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
                instance.registered(MemoryModuleTypes.ATTACKING.get())
        ).apply(instance, (attackTarget, cantReach, attacking) -> (level, mob, gameTime) -> {
            // 1. 检查前置条件
            if (!shouldRun.test(mob)) {
                return true; // 不满足条件，跳过本次检查，但行为继续运行
            }

            LivingEntity target = instance.get(attackTarget);
            Optional<Long> tiredSince = instance.tryGet(cantReach);
            Optional<? extends LivingEntity> newTargetOpt = targetFinder.apply(mob);
            // 有效性判断
            if(mob.canAttack(target) && target.isAlive() && target.level() == mob.level() && !extraInvalidCondition.test(mob,target,newTargetOpt)){
                if(checkTired && tiredSince.isPresent() && level.getGameTime()-tiredSince.get() > TIMEOUT){
                    onTimeout.accept(mob,target);
                }else{
                    onAttackTarget.accept(mob,target);
                    return true;
                }
            }
            // 否则需要处理：尝试寻找新目标
            if (newTargetOpt.isPresent()) {
                LivingEntity newTarget = newTargetOpt.get();
                // 触发 NeoForge 事件（与 StartAttacking 一致）
                LivingChangeTargetEvent event = CommonHooks.onLivingChangeTarget(mob, newTarget, LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET);
                LivingEntity newAboutToBeSetTarget = event.getNewAboutToBeSetTarget();
                if (!event.isCanceled() && newAboutToBeSetTarget != null) {
                    onInvalid.accept(mob, target); // 无效旧目标的回调
                    onAttackTarget.accept(mob, newAboutToBeSetTarget); //新目标的回调
                    attackTarget.set(newAboutToBeSetTarget);
                    cantReach.erase();
                    return true; // 切换成功，行为继续
                }
            }
            // 没有新目标，技能进行中，保留当前目标（即使无效或超时），让技能继续
            if (instance.tryGet(attacking).isPresent()) {
                return true;
            } else {
                onInvalid.accept(mob, target); // 无效旧目标的回调
                // 技能已结束，清除目标
                attackTarget.erase();
                cantReach.erase();
                return false;
            }
        }));
    }
}
