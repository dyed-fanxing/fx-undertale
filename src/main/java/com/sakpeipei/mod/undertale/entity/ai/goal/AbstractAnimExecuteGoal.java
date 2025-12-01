package com.sakpeipei.mod.undertale.entity.ai.goal;

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
 */
public abstract class AbstractAnimExecuteGoal<T> extends Goal {
    protected final Mob mob;
    protected int animStartTick;
    protected int cooldownEndTick;
    protected AnimType<T> anim;

    public AbstractAnimExecuteGoal(Mob mob) {
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
                if (anim.isTriggerAnim()) {
                    PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimIDPacket(mob.getId(), anim.getId()));
                }
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
                cooldownEndTick += Math.max(execute(target, anim) - (anim.getDuration() - animTick), 0);
            }
        }
    }


    /**
     * 执行完一个动画该做的收尾工作
     */
    @Override
    public void stop() {
        cooldownEndTick += anim.getCd();
        if(anim.isCompeted()){
            onCompleted();
            anim = null;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // 抽象方法
    @NotNull
    protected abstract AnimType<T> select(LivingEntity target);
    protected abstract int execute(LivingEntity target, AnimType<T> anim);


    protected void onCompleted(){
        PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimIDPacket(mob.getId(), (byte) 0));
    }

}
