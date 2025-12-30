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
public abstract class AbstractAnimExecuteGoal<T,R extends Mob & IAnimatable> extends Goal {
    protected final R mob;
    protected int animTick;
    protected int cooldownEndTick;
    protected AnimType<T> anim; // 动画类型

    public AbstractAnimExecuteGoal(R mob) {
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
        LivingEntity target = mob.getTarget();
        if (target != null) {
            anim = select(target);
            mob.setAnimID(anim.getId());
        }
    }

    @Override
    public boolean canContinueToUse() {
        return animTick < anim.getDuration();
    }

    @Override
    public void tick() {
        if (anim.shouldHitAt(animTick)) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                // 执行攻击时返回的额外动画时间 - 判定生效时剩余的动画时间，如果大于0，则代表这次攻击动画的时间比预设的多，需要增加动画持续时间
                int remaining = execute(target, anim) - (anim.getDuration() - animTick);
                if(remaining > 0){
                    anim.addDuration(remaining);
                }
            }
        }
        animTick++;
    }


    /**
     * 动画执行完成
     */
    @Override
    public void stop() {
        cooldownEndTick = anim.getCd() + mob.tickCount;
        anim = null;
        mob.setAnimID((byte)0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @NotNull
    protected abstract AnimType<T> select(LivingEntity target);

    protected abstract int execute(LivingEntity target, AnimType<T> anim);


}
