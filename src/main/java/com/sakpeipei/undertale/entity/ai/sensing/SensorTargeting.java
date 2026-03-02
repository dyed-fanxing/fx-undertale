package com.sakpeipei.undertale.entity.ai.sensing;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public interface SensorTargeting {

    static boolean isEntityTargetableByFollowRange(LivingEntity attacker, LivingEntity target) {
        double range = attacker.getAttributeValue(Attributes.FOLLOW_RANGE);
        return attacker.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ?
                TargetingConditions.forNonCombat().range(range).ignoreInvisibilityTesting().test(attacker, target) : TargetingConditions.forNonCombat().range(range).test(attacker, target);
    }

    static boolean isEntityAttackableByFollowRange(LivingEntity attacker, LivingEntity target) {
        double range = attacker.getAttributeValue(Attributes.FOLLOW_RANGE);
        return attacker.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ?
                TargetingConditions.forCombat().range(range).ignoreInvisibilityTesting().test(attacker, target) :
                TargetingConditions.forCombat().range(range).test(attacker, target);
    }

    static boolean isEntityAttackableIgnoringLineOfSightByFollowRange(LivingEntity attacker, LivingEntity target) {
        double range = attacker.getAttributeValue(Attributes.FOLLOW_RANGE);
        return attacker.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ?
                TargetingConditions.forCombat().range(range).ignoreLineOfSight().ignoreInvisibilityTesting().test(attacker, target) :
                TargetingConditions.forCombat().range(range).ignoreLineOfSight().test(attacker, target);
    }

    static boolean isEntityAttackableIgnoringLineOfSightByFollowRangeIgnoreHeight(LivingEntity attacker, LivingEntity target) {
        double range = attacker.getAttributeValue(Attributes.FOLLOW_RANGE);
        return attacker.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ?
                TargetingConditions.forCombat().range(range).ignoreLineOfSight().ignoreInvisibilityTesting().test(attacker, target) :
                TargetingConditions.forCombat().range(range).ignoreLineOfSight().test(attacker, target);
    }
}
