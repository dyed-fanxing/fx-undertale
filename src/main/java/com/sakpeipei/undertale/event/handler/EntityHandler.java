package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.entity.projectile.FlyingBone;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber
public class EntityHandler {
    @SubscribeEvent
    public static void onEntityRefreshDimension(EntityEvent.Size event){
        EntityDimensions newSize = event.getNewSize();
        Entity entity = event.getEntity();
        GravityData data = entity.getData(AttachmentTypes.GRAVITY);
        if(data.getGravity() != Direction.DOWN){
//            entity.getEyeHeight()
//            entity.getEyePosition()
        }
    }

}
