//package com.sakpeipei.undertale.entity.ai.goal;
//
//import com.sakpeipei.undertale.entity.IAnimatable;
//import com.sakpeipei.undertale.entity.common.anim.AnimType;
//import com.sakpeipei.undertale.entity.common.anim.Step;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.Mob;
//import net.minecraft.world.entity.ai.goal.Goal;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.List;
//
//
///**
// * @author Sakqiongzi
// * @since 2025-11-23 21:21
// * 带有服务端触发客户端AnimType接口的任意动画类型的GOAL执行器
// */
//public abstract class AbstractSequeueAnimGoal<T,R extends Mob & IAnimatable> extends Goal {
//    protected final R mob;
//    protected int tick; // 动画tick
//    protected int cooldownEndTick;
//    protected List<Step<T>> steps;
//    protected int step;
//    protected int duration;
//
//    public AbstractSequeueAnimGoal(R mob) {
//        this.mob = mob;
//    }
//
//    @Override
//    public boolean canUse() {
//        LivingEntity target = mob.getTarget();
//        return target != null && target.isAlive() && mob.canAttack(target) && mob.tickCount >= cooldownEndTick;
//    }
//
//    @Override
//    public void start() {
//        tick = 0;
//        step = 0;
//        LivingEntity target = mob.getTarget();
//        if (target != null) {
//            steps = select(target);
//        }
//    }
//
//    @Override
//    public boolean canContinueToUse() {
//        return tick < duration;
//    }
//
//    @Override
//    public void tick() {
//        Step<T> curr = steps.get(step);
//        if(curr.getAnimTick() == tick){
//            mob.setAnimID(curr.getId());
//        }
//        if(curr.getHitTick() == tick) {
//            LivingEntity target = mob.getTarget();
//            if (target != null) {
//                // 执行攻击时返回的额外动画时间 - 判定生效时剩余的动画时间，如果大于0，则代表这次攻击动画的时间比预设的多，需要增加动画持续时间
//                int remaining = execute(target, steps) - (duration - tick);
//                if(remaining > 0){
//                    this.duration += remaining;
//                }
//            }
//            step++;
//        }
//        tick++;
//    }
//
//
//    /**
//     * 动画执行完成
//     */
//    @Override
//    public void stop() {
//        cooldownEndTick = anim.getCd() + mob.tickCount;
//        mob.setAnimID((byte)-1);
//    }
//
//    @Override
//    public boolean requiresUpdateEveryTick() {
//        return true;
//    }
//
//    @NotNull
//    protected abstract List<Step<T>> select(LivingEntity target);
//
//    protected abstract int execute(LivingEntity target, AnimType<T> anim);
//
//
//}
