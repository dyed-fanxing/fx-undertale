package com.sakpeipei.undertale.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NearestFollowRangeLivingEntitySensor<T extends LivingEntity> extends Sensor<T> {

    protected void doTick(ServerLevel p_26710_, T mob) {
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB aabb = mob.getBoundingBox().inflate(followRange, followRange, followRange);
        List<LivingEntity> list = p_26710_.getEntitiesOfClass(LivingEntity.class, aabb, (p_26717_) -> p_26717_ != mob && p_26717_.isAlive());
        Objects.requireNonNull(mob);
        list.sort(Comparator.comparingDouble(mob::distanceToSqr));
        Brain<?> brain = mob.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, list);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, new NearestVisibleLivingEntities(mob, list));
    }

    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }
}
