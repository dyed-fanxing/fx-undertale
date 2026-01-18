//package com.sakpeipei.undertale.entity.ai.goal;
//
//import com.sakpeipei.undertale.common.anim.SequenceAnim;
//import com.sakpeipei.undertale.common.anim.SingleAnim;
//import com.sakpeipei.undertale.entity.IAnimatable;
//import com.sakpeipei.undertale.network.AnimPacket;
//import net.minecraft.network.chat.Component;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.Mob;
//import net.minecraft.world.entity.ai.goal.Goal;
//import net.neoforged.fml.loading.FMLEnvironment;
//import net.neoforged.neoforge.network.PacketDistributor;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//
//
///**
// * @author Sakqiongzi
// * @since 2025-11-23 21:21
// * 带有服务端触发客户端AnimType接口的任意动画类型的GOAL执行器
// */
//public abstract class SequenceAnimTGoal<T,R extends Mob & IAnimatable> extends Goal {
//    private static final Logger log = LogManager.getLogger(SequenceAnimTGoal.class);
//    protected final R mob;
//    protected int tick;             // 动画tick
//    protected int cooldownEndTick;  // 动画结束冷却Tick点
//
//    protected int step;             // 当前步骤索引
//    protected int stepCooldown;     // 单个动画步骤的冷却时间
//    protected SequenceAnim<T> anim;
//    protected int hitTick;         // 判定点
//
//    public SequenceAnimTGoal(R mob) {
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
//        stepCooldown = 0;
//        LivingEntity target = mob.getTarget();
//        if (target != null) {
//            anim = select(target);
//            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),anim.getSteps().getFirst().getId(),1.0f));
//            log.debug("选择动画，动画步骤：{}，判定Tick：{}",anim.getSteps(), anim.getCd());
//        }
//    }
//
//    @Override
//    public boolean canContinueToUse() {
//        return step < anim.getSteps().size();
//    }
//    // 0 1 2 3 4 5 6 7 8 9 10 11
//    // s         e s
//    @Override
//    public void tick() {
//        List<SingleAnim<T>> steps = anim.getSteps();
//        if(stepCooldown > 0) {
//            stepCooldown--;
//            if(stepCooldown == 0){
//                PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),steps.get(step).getId(),1.0f));
//            }
//            return;
//        }
//
//        SingleAnim<T> curr = steps.get(step);
//        if(curr.shouldHitAt(tick)) {
//            LivingEntity target = mob.getTarget();
//            // 只在开发环境（IDE运行）中显示消息
//            if (!FMLEnvironment.production) {
//                log.debug("发生判定，动画ID：{}，判定Tick：{}",curr.getId(),curr.getHitTicks());
//                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("发生判定：动画ID：%d，判定Ticks：%s",curr.getId(), Arrays.toString(curr.getHitTicks()))),false);
//            }
//            if (target != null) {
//                // 执行攻击时返回的额外动画时间 - 判定生效时剩余的动画时间，如果大于0，则代表这次攻击动画的时间比预设的多，需要增加动画持续时间
//                int remaining = execute(target, curr) - (curr.getLength() - tick);
//                if(remaining > 0){
//                    if (!FMLEnvironment.production) {
//                        Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("动画长度应用偏移前,动画ID：%d，长度：%d",curr.getId(),curr.getLength())),false);
//                        log.debug("动画长度应用偏移前,动画ID：{}，长度：{}",curr.getId(),curr.getLength());
//                    }
//                    anim.offsetLength(step, remaining);
//                    if (!FMLEnvironment.production) {
//                        Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("动画长度应用偏移后,动画ID：%d，长度：%d",curr.getId(),curr.getLength())),false);
//                        log.debug("动画长度应用偏移后,动画ID：{}，长度：{}",curr.getId(),curr.getLength());
//                    }
//                }
//            }
//        }
//
//        tick++;
//        if(tick == curr.getLength()){
//            PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(), (byte) -1,0.0f));
//            stepStop(curr);
//            if (!FMLEnvironment.production) {
//                Objects.requireNonNull(mob.level().getServer()).getPlayerList().broadcastSystemMessage(Component.literal(String.format("步骤动画结束：步骤索引：%d,动画ID：%d，冷却时间：%d",step,curr.getId(),stepCooldown)),false);
//            }
//            step++;
//        }
//    }
//
//
//    /**
//     * 动画执行完成
//     */
//    @Override
//    public void stop() {
//        cooldownEndTick = anim.getCd() + mob.tickCount;
//        PacketDistributor.sendToPlayersTrackingEntity(mob,new AnimPacket(mob.getId(),(byte) -1,0f));
//    }
//
//    @Override
//    public boolean requiresUpdateEveryTick() {
//        return true;
//    }
//
//    @NotNull
//    protected abstract SequenceAnim<T> select(LivingEntity target);
//
//    protected abstract int execute(LivingEntity target, SingleAnim<T> anim);
//
//    protected void stepStop(SingleAnim<T> curr){
//        stepCooldown = curr.getCd();
//    }
//}
