package com.sakpeipei.mod.undertale.entity.ai.goal;

import com.sakpeipei.mod.undertale.entity.common.AbstractAnimType;
import com.sakpeipei.mod.undertale.entity.common.AnimType;
import com.sakpeipei.mod.undertale.network.AnimIDPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:21
 */
public abstract class AbstractBaseAnimExecuteGoal<T> extends Goal {
    protected final Mob mob;
    protected int animStartTick;
    protected int cooldownEndTick;
    protected AnimType<T> anim;

    public AbstractBaseAnimExecuteGoal(Mob mob) {
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
            anim = select(target);
            if (anim.isTriggerAnim()) {
                PacketDistributor.sendToPlayersTrackingEntity(mob, new AnimIDPacket(mob.getId(), anim.getId()));
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
                int extraCooldown = execute(target, animTick);
                cooldownEndTick += Math.max(extraCooldown - (anim.getDuration() - animTick), 0);
            }
        }
    }


    /**
     * 执行完一个动画该做的收尾工作
     */
    @Override
    public void stop() {
        cooldownEndTick += anim.getCd();

    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // 抽象方法
    @NotNull
    protected abstract AnimType<T> select(LivingEntity target);
    protected abstract int execute(LivingEntity target, int animTick);

    protected void onComplete() {}

}
