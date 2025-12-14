package com.sakpeipei.mod.undertale.entity.ai.goal;

import com.sakpeipei.mod.undertale.entity.common.anim.AnimType;
import com.sakpeipei.mod.undertale.network.AnimIDPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sakqiongzi
 * @since 2025-11-22 21:46
 * 多个动画执行GOAL，需要维护服务端动画Tick
 */
public abstract class SequenceAnimExecuteGoal<T> extends Goal {
    protected final Mob mob;
    protected int animStartTick;  // 动画开始Tick点
    protected int cooldownEndTick; // 冷却结束Tick点
    protected AnimType<T> anim;

    public SequenceAnimExecuteGoal(Mob mob) {
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
        LivingEntity target = mob.getTarget();
        if(target != null){
            anim = select(target);
            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimIDPacket(mob.getId(), anim.getId()));
        }
    }

    /**
     *  goal系统的分成两步执行，偶数tickCount才会执行canUse和canContinueToUse进判断
     *  奇数不会，奇数在开启了requiresUpdateEveryTick的情况下，会直接执行tick
     *  所以建议动画时长设置成偶数，不要奇数，否则会多执行一tick
     */
    @Override
    public boolean canContinueToUse() {
        return mob.tickCount - animStartTick < anim.getDuration();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if(target != null){
            int animTick = mob.tickCount - animStartTick;
            if(anim.shouldHitAt(animTick)){
                cooldownEndTick += Math.max(execute(target,anim) - (anim.getDuration() - animTick),0) ;
            }
        }
    }
    /**
     * 选择动画anim
     */
    @NotNull
    protected abstract AnimType<T> select(LivingEntity target);

    /**
     * @param target 目标
     * @param anim 动画数据
     * @return 返回需要补充动画CD的额外CD
     */
    protected abstract int execute(LivingEntity target, AnimType<T> anim);

    @Override
    public void stop() {
        cooldownEndTick += anim.getCd();
    }

    protected void onComplete(){}

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}