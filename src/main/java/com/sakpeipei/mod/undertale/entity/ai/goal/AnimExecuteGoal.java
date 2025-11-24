package com.sakpeipei.mod.undertale.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * @author Sakqiongzi
 * @since 2025-11-23 15:57
 */
public abstract class AnimExecuteGoal extends Goal {
    protected final Mob mob;
    protected int animStartTick;  // 动画开始Tick点
    protected int cooldownEndTick; // 冷却结束Tick点

    public AnimExecuteGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target) && mob.tickCount >= cooldownEndTick;
    }

    @Override
    public void start() {
        animStartTick = mob.tickCount;
    }

    /**
     *  goal系统的分成两步执行，偶数tickCount才会执行canUse和canContinueToUse进判断
     *  奇数不会，奇数在开启了requiresUpdateEveryTick的情况下，会直接执行tick
     *  所以建议动画时长设置成偶数，不要奇数，否则会多执行一tick
     */

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
