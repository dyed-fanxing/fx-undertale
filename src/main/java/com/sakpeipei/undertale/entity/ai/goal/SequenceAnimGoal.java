package com.sakpeipei.undertale.entity.ai.goal;

import com.sakpeipei.undertale.common.anim.SequenceAnim;
import com.sakpeipei.undertale.common.anim.SingleAnim;
import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.network.AnimPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:21
 * 带有服务端触发客户端AnimType接口的任意动画类型的GOAL执行器
 */
public abstract class SequenceAnimGoal<T,R extends Mob & IAnimatable> extends Goal {
    protected final R mob;
    protected int tick;             // 动画tick
    protected int cooldownEndTick;  // 动画结束冷却Tick点
    
    protected int step;             // 当前步骤索引
    protected int startStepTick;    // 开始执行当前动画步骤的Tick点
    protected int stepCooldown;     // 单个动画步骤的冷却时间
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
        stepCooldown = 0;
        startStepTick = 1;
        LivingEntity target = mob.getTarget();
        if (target != null) {
            anim = select(target);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return step < anim.getSteps().size();
    }

    @Override
    public void tick() {
        if(stepCooldown > 0){
            stepCooldown--;
            if(stepCooldown == 0){
                step++;
            }
            return;
        }else{
            tick++;
        }
        List<SingleAnim<T>> steps = anim.getSteps();
        SingleAnim<T> curr = steps.get(step);
        if(tick == startStepTick){
            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),curr.getId(),1.0f));
        }
        if(curr.shouldHitAt(tick)) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                // 执行攻击时返回的额外动画时间 - 判定生效时剩余的动画时间，如果大于0，则代表这次攻击动画的时间比预设的多，需要增加动画持续时间
                int remaining = execute(target, curr) - (curr.getLength() - tick);
                if(remaining > 0){
                    anim.applyOffset(step, remaining);
                }
            }
        } else if(tick == curr.getLength()){
            stepCooldown = curr.getCd();
            startStepTick = curr.getLength() + 1;
            stepStop();
        }
    }


    /**
     * 动画执行完成
     */
    @Override
    public void stop() {
        cooldownEndTick = anim.getCd() + mob.tickCount;
        PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),(byte) -1,0f));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @NotNull
    protected abstract SequenceAnim<T> select(LivingEntity target);

    protected abstract int execute(LivingEntity target, SingleAnim<T> anim);

    protected void stepStop(){
    }
}
