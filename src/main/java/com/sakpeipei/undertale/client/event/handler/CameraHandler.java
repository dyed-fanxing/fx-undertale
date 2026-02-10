package com.sakpeipei.undertale.client.event.handler;

import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber
@OnlyIn(Dist.CLIENT)
public class CameraHandler {
    private static final Logger log = LoggerFactory.getLogger(CameraHandler.class);

    @SubscribeEvent
    public static void onCameraAngle(ViewportEvent.ComputeCameraAngles event){
        Camera camera = event.getCamera();
        Entity entity = camera.getEntity();
        Direction gravity = entity.getData(AttachmentTypeRegistry.GRAVITY).getGravity();
        if (gravity == Direction.DOWN) {
            return;
        }
        switch (gravity) {
            case UP -> event.setRoll(event.getRoll()+180);
            case NORTH -> {
            }
        }
    }
}
