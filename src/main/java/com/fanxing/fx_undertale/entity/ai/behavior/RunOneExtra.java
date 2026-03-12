package com.fanxing.fx_undertale.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunOneExtra<T extends LivingEntity> extends GateBehaviorWithExtraCondition<T> {
    public RunOneExtra(Map<MemoryModuleType<?>, MemoryStatus> requireMemoryMap, Set<MemoryModuleType<?>> eraseMemoryMap, GateBehavior.OrderPolicy orderPolicy, List<Pair<? extends BehaviorControl<? super T>, Integer>> children) {
        super(requireMemoryMap, eraseMemoryMap, orderPolicy, GateBehavior.RunningPolicy.RUN_ONE, children);
    }
    public RunOneExtra(Map<MemoryModuleType<?>, MemoryStatus> requireMemoryMap, GateBehavior.OrderPolicy orderPolicy, List<Pair<? extends BehaviorControl<? super T>, Integer>> children) {
        super(requireMemoryMap,Set.of(), orderPolicy, GateBehavior.RunningPolicy.RUN_ONE, children);
    }
    public RunOneExtra( GateBehavior.OrderPolicy orderPolicy, List<Pair<? extends BehaviorControl<? super T>, Integer>> children) {
        super(Map.of(),Set.of(), orderPolicy, GateBehavior.RunningPolicy.RUN_ONE, children);
    }
}
