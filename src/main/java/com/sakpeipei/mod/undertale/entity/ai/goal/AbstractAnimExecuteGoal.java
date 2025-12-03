package com.sakpeipei.mod.undertale.entity.ai.goal;

import com.sakpeipei.mod.undertale.entity.IAnimatable;
import com.sakpeipei.mod.undertale.entity.common.AnimType;
import com.sakpeipei.mod.undertale.network.AnimIDPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;


/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:21
 * 带有服务端触发客户端AnimType接口的任意动画类型的GOAL执行器
 */
public abstract class AbstractAnimExecuteGoal<T,R extends Mob & IAnimatable> extends Goal {
    protected final R mob;
    protected int animStartTick;
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
        animStartTick = mob.tickCount;
        LivingEntity target = mob.getTarget();
        if (target != null) {
            if(anim == null){
                anim = select(target);
            }
            if (anim.isTriggerAnim()) {
                triggerAnim(anim);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return mob.tickCount - animStartTick < anim.getDuration();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            int animTick = mob.tickCount - animStartTick;
            if (anim.shouldHitAt(animTick)) {
                onHit(target,animTick);
            }
        }
    }


    /**
     * 内部动画单元执行完
     */
    @Override
    public void stop() {
        cooldownEndTick += anim.getCd() + mob.tickCount;
        if(anim.isCompeted()){
            onCompleted();
            anim = null;
        }
        mob.setAnimID((byte)0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * 触发动画
     * @param anim
     */
    protected void triggerAnim(AnimType<T> anim){
        mob.setAnimID(anim.getId());
    }
    /**
     * 当判定时
     * @param target 目标
     * @param animTick 动画Tick
     */
    protected void onHit(LivingEntity target, int animTick) {
        cooldownEndTick = Math.max(execute(target, anim) - (anim.getDuration() - animTick), 0);
    }

    /**
     * 整个动画执行结束
     */
    protected void onCompleted(){
    }


    @NotNull
    protected abstract AnimType<T> select(LivingEntity target);

    protected abstract int execute(LivingEntity target, AnimType<T> anim);


}
