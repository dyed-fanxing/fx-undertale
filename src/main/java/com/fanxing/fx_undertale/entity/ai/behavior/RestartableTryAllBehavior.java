package com.fanxing.fx_undertale.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class RestartableTryAllBehavior<T extends LivingEntity> implements BehaviorControl<T> {
    private static final Logger log = LoggerFactory.getLogger(RestartableTryAllBehavior.class);
    private final Map<MemoryModuleType<?>, MemoryStatus> entryCondition;
    private final Set<MemoryModuleType<?>> exitErasedMemories;
    private final GateBehavior.OrderPolicy orderPolicy; // 只保留顺序策略，用于启动时排序
    private final ShufflingList<BehaviorControl<? super T>> behaviors = new ShufflingList<>();
    private Behavior.Status status = Behavior.Status.STOPPED;

    public static <T extends LivingEntity> RestartableTryAllBehavior<T> Order(List<Pair<? extends BehaviorControl<? super T>, Integer>> behaviors) {
        return new RestartableTryAllBehavior<>(Map.of(), Set.of(), GateBehavior.OrderPolicy.ORDERED, behaviors);
    }

    public static <T extends LivingEntity> RestartableTryAllBehavior<T> SHUFFLED(List<Pair<? extends BehaviorControl<? super T>, Integer>> behaviors) {
        return new RestartableTryAllBehavior<>(Map.of(), Set.of(), GateBehavior.OrderPolicy.SHUFFLED, behaviors);
    }

    public RestartableTryAllBehavior(GateBehavior.OrderPolicy orderPolicy, List<Pair<? extends BehaviorControl<? super T>, Integer>> behaviors) {
        this(ImmutableMap.of(), ImmutableSet.of(), orderPolicy, behaviors);
        behaviors.forEach(p -> this.behaviors.add(p.getFirst(), p.getSecond()));
    }

    public RestartableTryAllBehavior(Map<MemoryModuleType<?>, MemoryStatus> entryCondition, Set<MemoryModuleType<?>> exitErasedMemories, GateBehavior.OrderPolicy orderPolicy, List<Pair<? extends BehaviorControl<? super T>, Integer>> behaviors) {
        this.entryCondition = entryCondition;
        this.exitErasedMemories = exitErasedMemories;
        this.orderPolicy = orderPolicy;
        behaviors.forEach(p -> this.behaviors.add(p.getFirst(), p.getSecond()));
    }

    @Override
    public Behavior.@NotNull Status getStatus() {
        return status;
    }

    private boolean hasRequiredMemories(T entity) {
        for (Map.Entry<MemoryModuleType<?>, MemoryStatus> entry : entryCondition.entrySet()) {
            if (!entity.getBrain().checkMemory(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 子类可重写此方法添加额外条件（如阶段检查）。默认返回 true。
     */
    protected boolean checkExtraStartConditions(ServerLevel level, T entity) {
        return true;
    }

    @Override
    public final boolean tryStart(@NotNull ServerLevel level, @NotNull T entity, long gameTime) {
        if (this.hasRequiredMemories(entity) && checkExtraStartConditions(level, entity)) {
            this.status = Behavior.Status.RUNNING;
            this.orderPolicy.apply(this.behaviors);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final void tickOrStop(@NotNull ServerLevel level, @NotNull T entity, long gameTime) {
        this.behaviors.forEach((behaviorControl) -> {
            if (behaviorControl.getStatus() == Behavior.Status.RUNNING) {
                behaviorControl.tickOrStop(level, entity, gameTime);
            } else {
                behaviorControl.tryStart(level, entity, gameTime);
            }
        });
        if (this.behaviors.stream().noneMatch((behaviorControl) -> behaviorControl.getStatus() == Behavior.Status.RUNNING)) {
            this.doStop(level, entity, gameTime);
        }
    }

    @Override
    public final void doStop(@NotNull ServerLevel level, @NotNull T entity, long gameTime) {
        this.status = Behavior.Status.STOPPED;
        this.behaviors.stream().filter((behaviorControl) -> behaviorControl.getStatus() == Behavior.Status.RUNNING).forEach((behaviorControl) -> behaviorControl.doStop(level, entity, gameTime));
        Brain<?> var10001 = entity.getBrain();
        Objects.requireNonNull(var10001);
        this.exitErasedMemories.forEach(var10001::eraseMemory);
    }

    @Override
    public @NotNull String debugString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        Set<BehaviorControl<? super T>> running = behaviors.stream().filter(b -> b.getStatus() == Behavior.Status.RUNNING).collect(Collectors.toSet());
        return "(" + debugString() + "): " + running;
    }
}
