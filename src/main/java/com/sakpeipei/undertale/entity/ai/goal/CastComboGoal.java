package com.sakpeipei.undertale.entity.ai.goal;

import com.sakpeipei.undertale.entity.ai.anim.CastStep;
import com.sakpeipei.undertale.entity.IAnimatable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;


public abstract class CastComboGoal<T extends Mob & IAnimatable> extends Goal {
    private static final Logger log = LoggerFactory.getLogger(CastComboGoal.class);
    protected final T mob;
    protected List<CastStep> steps;
    protected int step;
    protected int tick;
    protected int cooldownEndTick;
    protected LivingEntity target;
    protected int duration;


    public CastComboGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && mob.canAttack(target) && mob.tickCount >= cooldownEndTick;
    }

    @Override
    public boolean canContinueToUse() {
        return step < steps.size();
    }

    @Override
    public void start() {
        target = mob.getTarget();
        step = 0;
        tick = 0;
        cooldownEndTick = 0;
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
        if (target == null || step >= steps.size()) {
            return;
        }
        CastStep curr = steps.get(step);

        if(tick == 0){
            if(curr.id() != null){
                mob.setAnimID(curr.id());
                // 只在开发环境（IDE运行）中显示消息
                if (!FMLEnvironment.production) {
                    log.debug("触发动画ID：{}，判定点：{}，默认动画时长：{}，冷却时间：{}", curr.id(),curr.hitTicks(), curr.duration(),curr.cooldown());
                    Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("触发动画ID：%d，判定点：%s，默认动画时长：%d，冷却时间：%d", curr.id(), Arrays.toString(curr.hitTicks()), curr.duration(),curr.cooldown())), false);
                }
            }
            // 只在开发环境（IDE运行）中显示消息
            if (!FMLEnvironment.production) {
                log.debug("无动画，判定点：{}，默认动画时长：{}，冷却时间：{}",curr.hitTicks(), curr.duration(),curr.cooldown());
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("无动画，判定点：%s，默认动画时长：%d，冷却时间：%d", Arrays.toString(curr.hitTicks()), curr.duration(),curr.cooldown())), false);
            }
            duration = curr.duration();
        }

        int[] hits = curr.hitTicks();
        for (int hit : hits) {
            if(tick == hit){
                // 施法返回的额外冷却
                duration += Math.max(curr.onHit().applyAsInt(target) - duration + tick,0);
            }
        }
        this.tick++;

        if(curr.canNext().apply(tick,duration)){
            curr.onNext().accept(step);
            step++;
            tick = 0;
            cooldownEndTick += curr.cooldown();
        }
    }


    @Override
    public void stop() {
        cooldownEndTick += mob.tickCount;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    protected void interrupt(){
        step = steps.size();
    }

    protected abstract List<CastStep> select(LivingEntity target);
}