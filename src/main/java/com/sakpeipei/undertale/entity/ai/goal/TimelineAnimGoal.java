package com.sakpeipei.undertale.entity.ai.goal;

import com.sakpeipei.undertale.common.anim.TimelineAnim;
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

import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;


/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:21
 * 时间线动画执行器
 */
public abstract class TimelineAnimGoal<R extends Mob & IAnimatable> extends Goal {
    private static final Logger log = LogManager.getLogger(TimelineAnimGoal.class);
    protected final R mob;
    protected int tick;             // 动画tick
    protected int cooldownEndTick;  // 动画结束冷却Tick点

    protected int step;             // 当前步骤索引
    protected TimelineAnim anim;

    public TimelineAnimGoal(R mob) {
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
        }
    }

    @Override
    public boolean canContinueToUse() { return tick < anim.length();}
    @Override
    public void tick() {
        Map<Integer, Byte> anims = anim.anims();
        Map<Integer, ToIntFunction<LivingEntity>> actions = anim.actions();
        Byte animId = anims.get(tick);
        if(animId != null){
            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),animId,1.0f));
        }
        ToIntFunction<LivingEntity> action = actions.get(tick);
        if(action != null){
            LivingEntity target = mob.getTarget();
            // 只在开发环境（IDE运行）中显示消息
            if (!FMLEnvironment.production) {
                log.debug("发生判定，动画ID：{}，判定Tick：{}",animId,tick);
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("发生判定：动画ID：%d，判定Tick：%d",animId, tick)), false);
            }
            if (target != null) {
                action.applyAsInt(target);
            }
        }
        tick++;
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
    protected abstract TimelineAnim select(LivingEntity target);
}
