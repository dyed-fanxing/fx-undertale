package com.sakpeipei.undertale.client.event.handler;

import com.sakpeipei.undertale.entity.IRollable;
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
            case EAST -> event.setRoll(event.getRoll() - 90); // 站在东墙上：向左倾斜90度
            case WEST -> {
                // 站在西墙上：向右倾斜90度
                event.setRoll(event.getRoll() + 90);
            }
            case SOUTH -> {
//                event.setYaw(event.getRoll());      // 世界yaw = 局部roll
//                event.setRoll(event.getYaw());      // 世界roll = 局部yaw
                IRollable rollable = (IRollable)entity;
                log.debug("roll：{}",rollable.undertale$getViewRoll((float) event.getPartialTick()));
                event.setRoll(rollable.undertale$getViewRoll((float) event.getPartialTick()));      // 世界roll = 局部yaw
            }
            case NORTH -> {
                // 站在北墙上：向后倾斜90度
                // 相机需要向后旋转90度
//                event.setPitch(-event.getPitch());   // 反转俯仰
                // yaw可能保持不变
            }
        }
    }
}
