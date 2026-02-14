package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.boss.Sans;
import com.sakpeipei.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.undertale.registry.EntityTypes;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sakqiongzi
 * @since 2025-09-08 23:05
 */
@EventBusSubscriber(modid = Undertale.MOD_ID)
public class LivingEntityHandler {
    private static final Logger log = LoggerFactory.getLogger(LivingEntityHandler.class);

    /**
     * 注册需要属性的实体，即继承自LivingEntity
     */
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(EntityTypes.GASTER_BLASTER_PRO.get(), GasterBlasterPro.createAttributes().build());
        event.put(EntityTypes.SANS.get(), Sans.createAttributes().build());
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
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();

    }


}
