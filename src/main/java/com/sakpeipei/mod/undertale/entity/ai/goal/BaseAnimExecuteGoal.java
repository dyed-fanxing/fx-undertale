package com.sakpeipei.mod.undertale.entity.ai.goal;

import com.sakpeipei.mod.undertale.entity.common.IAnimType;
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
public abstract class BaseAnimExecuteGoal<T> extends Goal {
    protected final Mob mob;
    protected int animStartTick;
    protected int cooldownEndTick;
    protected IAnimType<T> anim; // 动画对象，可以是AnimType或SequenceAnim

    public BaseAnimExecuteGoal(Mob mob) {
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
            if (shouldTriggerAnim()) {
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

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // 抽象方法
    @NotNull
    protected abstract IAnimType<T> select(LivingEntity target);
    protected abstract int execute(LivingEntity target, int animTick);

    // 可重写的方法
    protected boolean shouldTriggerAnim() {
        return true;
    }

    protected void onComplete() {}
}
