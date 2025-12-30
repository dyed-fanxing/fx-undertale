package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.Undertale;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

/**
 * @author Sakqiongzi
 * @since 2025-10-21 22:45
 */
@EventBusSubscriber(modid = Undertale.MODID)
public class EntitySizeHandler {
    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
//        if(event.getEntity() instanceof GroundBoneProjectile bone){
//            EntityDimensions oldSize = event.getOldSize();
//            float height = oldSize.height();
//            event.setNewSize(event.getOldSize().scale(1f,( height + bone.getHeight() ) / height));
//        }
    }
}
