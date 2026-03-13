package com.fanxing.fx_undertale.event.handler;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.fx_undertale.registry.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author FanXing
 * @since 2025-09-08 23:05
 */
@EventBusSubscriber(modid = FxUndertale.MOD_ID)
public class LivingEntityHandler {
    private static final Logger log = LoggerFactory.getLogger(LivingEntityHandler.class);

    /**
     * 注册需要属性的实体，即继承自LivingEntity
     */
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(EntityTypes.SANS.get(), Sans.createAttributes().build());
        event.put(EntityTypes.GASTER_BLASTER.get(), GasterBlaster.createAttributes().build());
    }

    /**
     * 存活实体进入伤害事件
     */
    @SubscribeEvent
    public static void onEntityIncomingDamage(LivingIncomingDamageEvent event){
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
//        LogUtils.getLogger().info("{},当前生命值{},当前吸收值{},原始伤害{},伤害来源{},结算伤害{}",
//                event.getEntity(),
//                event.getEntity().getHealth(),
//                event.getEntity().getAbsorptionAmount(),
//                event.getOriginalDamage(),
//                event.getSource(),
//                event.getNewDamage()
//        );
    }

    @SubscribeEvent
    public static void onDamagePost(LivingShieldBlockEvent event) {
        LivingEntity entity = event.getEntity();
        if(entity instanceof Player){
            log.info("盾牌事件：{}，阻挡伤害：{}，伤害来源：{}，阻挡时的new伤害：{}",event.getBlocked(),event.getBlockedDamage(),event.getDamageSource(),event.getDamageContainer().getNewDamage());
        }
    }


}
