package com.sakpeipei.mod.undertale.event;

import com.mojang.logging.LogUtils;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.effect.KarmaManager;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * @author Sakqiongzi
 * @since 2025-09-08 23:05
 */
@EventBusSubscriber(modid = Undertale.MODID)
public class LivingEntityEventHandler {
    @SubscribeEvent
    public static void onLivingEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        // 死亡时重置所有 Karma 数据
        KarmaManager.removeKarma(entity);
    }
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
            if (entity instanceof TraceableEntity traceableEntity && traceableEntity.getOwner() instanceof Sans) {
                //移除 攻击类型判断重复
            }
            LogUtils.getLogger().info("{}被销毁了", entity);
        }

    }
    //    /**
//     * 存活实体受伤事件之前
//     * @param event
//     */
//    @SubscribeEvent(priority = EventPriority.HIGH)
//    public static void onLivingDamagePre(LivingDamageEvent.Pre event){
//        DamageContainer container = event.getContainer();
//        DamageSource source = event.getSource();
//        if (source.is(Tags.GASTER_BLASTER_BYPASS)) {
//            container.setPostAttackInvulnerabilityTicks(0);
//        }
//    }
    /**
     * 存活实体进入伤害事件
     */
    @SubscribeEvent
    public static void onEntityIncomingDamage(LivingIncomingDamageEvent event){
        DamageContainer container = event.getContainer();
        DamageSource source = event.getSource();
        if(source.getEntity() instanceof Sans){
            container.setPostAttackInvulnerabilityTicks(0);
        }
    }



    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LogUtils.getLogger().info("{},{},{},{},{}",
                event.getEntity(),
                event.getEntity().getHealth(),
                event.getOriginalDamage(),
                event.getSource(),
                event.getNewDamage()
        );
    }
}
