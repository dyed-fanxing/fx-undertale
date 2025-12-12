package com.sakpeipei.mod.undertale.event.handler;

import com.mojang.logging.LogUtils;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.entity.summon.GroundBone;
import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Undertale.MODID)
public class EventHandlers {
    /**
     * 注册需要属性的实体，即继承自LivingEntity
     */
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(EntityTypeRegistry.GASTER_BLASTER_PRO.get(), GasterBlasterPro.createAttributes().build());
        event.put(EntityTypeRegistry.SANS.get(), Sans.createAttributes().build());
    }


    @SubscribeEvent
    public static void onPlayer(PlayerEvent.PlayerLoggedInEvent event) {
//        event.getEntity().getPersistentData().remove(GasterBlasterProItem.GASTER_BLASTER_PRO_KEY);
//        LOGGER.info("玩家数据移除Pro后{}",event.getEntity().getPersistentData());
        LogUtils.getLogger().info("玩家数据{}",event.getEntity().getPersistentData());

    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
//        if(entity instanceof FlyingBone){
//            entity.discard();
//            event.setCanceled(true);
//        }
    }


}
