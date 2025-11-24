//package com.sakpeipei.mod.undertale.entity.ai.goal;
//
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.Mob;
//import net.minecraft.world.entity.ai.goal.Goal;
//
//import java.util.EnumSet;
//
///**
// * @author Sakqiongzi
// * @since 2025-11-22 17:40
// */
//public abstract class AbstractSpellGoal extends Goal {
//    protected final Mob mob;
//    protected LivingEntity target;
//    protected final double speedModifier;
//    protected int seeTime;
//    protected boolean hasLineOfSight;
//
//    // 攻击范围配置
//    protected float attackRange = 16.0f;
//    protected float attackRangeSqr = attackRange * attackRange;
//    protected float minRange = 4.0f;
//    protected float minRangeSqr = minRange * minRange;
//
//    public AbstractSpellGoal(Mob sans, double speedModifier) {
//        this.mob = sans;
//        this.speedModifier = speedModifier;
//        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
//    }
//
//    @Override
//    public boolean canUse() {
//        target = mob.getTarget();
//        return target != null && target.isAlive();
//    }
//
//    @Override
//    public boolean canContinueToUse() {
//        return canUse() && !mob.isAttacking;
//    }
//
//    @Override
//    public void start() {
//        sans.isAttacking = true;
//        seeTime = sans.seeTime; // 继承当前视线时间
//    }
//
//    @Override
//    public void stop() {
//        sans.isAttacking = false;
//        target = null;
//        seeTime = 0;
//    }
//
//    @Override
//    public void tick() {
//        // 更新视线状态
//        updateSightStatus();
//
//        // 基础移动逻辑
//        handleMovement();
//
//        // 抽象攻击逻辑，由子类实现
//        if (shouldAttack()) {
//            handleAttack();
//        }
//    }
//
//    protected void updateSightStatus() {
//        hasLineOfSight = mob.getSensing().hasLineOfSight(target);
//        if (hasLineOfSight) {
//            seeTime = Math.min(seeTime + 1, 40);
//        } else {
//            seeTime = Math.max(seeTime - 1, 0);
//        }
//    }
//
//    protected void handleMovement() {
//        double distanceSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
//        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
//        if (seeTime > 0) {
//            // 有视线时的移动策略
//            if (distanceSqr <= minRangeSqr) {
//                // 太近：后退
//                mob.getNavigation().stop();
//                mob.getMoveControl().strafe(-0.75f, 0.0f);
//            } else if (distanceSqr > minRangeSqr && distanceSqr <= attackRangeSqr) {
//                // 攻击距离内：停止移动准备攻击
//                mob.getNavigation().stop();
//            } else {
//                // 追击目标
//                mob.getNavigation().moveTo(target, speedModifier);
//                if (distanceSqr > attackRangeSqr * 1.5f) {
//                    // 太远时传送
//                    teleportTowards(target);
//                }
//            }
//        } else {
//            // 没有视线：向目标位置移动
//            mob.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), speedModifier);
//        }
//    }
//
//    protected boolean shouldAttack() {
//        double distanceSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
//        return seeTime >= 10 &&
//                distanceSqr > minRangeSqr &&
//                distanceSqr <= attackRangeSqr &&
//                hasLineOfSight;
//    }
//
//    // 抽象方法，子类实现具体攻击逻辑
//    protected abstract void handleAttack();
//
//    // 工具方法
//    protected void teleportTowards(Entity target) {
//        Vec3 dir = new Vec3(target.getX() - sans.getX(), target.getEyeY() - sans.getEyeY(), target.getZ() - sans.getZ());
//        dir = dir.normalize().scale((double) attackRange / 2);
//        sans.tryTeleportTo(
//                sans.getX() + dir.x + (sans.random.nextDouble() - 0.5) * 4,
//                sans.getY() + dir.y + (sans.random.nextDouble() - 0.5) * 16,
//                sans.getZ() + dir.z + (sans.random.nextDouble() - 0.5) * 4
//        );
//    }
//
//    // 可重写的方法
//    protected float getAttackRange() {
//        return attackRange;
//    }
//
//    protected float getMinRange() {
//        return minRange;
//    }
//
//    protected int getRequiredSeeTime() {
//        return 10;
//    }
//}