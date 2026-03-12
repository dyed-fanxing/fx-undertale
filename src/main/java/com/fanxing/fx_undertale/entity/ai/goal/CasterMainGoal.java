package com.fanxing.fx_undertale.entity.ai.goal;

import com.fanxing.fx_undertale.entity.IAnimatable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public abstract class CasterMainGoal<T extends Mob&IAnimatable> extends Goal {
    protected final T mob;
    protected LivingEntity target;
    protected int seeTime;
    protected final float attackRadiusSqr;
    protected final float backRadiusSqr;
    protected final float pursuitRadiusSqr;
    protected final double speedModifier;

    public CasterMainGoal(T mob, double speedModifier, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.backRadiusSqr = this.attackRadiusSqr * 0.25f;
        this.pursuitRadiusSqr = this.attackRadiusSqr + this.attackRadiusSqr * 0.5f;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        seeTime = 0;
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            target = mob.getTarget();
            if (target == null) return;
        }

        double disSqr = mob.distanceToSqr(target);
        boolean hasSight = mob.getSensing().hasLineOfSight(target);
        if (hasSight) {
            seeTime++;
        } else {
            seeTime = Math.max(-60, seeTime - 1);
        }
        mob.getLookControl().setLookAt(target, mob.getHeadRotSpeed(), mob.getMaxHeadXRot());

        // 子类可控制是否跳过移动（如施法期间）
        if (shouldSkipMovement()) return;

        if (seeTime > 0) {
            if (disSqr <= backRadiusSqr) {
                handleRetreat(target);          // 距离过近，后退
            } else if (disSqr <= attackRadiusSqr) {

                mob.getNavigation().stop();    // 保持在攻击范围内，静止
            } else {
                mob.getNavigation().moveTo(target, speedModifier); // 接近目标
                if (disSqr > pursuitRadiusSqr) {
                    tryTeleportTowards(target); // 超出追击范围，尝试传送
                }
            }
        } else if (seeTime > -60) { // 丢失视线后仍追击一段时间
            if (disSqr <= pursuitRadiusSqr) {
                mob.getNavigation().moveTo(target, speedModifier);
            } else {
                tryTeleportTowards(target);
            }
        }
    }

    /**
     * 子类可重写以在特定条件下暂停移动（例如施法动画期间）
     */
    protected boolean shouldSkipMovement() {
        return false;
    }

    /**
     * 处理后退行为。子类必须实现具体逻辑（如智能寻路、传送等）
     */
    protected abstract void handleRetreat(LivingEntity target);

    /**
     * 尝试向目标传送。子类需实现具体的传送逻辑（如末影人式传送）
     */
    protected abstract void tryTeleportTowards(LivingEntity target);
}
