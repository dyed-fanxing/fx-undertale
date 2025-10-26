package com.sakpeipei.mod.undertale.event.handler;

import com.mojang.logging.LogUtils;
import com.sakpeipei.mod.undertale.Undertale;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * @author Sakqiongzi
 * @since 2025-09-08 23:05
 */
@EventBusSubscriber(modid = Undertale.MODID)
public class LivingEntityHandler {
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();

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
}
