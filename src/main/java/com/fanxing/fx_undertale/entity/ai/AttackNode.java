package com.fanxing.fx_undertale.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.util.TriPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * 攻击节点，描述一个攻击步骤的所有属性。
 */
public class AttackNode<T extends LivingEntity> {
    protected String id; // 名称，唯一ID
    protected BiPredicate<T, LivingEntity> condition = (a, t) -> true;           // 可用条件
    protected BiFunction<T, LivingEntity, Double> weight = (a, t) -> 1.0; // 默认权重1
    protected Integer animId;                                       // 动画ID
    protected int priority = 10;                                     // 动画优先级
    protected TriPredicate<T, LivingEntity, Integer> tick;          // 执行动作，并返回是否完成，是节点内部的逻辑计算
    protected int cooldown;                                   // 冷却时间
    protected List<AttackNode<T>> children = new ArrayList<>();     // 派生节点/子节点
    protected AttackNode<T> prev;                                   // 前驱节点
    protected boolean isControlMove;                                // 是否控制移动
    protected Set<String> allowedConcurrentIds; // 允许并发的节点ID    null=允许所有并发  空集合=禁止并发（互斥） 非空集合=允许并发的节点ID集合

    public AttackNode(String id, int hitTick, BiConsumer<T, LivingEntity> action, int duration, int cooldown) {
        this.id = id;
        this.cooldown = cooldown;
        this.tick = (attacker, target, tick) -> {
            if (tick == hitTick) action.accept(attacker, target);
            return tick >= duration;
        };
    }

    public AttackNode(String id, int animId, int hitTick, BiConsumer<T, LivingEntity> action, int duration, int cooldown) {
        this.id = id;
        this.animId = animId;
        this.cooldown = cooldown;
        this.tick = (attacker, target, tick) -> {
            if (tick == hitTick) action.accept(attacker, target);
            return tick >= duration;
        };
    }

    public AttackNode(String id, int cooldown, TriPredicate<T, LivingEntity, Integer> tick) {
        this.id = id;
        this.cooldown = cooldown;
        this.tick = tick;
    }

    public AttackNode(String id, int animId, int cooldown) {
        this.id = id;
        this.animId = animId;
        this.cooldown = cooldown;
    }

    public AttackNode(String id, int animId, int cooldown, TriPredicate<T, LivingEntity, Integer> tick) {
        this.id = id;
        this.animId = animId;
        this.cooldown = cooldown;
        this.tick = tick;
    }

    public AttackNode<T> id(String id) {
        this.id = id;
        return this;
    }
    public AttackNode<T> cooldown(int cooldown){
        this.cooldown = cooldown;
        return this;
    }
    public AttackNode<T> priority(int priority){
        this.priority = priority;
        return this;
    }
    public AttackNode<T> controlMove() {
        isControlMove = true;
        return this;
    }

    public AttackNode<T> condition(BiPredicate<T, LivingEntity> condition) {
        this.condition = condition;
        return this;
    }

    // 静态权重
    public AttackNode<T> weight(double weight) {
        this.weight = (a, t) -> weight;
        return this;
    }

    // 动态权重
    public AttackNode<T> weight(BiFunction<T, LivingEntity, Double> weightFunc) {
        this.weight = weightFunc;
        return this;
    }

    public AttackNode<T> tick(TriPredicate<T, LivingEntity, Integer> tick) {
        this.tick = tick;
        return this;
    }

    /**
     * 一次性设置多个子节点（分支）。
     */
    public AttackNode<T> children(List<AttackNode<T>> children) {
        this.children = children;
        return this;
    }

    public AttackNode<T> child(AttackNode<T> child) {
        this.children.add(child);
        return this;
    }

    // 修改 then 方法，建立双向关系
    public AttackNode<T> then(AttackNode<T> next) {
        this.children.add(next);
        next.prev = this; // 设置前驱
        return next;
    }


    // 新增方法，从当前节点向前追溯到根
    public AttackNode<T> root() {
        AttackNode<T> node = this;
        while (node.prev != null) {
            node = node.prev;
        }
        return node;
    }

    public boolean tick(T attacker, LivingEntity target, int tick) {
        return this.tick.test(attacker, target, tick);
    }

    public boolean canUse(T attacker, LivingEntity target) {
        return condition.test(attacker, target);
    }

    public double getWeight(T attacker, LivingEntity target) {
        return this.weight.apply(attacker, target);
    }


    // 互斥（禁止任何并发）
    public AttackNode<T> mutex() {
        this.allowedConcurrentIds = Collections.emptySet();
        return this;
    }
    // 允许所有并发
    public AttackNode<T> allowConcurrent() {
        this.allowedConcurrentIds = null;
        return this;
    }
    @SafeVarargs
    public final AttackNode<T> allowConcurrent(AttackNode<T>... nodes) {
        this.allowedConcurrentIds = Arrays.stream(nodes)
                .map(AttackNode::getId)
                .collect(Collectors.toSet());
        return this;
    }
    // 传入字符串 ID：若数组为空则允许所有
    public AttackNode<T> allowConcurrent(String... ids) {
        this.allowedConcurrentIds = Set.of(ids);
        return this;
    }
    // 传入字符串 ID：若数组为空则允许所有
    public AttackNode<T> addAllowConcurrent(String... ids) {
        if(allowedConcurrentIds == null) this.allowedConcurrentIds = Set.of(ids);
        else this.allowedConcurrentIds.add(Arrays.toString(ids));
        return this;
    }
    public boolean isConcurrentAllowed(String targetId) {
        if (allowedConcurrentIds == null) return true;
        return allowedConcurrentIds.contains(targetId);
    }


    // getter
    public List<AttackNode<T>> getChildren() {
        return children;
    }

    public int getCooldown() {
        return cooldown;
    }

    public Integer getAnimId() {
        return animId;
    }

    public String getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isControlMove() {
        return isControlMove;
    }

    public Set<String> getAllowedConcurrentIds() {
        return allowedConcurrentIds;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AttackNode<?> that = (AttackNode<?>) obj;
        return Objects.equals(id, that.id);  // 只比较 id
    }

    @Override
    public String toString() {
        return "AttackNode{" +
                "id=" + id +
                ", cooldown=" + cooldown +
                ", isControlMove=" + isControlMove +
                ", priority=" + priority +
                ", prev=" + prev +
                '}';
    }

    public AttackNode<T> copy() {
        // 1. 根据字段选择合适的构造器
        AttackNode<T> cloned;
        if (this.tick != null) {
            cloned = new AttackNode<>(this.id, this.cooldown, this.tick);
        } else if (this.animId != null) {
            cloned = new AttackNode<>(this.id, this.animId, this.cooldown);
        } else {
            // fallback：无 tick 无动画，使用空 tick
            cloned = new AttackNode<>(this.id, this.cooldown, (a, t, tick) -> true);
        }
        // 2. 复制普通字段
        cloned.priority = this.priority;
        cloned.isControlMove = this.isControlMove;
        cloned.condition = this.condition;
        cloned.weight = this.weight;

        // 3. 复制并发许可集合
        if (this.allowedConcurrentIds != null) {
            cloned.allowedConcurrentIds = new HashSet<>(this.allowedConcurrentIds);
        }

        // 4. 深拷贝子节点
        cloned.children = new ArrayList<>(this.children.size());
        for (AttackNode<T> child : this.children) {
            AttackNode<T> clonedChild = child.copy();   // 递归调用 copy()
            clonedChild.prev = cloned;
            cloned.children.add(clonedChild);
        }

        cloned.prev = null;   // 新克隆的节点是独立树根
        return cloned;
    }
}