package com.fanxing.fx_undertale.entity.ai.behavior;

import com.fanxing.fx_undertale.entity.ai.tracker.IgnoringSensorEntityTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SetEntityLookAtAttackTarget {
    public static <E extends Mob> BehaviorControl<E> create() {
        return BehaviorBuilder.create(instance -> instance.group(
                instance.present(MemoryModuleType.ATTACK_TARGET)
        ).apply(instance, (attackTarget) -> (level, mob, gameTime) -> {
            LivingEntity target = instance.get(attackTarget);
            mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
            return true;
        }));
    }
    public static <E extends Mob> BehaviorControl<E> createIgnoringSensor() {
        return BehaviorBuilder.create(instance -> instance.group(
                instance.present(MemoryModuleType.ATTACK_TARGET)
        ).apply(instance, (attackTarget) -> (level, mob, gameTime) -> {
            LivingEntity target = instance.get(attackTarget);
            mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new IgnoringSensorEntityTracker(target, true));
            return true;
        }));
    }
}
