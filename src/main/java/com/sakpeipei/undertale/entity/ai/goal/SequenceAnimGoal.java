package com.sakpeipei.undertale.entity.ai.goal;

import com.sakpeipei.undertale.common.anim.AnimStep;
import com.sakpeipei.undertale.common.anim.SequenceAnim;
import com.sakpeipei.undertale.entity.IAnimatable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:21
 * 带有服务端触发客户端AnimType接口的任意动画类型的GOAL执行器
 */
public abstract class SequenceAnimGoal<T,R extends Mob & IAnimatable> extends Goal {
    protected final R mob;
    protected int tick; // 动画tick
    protected int cooldownEndTick;
    protected int step;
    protected SequenceAnim<T> anim;

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
        }
    }

    @Override
    public boolean canContinueToUse() {
        return tick < anim.getDuration();
    }

    @Override
    public void tick() {
        List<AnimStep<T>> steps = anim.getSteps();
        AnimStep<T> curr = steps.get(step);
        if(curr.shouldTriggerAnim(tick)){
            mob.setAnimID(curr.getId());
        }
        if(curr.shouldHitAt(tick)) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                // 执行攻击时返回的额外动画时间 - 判定生效时剩余的动画时间，如果大于0，则代表这次攻击动画的时间比预设的多，需要增加动画持续时间
                int remaining = execute(target, curr) - (anim.getDuration() - tick);
                if(remaining > 0){
                    anim.addDuration(step, remaining);
                }
            }
            step++;
        }
        tick++;
    }


    /**
     * 动画执行完成
     */
    @Override
    public void stop() {
        cooldownEndTick = anim.getCd() + mob.tickCount;
        mob.setAnimID((byte)-1);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @NotNull
    protected abstract SequenceAnim<T> select(LivingEntity target);

    protected abstract int execute(LivingEntity target, AnimStep<T> anim);


}
