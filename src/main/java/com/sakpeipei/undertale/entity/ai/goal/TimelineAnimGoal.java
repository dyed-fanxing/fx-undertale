package com.sakpeipei.undertale.entity.ai.goal;

import com.sakpeipei.undertale.common.anim.TimelineAnim;
import com.sakpeipei.undertale.entity.IAnimatable;
import com.sakpeipei.undertale.net.packet.AnimPacket;
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
    protected int length;           // 动画长度

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
        LivingEntity target = mob.getTarget();
        if (target != null) {
            anim = select(target);
            length = anim.length();
        }
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target) && tick < length;
    }
    @Override
    public void tick() {
        Map<Integer, Byte> anims = anim.anims();
        Map<Integer, ToIntFunction<LivingEntity>> actions = anim.actions();
        Byte animId = anims.get(tick);
        if(animId != null){
            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),animId,1.0f));
            log.debug("触发动画：{}",animId);
            if (!FMLEnvironment.production) {
                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("触发动画：%d", animId)), false);
            }
        }
        ToIntFunction<LivingEntity> action = actions.get(tick);
        if(action != null){
            LivingEntity target = mob.getTarget();
            // 只在开发环境（IDE运行）中显示消息
            log.debug("发生判定，判定Tick：{}",tick);
            if (target != null) {
                // 执行攻击时返回的额外动画时间 - 判定生效时剩余的动画时间，如果大于0，则代表这次攻击动画的时间比预设的多，需要增加动画长度
                length += Math.max(action.applyAsInt(target) - anim.length() + tick,0);
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
