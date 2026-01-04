package com.sakpeipei.undertale.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;


/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:21
 * 带有服务端触发客户端AnimType接口的任意动画类型的GOAL执行器
 */
public abstract class AnimGoal extends Goal {
    protected final Mob mob;
    protected int tick;
    protected int cooldownEndTick;

    public AnimGoal(Mob mob) {
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
    }

    /**
     *  goal系统的分成两步执行，偶数tickCount才会执行canUse和canContinueToUse进判断
     *  奇数不会，奇数在开启了requiresUpdateEveryTick的情况下，会直接执行tick
     *  所以建议动画时长设置成偶数，不要奇数，否则会多执行一tick
     */
    @Override
    public boolean canContinueToUse() {
        return tick < getDuration();
    }

    @Override
    public void tick() {
        tick++;
    }

    @Override
    public void stop() {
        this.cooldownEndTick = getCooldown() + mob.tickCount;
    }

    protected abstract int getDuration();
    protected abstract int getCooldown();

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
