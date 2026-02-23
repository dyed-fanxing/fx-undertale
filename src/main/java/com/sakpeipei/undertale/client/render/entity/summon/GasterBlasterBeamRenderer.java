package com.sakpeipei.undertale.client.render.entity.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.common.Config;
import com.sakpeipei.undertale.common.RenderTypes;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.undertale.utils.RenderUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GasterBlasterBeamRenderer {
    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "textures/entity/beam/front.png");
    private static final RenderType BEAM_FRONT_TYPE = RenderTypes.ENTITY_BEAM.apply(FRONT);
    // 浅灰白色（220,220,220,255）
    static int r=220, g=220, b=220,a = 255;

    /**
     * 渲染 GB
     * @param partialTick 部分刻，客户端插值
     */
    public static  void render(GasterBlaster animatable, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight){
        poseStack.pushPose(); // 在这里压栈
        float size = animatable.getSize();
        float radius = size*0.5f;
        byte segments = Config.segments(size);
        float partialSize = 0.0f;
        int fireTick = animatable.getFireTick();
        int shotTick = animatable.getShotTick();
        int decayTick = animatable.getDecayTick();
        int discardTick = decayTick + 3;
        poseStack.translate(0,animatable.getEyeHeight(),0);
        if(animatable.timer < fireTick){
            partialSize = Mth.lerp((partialTick + animatable.timer) / fireTick, 0, radius*0.75f);
            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialSize, segments, r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
        }else{
            float length = animatable.getLength();
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick,-animatable.yRotO,-animatable.getYRot())));
            // 要渲染的胶囊体默认是竖向的Y轴的，需要旋转到Z轴
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick,animatable.xRotO+90f,animatable.getXRot() + 90f)));
            if(animatable.timer < shotTick) {
                partialSize = Mth.lerp((partialTick + animatable.timer)/shotTick, 0, radius);
            } else if(animatable.timer < decayTick) {
                partialSize =  radius + (float) Math.sin((animatable.timer + partialTick) * 0.5f) * 0.05f;
            } else if(animatable.timer < discardTick) {
                partialSize = Mth.lerp( (animatable.timer + partialTick )/ discardTick,radius,0);
            }
            RenderUtils.renderCapsule(poseStack,buffer.getBuffer(BEAM_FRONT_TYPE),partialSize,length,segments, r, g, b, a,OverlayTexture.NO_OVERLAY,packedLight);
        }
        poseStack.popPose();
    }

    /**
     * 渲染Pro GB
     * @param partialTick 部分刻时间（用于平滑动画）
     */
    public static void render(GasterBlasterPro animatable, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
//        poseStack.pushPose(); // 在这里压栈
//        float size = animatable.getSize();
//        float radius = size * 0.5f;
//        byte segments = Config.segments(size);
//        float partialSize = 0.0f;
//        int fireTick = animatable.getFireTick();
//        poseStack.translate(0,animatable.getMonthHeight(),0);
//        if(animatable.timer < fireTick){
//            partialSize = Mth.lerp((animatable.timer + partialTick) / fireTick, 0, radius*0.75f);
//            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialSize, segments, r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
//        }else if(!Vec3.ZERO.equals(animatable.getEnd())){
//            Vec3 dir = animatable.getEnd().subtract(animatable.getStart());
//            poseStack.mulPose(Axis.YP.rotationDegrees(RotUtils.yRotD(dir) + 90f));
//            poseStack.mulPose(Axis.XP.rotationDegrees(RotUtils.xRotD(dir) + 90f)); // 要渲染的胶囊体默认是竖向的Y轴的，需要旋转到Z轴
//            if(animatable.timer < fireTick + 2) {
//                partialSize = Mth.lerp((partialTick + animatable.timer - fireTick)/2, partialSize, radius);
//            } else if(animatable.timer < animatable.getDecayTick()) {
//                partialSize =  radius + (float) Math.sin((animatable.timer + partialTick) * 0.5f) * 0.05f;
//            } else {
//                partialSize = Mth.lerp( (animatable.timer + partialTick )/ 3,partialSize,0);
//            }
//            RenderUtils.renderCapsule(poseStack,buffer.getBuffer(BEAM_FRONT_TYPE),partialSize,(float) dir.length(),segments, r, g, b, a,OverlayTexture.NO_OVERLAY,packedLight);
//        }
//        poseStack.popPose();
    }

}