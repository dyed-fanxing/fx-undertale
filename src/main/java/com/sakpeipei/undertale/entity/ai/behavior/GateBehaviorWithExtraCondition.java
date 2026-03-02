package com.sakpeipei.undertale.entity.ai.behavior;

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

public class GateBehaviorWithExtraCondition<T extends LivingEntity> implements BehaviorControl<T> {
    private static final Logger log = LoggerFactory.getLogger(GateBehaviorWithExtraCondition.class);
    private final Map<MemoryModuleType<?>, MemoryStatus> entryCondition;
    private final Set<MemoryModuleType<?>> exitErasedMemories;
    private final GateBehavior.OrderPolicy orderPolicy;
    private final GateBehavior.RunningPolicy runningPolicy;
    private final ShufflingList<BehaviorControl<? super T>> behaviors = new ShufflingList<>();
    private Behavior.Status status;



    public GateBehaviorWithExtraCondition(Map<MemoryModuleType<?>, MemoryStatus> entryCondition, Set<MemoryModuleType<?>> exitErasedMemories, GateBehavior.OrderPolicy orderPolicy, GateBehavior.RunningPolicy runningPolicy, List<Pair<? extends BehaviorControl<? super T>, Integer>> behaviors) {
        this.status = Behavior.Status.STOPPED;
        this.entryCondition = entryCondition;
        this.exitErasedMemories = exitErasedMemories;
        this.orderPolicy = orderPolicy;
        this.runningPolicy = runningPolicy;
        behaviors.forEach((p_258332_) -> this.behaviors.add(p_258332_.getFirst(), p_258332_.getSecond()));
    }

    public Behavior.@NotNull Status getStatus() {
        return this.status;
    }

    protected boolean hasRequiredMemories(T p_259419_) {
        for(Map.Entry<MemoryModuleType<?>, MemoryStatus> entry : this.entryCondition.entrySet()) {
            MemoryModuleType<?> memorymoduletype = entry.getKey();
            MemoryStatus memorystatus = entry.getValue();
            if (!p_259419_.getBrain().checkMemory(memorymoduletype, memorystatus)) {
                return false;
            }
        }
        return true;
    }
    protected boolean checkExtraStartConditions(ServerLevel level, T entity) {
        return true;
    }

    public final boolean tryStart(@NotNull ServerLevel level, @NotNull T entity, long time) {
        if (this.hasRequiredMemories(entity) && checkExtraStartConditions(level, entity)) {
            this.status = Behavior.Status.RUNNING;
            this.orderPolicy.apply(this.behaviors);
            this.runningPolicy.apply(this.behaviors.stream(), level, entity, time);
            return true;
        } else {
            return false;
        }
    }

    public final void tickOrStop(@NotNull ServerLevel level, @NotNull T entity, long gameTime) {
        this.behaviors.stream().filter((behaviorControl) -> behaviorControl.getStatus() == Behavior.Status.RUNNING).forEach((behaviorControl) -> behaviorControl.tickOrStop(level, entity, gameTime));
        if (this.behaviors.stream().noneMatch((behaviorControl) -> behaviorControl.getStatus() == Behavior.Status.RUNNING)) {
            this.doStop(level, entity, gameTime);
        }

    }

    public final void doStop(@NotNull ServerLevel level, T entity, long gameTime) {
        this.status = Behavior.Status.STOPPED;
        this.behaviors.stream().filter((p_258337_) -> p_258337_.getStatus() == Behavior.Status.RUNNING).forEach((p_258341_) -> p_258341_.doStop(level, entity, gameTime));
        Brain<?> var10001 = entity.getBrain();
        Objects.requireNonNull(var10001);
        this.exitErasedMemories.forEach(var10001::eraseMemory);
    }

    public @NotNull String debugString() {
        return this.getClass().getSimpleName();
    }

    public String toString() {
        Set<BehaviorControl<? super T>> set = this.behaviors.stream().filter((p_258343_) -> p_258343_.getStatus() == Behavior.Status.RUNNING).collect(Collectors.toSet());
        return "(" + this.getClass().getSimpleName() + "): " + set;
    }
}
