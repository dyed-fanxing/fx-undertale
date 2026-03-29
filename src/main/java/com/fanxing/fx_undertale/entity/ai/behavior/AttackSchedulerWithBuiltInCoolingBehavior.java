package com.fanxing.fx_undertale.entity.ai.behavior;


import com.google.common.collect.ImmutableMap;
import com.fanxing.fx_undertale.entity.ai.AttackNode;
import com.fanxing.fx_undertale.net.packet.AnimPacket;
import com.fanxing.fx_undertale.registry.MemoryModuleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


/**
 * 单个攻击组的调度行为。负责从组内选择节点并执行，管理派生，含有内置CD
 */
public class AttackSchedulerWithBuiltInCoolingBehavior<T extends LivingEntity> extends Behavior<T> {
    private static final Logger log = LoggerFactory.getLogger(AttackSchedulerWithBuiltInCoolingBehavior.class);
    protected final List<AttackNode<T>> nodes;                // 静态节点列表
    protected final Function<T, List<AttackNode<T>>> dynamicFactory;                // 动态节点列表
    protected MemoryModuleType<Unit> cooldownMemory = null;    // 内置冷却
    protected final float attackCoolingDownFactor;               // 全局冷却因子
    protected int totalCooldown;                              // 内置总冷却
    protected AttackNode<T> currentNode;                      // 当前节点
    protected int tick;                                       // 计数器

    public AttackSchedulerWithBuiltInCoolingBehavior(AttackNode<T> node, MemoryModuleType<Unit> cooldownMemory) {
        this(node, mob -> List.of(), cooldownMemory);
    }

    public AttackSchedulerWithBuiltInCoolingBehavior(AttackNode<T> node, Function<T, List<AttackNode<T>>> dynamicFactory, MemoryModuleType<Unit> cooldownMemory) {
        this(node, dynamicFactory, cooldownMemory, 0.16667f);
    }

    public AttackSchedulerWithBuiltInCoolingBehavior(AttackNode<T> node, Function<T, List<AttackNode<T>>> dynamicFactory, MemoryModuleType<Unit> cooldownMemory, float attackCoolingDownFactor) {
        this(List.of(node), dynamicFactory, cooldownMemory, attackCoolingDownFactor);
    }

    public AttackSchedulerWithBuiltInCoolingBehavior(List<AttackNode<T>> nodes, MemoryModuleType<Unit> cooldownMemory) {
        this(nodes, mob -> List.of(), cooldownMemory);
    }

    public AttackSchedulerWithBuiltInCoolingBehavior(List<AttackNode<T>> nodes, Function<T, List<AttackNode<T>>> dynamicFactory, MemoryModuleType<Unit> cooldownMemory) {
        this(nodes, dynamicFactory, cooldownMemory, 0.16667f);
    }

    public AttackSchedulerWithBuiltInCoolingBehavior(List<AttackNode<T>> nodes, Function<T, List<AttackNode<T>>> dynamicFactory, MemoryModuleType<Unit> cooldownMemory, float attackCoolingDownFactor) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT,MemoryModuleTypes.ATTACKING.get(),MemoryStatus.REGISTERED, cooldownMemory, MemoryStatus.VALUE_ABSENT),Integer.MAX_VALUE);
        this.nodes = nodes;
        this.dynamicFactory = dynamicFactory;
        this.cooldownMemory = cooldownMemory;
        this.attackCoolingDownFactor = attackCoolingDownFactor;
        log.info("动态工厂：{}",dynamicFactory);
    }


    public AttackSchedulerWithBuiltInCoolingBehavior(List<AttackNode<T>> nodes, Function<T, List<AttackNode<T>>> dynamicFactory) {
        this(nodes, dynamicFactory, 0.16667f);
    }
    public AttackSchedulerWithBuiltInCoolingBehavior(List<AttackNode<T>> nodes, Function<T, List<AttackNode<T>>> dynamicFactory, float attackCoolingDownFactor) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT,MemoryModuleTypes.ATTACKING.get(),MemoryStatus.VALUE_ABSENT),Integer.MAX_VALUE);
        this.nodes = nodes;
        this.dynamicFactory = dynamicFactory;
        this.attackCoolingDownFactor = attackCoolingDownFactor;
    }
    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull T mob) {
        return true;
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        currentNode = null;
        tick = 0;
        totalCooldown = 0;
        mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent((target) -> {
            List<AttackNode<T>> candidates = new ArrayList<>();
            // 添加静态节点
            for (AttackNode<T> node : nodes) {
                if (node.canUse(mob, target)) {
                    candidates.add(node);
                }
            }
            // 添加动态节点
            List<AttackNode<T>> dynamicNodes = dynamicFactory.apply(mob);
            if (dynamicNodes != null) {
                for (AttackNode<T> node : dynamicNodes) {
                    if (node.canUse(mob, target)) {
                        candidates.add(node);
                    }
                }
            }
            if (candidates.isEmpty()) return;
            currentNode = selectNodeByWeight(candidates, mob, target, mob.getRandom());
            mob.getBrain().setMemory(MemoryModuleTypes.ATTACKING.get(),Unit.INSTANCE);
        });
    }

    @Override
    protected void tick(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        if (currentNode == null) {
            doStop(level, mob, gameTime);
            return;
        }
        Optional<LivingEntity> targetOptional = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOptional.isPresent()) {
            LivingEntity target = targetOptional.get();
            if (tick == 0) {
                totalCooldown += currentNode.getCooldown();
                if (currentNode.getAnimId() != null) {
                    PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimPacket(mob.getId(), currentNode.getAnimId()));
                }
            }
            if (currentNode.tick(mob, target, tick)) {
                // 尝试派生
                if (!currentNode.getChildren().isEmpty()) {
                    List<AttackNode<T>> available = new ArrayList<>();
                    for (AttackNode<T> child : currentNode.getChildren()) {
                        if (child.canUse(mob, target)) {
                            available.add(child);
                        }
                    }
                    if (available.isEmpty()) {
                        doStop(level, mob, gameTime);
                    } else {
                        currentNode = selectNodeByWeight(available, mob, target, mob.getRandom());
                        tick = 0;
                    }
                    return;
                }
                doStop(level, mob, gameTime);
            } else if (tick > 2000) {
                totalCooldown += currentNode.getCooldown();
                doStop(level, mob, gameTime);
            }
            tick++;
        } else {
            doStop(level, mob, gameTime);
        }
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        return currentNode != null;
    }

    @Override
    protected void stop(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        mob.getBrain().setMemoryWithExpiry(cooldownMemory, Unit.INSTANCE, totalCooldown);
        mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN,true, (int) (totalCooldown * attackCoolingDownFactor));
        mob.getBrain().eraseMemory(MemoryModuleTypes.ATTACKING.get());

        PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimPacket(mob.getId(), (byte) -1));
        currentNode = null;
    }

    private AttackNode<T> selectNodeByWeight(List<AttackNode<T>> nodes, T mob, LivingEntity target, RandomSource random) {
        double total = 0.0;
        for (AttackNode<T> node : nodes) {
            total += node.getWeight(mob, target);
        }
        if (total <= 0) return nodes.getFirst();
        log.debug("总权重：{}",total);
        double r = random.nextDouble() * total;
        for (AttackNode<T> node : nodes) {
            double weight = node.getWeight(mob, target);
            if (r < weight) {
                if(target instanceof Player player){
                    log.debug("选中节点：{}，权重：{}，剩余随机值：{}，距离：{},玩家已知速度：{}，玩家已知水平速度：{}", node, weight, r,mob.distanceTo(target),player.getKnownMovement().length(),player.getKnownMovement().horizontalDistance());
                }else{
                    log.debug("选中节点：{}，权重：{}，剩余随机值：{}，距离：{},目标速度：{}，目标水平速度：{}", node, weight, r,mob.distanceTo(target),target.getDeltaMovement().length(),target.getDeltaMovement().horizontalDistance());
                }
                return node;
            }
            r -= weight;
        }
        return nodes.getFirst();
    }
}