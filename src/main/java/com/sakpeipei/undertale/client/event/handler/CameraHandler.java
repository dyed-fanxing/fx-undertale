//package com.sakpeipei.undertale.client.event.handler;
//
//import com.sakpeipei.undertale.entity.attachment.GravityData;
//import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
//import net.minecraft.client.Camera;
//import net.minecraft.core.Direction;
//import net.minecraft.world.entity.Entity;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.ViewportEvent;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//@EventBusSubscriber
//@OnlyIn(Dist.CLIENT)
//public class CameraHandler {
//    private static final Logger log = LoggerFactory.getLogger(CameraHandler.class);
//
//    @SubscribeEvent
//    public static void onCameraAngle(ViewportEvent.ComputeCameraAngles event){
//        Camera camera = event.getCamera();
//        Entity entity = camera.getEntity();
//        log.info("上一次的基准向量：lookVector：{},leftVector：{},upVector：{},相机位置：{}",camera.getLookVector(),camera.getLeftVector(),camera.getUpVector(),camera.getPosition());
//        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
//        if (data.getGravity() == Direction.DOWN) {
//            return;
//        }
//        switch (data.getGravity()) {
//            case UP -> {
//                event.setYaw(-event.getYaw());
//                event.setPitch(-event.getPitch());
//                event.setRoll(event.getRoll()+180);
//            }
//            case NORTH -> {
//                // 贴北墙：pitch-90, roll-90
//                event.setPitch(event.getPitch() - 90);
//                event.setRoll(event.getRoll() - 90);
//                log.info("北墙调整: pitch-90, roll-90");
//            }
//        }
//    }
//}
