package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.entity.attachment.Gravity;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

@EventBusSubscriber
public class EntityHandler {
    @SubscribeEvent
    public static void onEntityRefreshDimension(EntityEvent.Size event){
        EntityDimensions newSize = event.getNewSize();
        Entity entity = event.getEntity();
        Gravity data = entity.getData(AttachmentTypes.GRAVITY);
        if(data.getGravity() != Direction.DOWN){
        }
    }

}
