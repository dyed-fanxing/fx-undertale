package com.sakpeipei.undertale.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.util.TriPredicate;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
/**
 * 攻击节点，描述一个攻击步骤的所有属性。
 * @param <T> 攻击者实体类型
 */
public class AttackNode<T extends LivingEntity> {
    private BiPredicate<T, LivingEntity> condition = (a, t) -> true;           // 可用条件
    private BiFunction<T, LivingEntity, Double> weight = (a, t) -> 1.0; // 默认权重1
    private Byte animId;                                      // 动画ID
    private TriPredicate<T,LivingEntity, Integer> tick;           // 执行动作，并返回是否完成，是节点内部的逻辑计算
    private int cooldown;                                 // 冷却时间
    private List<AttackNode<T>> children = new ArrayList<>();                 // 派生节点/子节点
    private AttackNode<T> prev; // 新增前驱节点

    public AttackNode(){
    }
    public AttackNode(int hitTick,BiConsumer<T,LivingEntity> action,int duration,int cooldown){
        this.cooldown = cooldown;
        this.tick = (attacker,target,tick) -> {
            if(tick == hitTick) action.accept(attacker, target);
            return tick >= duration;
        };
    }
    public AttackNode(byte animId,int hitTick,BiConsumer<T,LivingEntity> action,int duration,int cooldown){
        this.animId = animId;
        this.cooldown = cooldown;
        this.tick = (attacker,target,tick) -> {
            if(tick == hitTick) action.accept(attacker, target);
            return tick >= duration;
        };
    }

    public AttackNode(int cooldown,TriPredicate<T,LivingEntity,Integer> tick){
        this.cooldown = cooldown;
        this.tick = tick;
    }
    public AttackNode(byte animId,int cooldown){
        this.animId = animId;
        this.cooldown = cooldown;
    }
    public AttackNode(byte animId,int cooldown,TriPredicate<T,LivingEntity,Integer> tick){
        this.animId = animId;
        this.cooldown = cooldown;
        this.tick = tick;
    }

    public AttackNode<T> condition(BiPredicate<T, LivingEntity> condition) { this.condition = condition; return this; }
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

    public AttackNode<T> tick(TriPredicate<T,LivingEntity, Integer> tick) {
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

    public boolean tick(T attacker,LivingEntity target, int tick) {
        return this.tick.test(attacker,target, tick);
    }
    public boolean canUse(T attacker, LivingEntity target) {
        return condition.test(attacker, target);
    }
    public double getWeight(T attacker,LivingEntity target) { return this.weight.apply(attacker, target); }
    public List<AttackNode<T>> getChildren() { return children; }
    public int getCooldown() {
        return cooldown;
    }

    public Byte getAnimId() { return animId; }

    @Override
    public String toString() {
        return "AttackNode{" +
                "animId=" + animId +
                ", cooldown=" + cooldown +
                ", children.size=" + children.size() +
                ", prev=" + (prev != null ? prev.hashCode() : "null") +
                '}';
    }
}