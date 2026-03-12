package com.fanxing.fx_undertale.entity.ai;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * 攻击组，包含一组共享同一冷却记忆的攻击节点。
 *
 * @param <T> 攻击者类型
 */
public class AttackGroup<T extends LivingEntity> {
    private final byte id;                                     // 组唯一ID
    private final BiPredicate<T, LivingEntity> condition;      // 组可用条件（例如阶段检查）
    private final int weight;                                  // 权重
    @Nullable
    private final Pair<MemoryModuleType<Unit>, Integer> cooldown;  // 组冷却记忆 + 时长（可为 null，表示无组冷却）
    private final List<AttackNode<T>> nodes;                   // 组内的攻击节点

    public AttackGroup(byte id,
                       BiPredicate<T, LivingEntity> condition,
                       List<AttackNode<T>> nodes, int weight,
                       @Nullable Pair<MemoryModuleType<Unit>, Integer> cooldown) {
        this.id = id;

        this.condition = condition;
        this.nodes = nodes;
        this.weight = weight;
        this.cooldown = cooldown;
    }

    /**
     * 判断整个组在当前状态下是否可用（组级条件，如阶段检查）。
     */
    public boolean canUse(T attacker, LivingEntity target) {
        return condition.test(attacker, target);
    }

    /**
     * 判断组的冷却是否就绪（如果组有冷却记忆，则必须不存在）。
     */
    public boolean isCooldownReady(T attacker) {
        return cooldown == null || !attacker.getBrain().hasMemoryValue(cooldown.getFirst());
    }

    /**
     * 应用组冷却（在攻击结束时调用）。
     */
    public void applyCooldown(T attacker) {
        if (cooldown != null) {
            attacker.getBrain().setMemoryWithExpiry(cooldown.getFirst(), Unit.INSTANCE, cooldown.getSecond());
        }
    }

    public List<AttackNode<T>> getNodes() {
        return nodes;
    }

    @Nullable
    public Pair<MemoryModuleType<Unit>, Integer> getCooldown() {
        return cooldown;
    }

    public int getWeight() {
        return weight;
    }

    public byte getId() {
        return id;
    }
}