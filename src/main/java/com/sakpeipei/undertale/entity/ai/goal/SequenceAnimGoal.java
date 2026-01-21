package com.sakpeipei.undertale.entity.ai.goal;

import com.sakpeipei.undertale.common.anim.SequenceAnim;
import com.sakpeipei.undertale.common.anim.SingleAnim;
import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.network.AnimPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:21
 * 序列动画执行器
 */
public abstract class SequenceAnimGoal<T,R extends Mob & IAnimatable> extends Goal {
    private static final Logger log = LogManager.getLogger(SequenceAnimGoal.class);
    protected final R mob;
    protected int tick;             // 动画tick
    protected int cooldownEndTick;  // 动画结束冷却Tick点

    protected int step;             // 当前步骤索引
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
        LivingEntity target = mob.getTarget();
        if (target != null) {
            anim = select(target);
            log.debug("选择动画，动画步骤：{}，判定Tick：{}",anim.steps(), anim.cd());
        }
    }

    @Override
    public boolean canContinueToUse() { return step < anim.steps().size() && tick < anim.steps().getLast().length(); }
    @Override
    public void tick() {
        if(mob.tickCount < cooldownEndTick ){
            return;
        }

        List<SingleAnim<T>> steps = anim.steps();
        SingleAnim<T> curr = steps.get(step);
        if(tick == 0){
            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),curr.id(),1.0f));
        }
        if(curr.shouldHitAt(tick)) {
            LivingEntity target = mob.getTarget();
            // 只在开发环境（IDE运行）中显示消息
            if (!FMLEnvironment.production) {
                log.debug("发生判定，动画ID：{}，判定Tick：{}",curr.id(),curr.hitTicks());
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("发生判定：动画ID：%d，判定Ticks：%s",curr.id(), Arrays.toString(curr.hitTicks()))),false);
            }
            if (target != null) {
                cooldownEndTick = Math.max(execute(target,curr) - curr.length() + tick,0);
                int[] ints = curr.hitTicks();
                if(tick == ints[ints.length - 1]){
                    step++;
                }
            }
        }

        tick++;

        if(tick == curr.length()){
            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(), (byte) -1,0.0f));
            stepStop(curr);
            if (!FMLEnvironment.production) {
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("步骤动画结束：步骤索引：%d,动画ID：%d，冷却时间：%d",step,curr.id(),cooldownEndTick-mob.tickCount)),false);
            }
        }
    }

    protected void stepStop(SingleAnim<T> curr){
        //最终冷却结束时间 = 偏移 + 当前时间 + 基础冷却
        cooldownEndTick += mob.tickCount + curr.cd() ;
    }
    /**
     * 动画执行完成
     */
    @Override
    public void stop() {
        cooldownEndTick = anim.cd() + mob.tickCount;
        PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),(byte) -1,0f));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @NotNull
    protected abstract SequenceAnim<T> select(LivingEntity target);

    protected abstract int execute(LivingEntity target, SingleAnim<T> anim);

}
