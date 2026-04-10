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

import java.util.*;
import java.util.function.Function;


/**
 * 单个攻击组的调度行为。负责从组内选择节点并执行，管理派生，无内置CD，使用默认原版的CD
 */
public class AttackSchedulerWithoutBuiltlnCoolingBehavior<T extends LivingEntity> extends Behavior<T> {
    private static final Logger log = LoggerFactory.getLogger(AttackSchedulerWithoutBuiltlnCoolingBehavior.class);
    protected final List<AttackNode<T>> nodes;                          // 静态节点列表
    protected final Function<T, List<AttackNode<T>>> dynamicFactory;    // 动态节点列表
    protected AttackNode<T> currentNode;                                // 当前节点
    protected int tick;                                                 // 计数器
    protected List<AttackNode<T>> cachedCandidates;
    protected int timeout;
    public AttackSchedulerWithoutBuiltlnCoolingBehavior(AttackNode<T> node) {
        this(node, mob -> List.of());
    }
    public AttackSchedulerWithoutBuiltlnCoolingBehavior(Function<T, List<AttackNode<T>>> dynamicFactory) {
        this(List.of(), dynamicFactory,2000);
    }
    public AttackSchedulerWithoutBuiltlnCoolingBehavior(AttackNode<T> node, Function<T, List<AttackNode<T>>> dynamicFactory) {
        this(List.of(node), dynamicFactory,2000);
    }
    public AttackSchedulerWithoutBuiltlnCoolingBehavior(AttackNode<T> node, Function<T, List<AttackNode<T>>> dynamicFactory,int timeout) {
        this(List.of(node), dynamicFactory,timeout);
    }
    public AttackSchedulerWithoutBuiltlnCoolingBehavior(List<AttackNode<T>> nodes, Function<T, List<AttackNode<T>>> dynamicFactory,int timeout) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT,MemoryModuleTypes.ATTACKING.get(), MemoryStatus.VALUE_ABSENT),Integer.MAX_VALUE);
        this.nodes = nodes;
        this.dynamicFactory = dynamicFactory;
        this.timeout = timeout;
    }

    /**
     * 获取允许启动的候选节点（满足 canUse 且被所有活跃节点允许）
     */
    private List<AttackNode<T>> getPermittedCandidates(T mob, LivingEntity target) {
        // 1. 收集所有满足 canUse 的节点（静态 + 动态）
        List<AttackNode<T>> candidates = new ArrayList<>();
        for (AttackNode<T> node : nodes) {
            if (node.canUse(mob, target)) candidates.add(node);
        }
        List<AttackNode<T>> dynamicNodes = dynamicFactory.apply(mob);
        if (dynamicNodes != null) {
            for (AttackNode<T> node : dynamicNodes) {
                if (node.canUse(mob, target)) candidates.add(node);
            }
        }
        if (candidates.isEmpty()) return Collections.emptyList();

        // 2. 获取当前活跃节点集合（存储节点引用）
        Set<AttackNode<? extends LivingEntity>> activeNodes = mob.getBrain().getMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get()).orElse(Collections.emptySet());
        if (activeNodes.isEmpty()) return candidates;

        // 3. 过滤：候选节点必须被所有活跃节点允许
        List<AttackNode<T>> permitted = new ArrayList<>();
        for (AttackNode<T> candidate : candidates) {
            boolean allowedByAll = true;
            for (AttackNode<? extends LivingEntity> active : activeNodes) {
                if (!active.isConcurrentAllowed(candidate.getId())) {
                    allowedByAll = false;
                    break;
                }
            }
            if (allowedByAll) permitted.add(candidate);
        }
        return permitted;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel level, @NotNull T mob) {
        Optional<LivingEntity> targetOpt = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) {
            cachedCandidates = null;
            return false;
        }
        cachedCandidates = getPermittedCandidates(mob, targetOpt.get());
        return !cachedCandidates.isEmpty();
    }

    @Override
    protected void start(@NotNull ServerLevel level, @NotNull T mob, long gameTime) {
        if (cachedCandidates == null || cachedCandidates.isEmpty()) {
            doStop(level, mob, gameTime);
            return;
        }
        LivingEntity target = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null) {
            doStop(level, mob, gameTime);
            return;
        }

        currentNode = selectNodeByWeight(cachedCandidates, mob, target, mob.getRandom());
        cachedCandidates = null;
        // 加入活跃集合
        Set<AttackNode<?>> activeSet = mob.getBrain()
                .getMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get())
                .orElse(new HashSet<>());
        activeSet.add(currentNode);
        mob.getBrain().setMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get(), activeSet);

        if (currentNode.isControlMove()) {
            mob.getBrain().setMemory(MemoryModuleTypes.MOVE_LOCKING.get(), Unit.INSTANCE);
        }
        mob.getBrain().setMemory(MemoryModuleTypes.ATTACKING.get(), Unit.INSTANCE);
        tick = 0;
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
                if (currentNode.getAnimId() != null) {
                    Set<AttackNode<?>> activeNodes = mob.getBrain().getMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get()).orElse(Collections.emptySet());
                    int maxPriority = activeNodes.stream().mapToInt(AttackNode::getPriority).max().orElse(0);
                    if (currentNode.getPriority() >= maxPriority) {
                        PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimPacket(mob.getId(), currentNode.getAnimId()));
                    }
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
                        // 移除旧节点
                        Set<AttackNode<?>> activeSet = mob.getBrain().getMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get()).orElse(new HashSet<>());
                        activeSet.remove(currentNode);
                        // 选择新节点（子节点）
                        currentNode = selectNodeByWeight(available, mob, target, mob.getRandom());
                        if (activeSet.stream().noneMatch(AttackNode::isControlMove)){
                            mob.getBrain().eraseMemory(MemoryModuleTypes.MOVE_LOCKING.get());
                        }
                        if (currentNode.isControlMove()) {
                            mob.getBrain().setMemory(MemoryModuleTypes.MOVE_LOCKING.get(), Unit.INSTANCE);
                        }
                        // 添加新节点
                        activeSet.add(currentNode);
                        mob.getBrain().setMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get(), activeSet);
                        tick = 0;
                    }
                    return;
                }
                doStop(level, mob, gameTime);
            } else if (tick > timeout) {
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
        // 从活跃集合中移除当前节点
        Set<AttackNode<?>> activeSet = mob.getBrain().getMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get()).orElse(new HashSet<>());
        int remainingMaxPriority = activeSet.stream().mapToInt(AttackNode::getPriority).max().orElse(-1);
        log.debug("Stop: activeNodes：{},maxPriority: {},currentNode.getPriority：{},current.animId：{}",activeSet,remainingMaxPriority,currentNode.getPriority(),currentNode.getAnimId());
        if (currentNode != null) {
//            Set<AttackNode<?>> activeSet = mob.getBrain().getMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get()).orElse(new HashSet<>());
//            int remainingMaxPriority = activeSet.stream().mapToInt(AttackNode::getPriority).max().orElse(-1);
            log.debug("Stop: activeNodes：{},maxPriority: {},currentNode.getPriority：{},current.animId：{}",activeSet,remainingMaxPriority,currentNode.getPriority(),currentNode.getAnimId());
            if (currentNode.getPriority() == remainingMaxPriority) {
                PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimPacket(mob.getId(),-1));
            }
            activeSet.remove(currentNode);
            if (activeSet.isEmpty()) mob.getBrain().eraseMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get());
            else mob.getBrain().setMemory(MemoryModuleTypes.ACTIVE_ATTACK_NODES.get(), activeSet);
        }

        mob.getBrain().eraseMemory(MemoryModuleTypes.ATTACKING.get());
        if (activeSet.stream().noneMatch(AttackNode::isControlMove)){
            mob.getBrain().eraseMemory(MemoryModuleTypes.MOVE_LOCKING.get());
        }


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