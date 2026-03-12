package com.fanxing.fx_undertale.entity.ai.goal;

import com.fanxing.fx_undertale.entity.IAnimatable;
import com.fanxing.fx_undertale.entity.ai.anim.CastStep;
import com.fanxing.fx_undertale.net.packet.AnimPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;


public abstract class CastComboGoal<T extends Mob & IAnimatable> extends Goal {
    private static final Logger log = LoggerFactory.getLogger(CastComboGoal.class);
    protected final T mob;
    protected int tick;
    protected int duration;
    protected int nextStepDelay;
    protected int cooldownEndTick;
    protected int totalCooldown;

    protected List<CastStep> steps;
    protected int step;
    protected LivingEntity target;

    public CastComboGoal(T mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && mob.canAttack(target) && mob.tickCount >= cooldownEndTick;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && mob.canAttack(target) && mob.tickCount >= cooldownEndTick && step < steps.size();
    }

    @Override
    public void start() {
        target = mob.getTarget();
        step = 0;
        tick = 0;
        totalCooldown = 0;
        steps = select(target);
        // 只在开发环境（IDE运行）中显示消息
        if (!FMLEnvironment.production) {
            log.debug("select选择施法连击");
            Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("select选择施法连击")), false);
        }
    }

    @Override
    public void tick() {
        // 防止奇数Tick在step==steps.size时候进入
        target = mob.getTarget();
        if (target == null || step >= steps.size() || nextStepDelay-- > 0) {
            return;
        }
        CastStep curr = steps.get(step);

        if(tick == 0){
            if(curr.id() != null){
                mob.sendAnimPacket(curr.id());
            }
            duration = curr.duration();
        }
        int[] hits = curr.hitTicks();
        for (int hit : hits) {
            if(tick == hit){
                duration += Math.max(curr.onHit().applyAsInt(target) - duration + tick,0);
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("执行判定，当前tick：%d", tick)), false);
            }
        }
        this.tick++;
        if(curr.canNext().apply(tick,hits[hits.length-1],duration)){
            curr.onNext().accept(step);
            Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("下一步，当前tick：%d", tick)), false);
            step++;
            tick = 0;
            totalCooldown += curr.cooldown();
        }
    }


    @Override
    public void stop() {
        cooldownEndTick = mob.tickCount + totalCooldown;
        resetAnim();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    protected void interrupt(){
        step = steps.size();
    }

    protected abstract List<CastStep> select(LivingEntity target);

    protected void resetAnim(){
        PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),(byte) -1,1f));
    }
}