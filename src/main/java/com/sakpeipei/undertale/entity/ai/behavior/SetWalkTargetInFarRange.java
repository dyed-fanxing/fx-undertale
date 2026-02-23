package com.sakpeipei.undertale.entity.ai.behavior;

import com.sakpeipei.undertale.entity.ai.tracker.IgnoringSensorEntityTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SetWalkTargetInFarRange {
    private static final Logger log = LoggerFactory.getLogger(SetWalkTargetInFarRange.class);

    /**
     * 使用 FollowRange 属性作为最大追踪范围。
     * @param speedModifier 移动速度
     */
    public static <T extends LivingEntity> BehaviorControl<T> create(float speedModifier) {
        return BehaviorBuilder.create(
                instance -> instance.group(
                        instance.registered(MemoryModuleType.WALK_TARGET),
                        instance.registered(MemoryModuleType.LOOK_TARGET),
                        instance.present(MemoryModuleType.ATTACK_TARGET),
                        instance.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                ).apply(instance, (walkTarget, lookTarget, attackTarget, visibleEntities) -> (level, mob, time) -> {
                    LivingEntity target = instance.get(attackTarget);
                    double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
                    if (mob.distanceToSqr(target) <= followRange * followRange) {
                        lookTarget.set(new IgnoringSensorEntityTracker(target, true));
                        walkTarget.set(new WalkTarget(new EntityTracker(target, false), speedModifier, 0));
                        return true;
                    }else {
                        walkTarget.erase();
                        return false;
                    }
                })
        );
    }
}
