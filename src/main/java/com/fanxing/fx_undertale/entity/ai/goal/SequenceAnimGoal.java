package com.fanxing.fx_undertale.entity.ai.goal;

import com.fanxing.fx_undertale.entity.ai.anim.AnimStep;
import com.fanxing.fx_undertale.entity.ai.anim.SequenceAnim;
import com.fanxing.fx_undertale.entity.IAnimatable;
import com.fanxing.fx_undertale.net.packet.AnimPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * @author FanXing
 * @since 2025-11-23 21:21
 * 序列动画执行器
 */
public abstract class SequenceAnimGoal<R extends Mob & IAnimatable> extends Goal {
    private static final Logger log = LogManager.getLogger(SequenceAnimGoal.class);
    protected final R mob;
    protected int tick;             // 动画tick
    protected int cooldownEndTick;  // 动画结束冷却Tick点

    protected int duration;
    protected int step;             // 当前步骤索引
    protected SequenceAnim anim;

    public SequenceAnimGoal(R mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target) && mob.tickCount >= cooldownEndTick;
    }

    @Override
    public void start() {
        tick = 0;
        step = 0;
        LivingEntity target = mob.getTarget();
        if (target != null) {
            anim = select(target);
            duration = anim.steps().getFirst().duration();
            log.debug("选择动画，动画步骤：{}，判定Tick：{}", anim.steps(), anim.cooldown());
        }
    }

    @Override
    public boolean canContinueToUse() {
        return step < anim.steps().size() && tick < duration;
    }

    @Override
    public void tick() {
        List<AnimStep> steps = anim.steps();
        if (step >= steps.size()) {
            return;
        }
        AnimStep curr = steps.get(step);
        if (tick == 0) {
            if (curr.id() != null){
                PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimPacket(mob.getId(), curr.id(), 1.0f));
            }
            duration = curr.duration();
        }
        if (curr.shouldHitAt(tick)) {
            LivingEntity target = mob.getTarget();
            // 只在开发环境（IDE运行）中显示消息
            if (!FMLEnvironment.production) {
                log.debug("发生判定，动画ID：{}，判定Tick：{}", curr.id(), curr.hitTicks());
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("发生判定：动画ID：%d，判定Ticks：%s", curr.id(), Arrays.toString(curr.hitTicks()))), false);
            }
            if (target != null) {
                duration += Math.max(curr.action().applyAsInt(target) - curr.duration() + tick, 0);
            }
        }

        tick++;

        if (tick == duration) {
            tick = 0;
            step++;
            if (!FMLEnvironment.production) {
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("步骤动画结束：步骤索引：%d,动画ID：%d，动画时长：%d", step, curr.id(), duration)), false);
            }
        }
    }

    /**
     * 动画执行完成
     */
    @Override
    public void stop() {
        cooldownEndTick = anim.cooldown() + mob.tickCount;
        PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimPacket(mob.getId(), (byte) -1, 0f));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @NotNull
    protected abstract SequenceAnim select(LivingEntity target);

}
