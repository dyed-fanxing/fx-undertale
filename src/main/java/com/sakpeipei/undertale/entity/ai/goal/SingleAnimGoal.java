package com.sakpeipei.undertale.entity.ai.goal;

import com.sakpeipei.undertale.common.anim.SingleAnim;
import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.net.packet.AnimPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sakqiongzi
 * @since 2025-11-22 21:46
 * 单个动画执行GOAL，需要维护服务端动画Tick
 */
public abstract class SingleAnimGoal<T,R extends Mob & IAnimatable> extends Goal {
    protected final R mob;
    protected int tick;             // 动画Tick
    protected int cooldownEndTick;  // 动画冷却结束Tick点
    protected int length;           // 动画长度
    protected SingleAnim<T> anim;

    public SingleAnimGoal(R mob) {
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
        LivingEntity target = mob.getTarget();
        if(target != null){
            anim = select(target);
            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),anim.id(),1.0f));
        }
    }

    /**
     *  goal系统的分成两步执行，偶数tickCount才会执行canUse和canContinueToUse进判断
     *  奇数不会，奇数在开启了requiresUpdateEveryTick的情况下，会直接执行tick
     *  所以建议动画时长设置成偶数，不要奇数，否则会多执行一tick
     */
    @Override
    public boolean canContinueToUse() {
        return tick < anim.length();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if(target != null){
            if(anim.shouldHitAt(tick)){
                // 执行攻击时返回的额外动画时间 - 判定生效时剩余的动画时间，如果大于0，则代表这次攻击动画的时间比预设的多，需要增加动画冷却时间
                cooldownEndTick = Math.max(execute(target) - anim.length() + tick,0);
            }
        }
        tick++;
    }
    /**
     * 选择动画anim
     */
    @NotNull
    protected abstract SingleAnim<T> select(LivingEntity target);

    /**
     * @param target 目标
     * @return 返回需要补充动画CD的额外CD
     */
    protected abstract int execute(LivingEntity target);

    @Override
    public void stop() {
        cooldownEndTick += anim.cd() + mob.tickCount;
        PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),(byte) -1,0f));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}