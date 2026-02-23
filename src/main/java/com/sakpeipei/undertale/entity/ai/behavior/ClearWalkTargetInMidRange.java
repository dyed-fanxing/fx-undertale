package com.sakpeipei.undertale.entity.ai.behavior;

import com.sakpeipei.undertale.entity.ai.tracker.IgnoringSensorEntityTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearWalkTargetInMidRange{
    private static final Logger log = LoggerFactory.getLogger(ClearWalkTargetInMidRange.class);

    /**
     * @param rangeFactor 范围因子： FollowRange * 该系数
     */
    public static <T extends Mob> BehaviorControl<T> create(double rangeFactor) {
        return BehaviorBuilder.create(
                instance -> instance.group(
                        instance.registered(MemoryModuleType.WALK_TARGET),
                        instance.registered(MemoryModuleType.LOOK_TARGET),
                        instance.present(MemoryModuleType.ATTACK_TARGET),
                        instance.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                ).apply(instance, (walkTarget, lookTarget, attackTarget, visibleEntities) -> (level, mob, time) -> {
                    LivingEntity target = instance.get(attackTarget);
                    double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE) * rangeFactor;
                    if (mob.distanceToSqr(target) <= range*range) {
                        lookTarget.set(new IgnoringSensorEntityTracker(target, true));
                        walkTarget.erase();
                        return true;
                    }else{
                        return false;
                    }
                })
        );
    }
}