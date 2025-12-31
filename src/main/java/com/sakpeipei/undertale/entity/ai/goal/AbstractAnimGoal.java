package com.sakpeipei.undertale.entity.ai.goal;

import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.entity.common.anim.AnimType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.NotNull;


/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:21
 * 带有服务端触发客户端AnimType接口的任意动画类型的GOAL执行器
 */
public abstract class AbstractAnimGoal extends Goal {
    protected final Mob mob;
    protected int animTick;
    protected int cooldownEndTick;

    public AbstractAnimGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target) && mob.tickCount >= cooldownEndTick;
    }

    @Override
    public void start() {
        animTick = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return animTick < getDuration();
    }

    @Override
    public void tick() {
        animTick++;
    }

    protected abstract int getDuration();

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
