package com.sakpeipei.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.common.Config;
import com.sakpeipei.undertale.common.RenderTypes;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.undertale.utils.RenderUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class GasterBlasterBeamRenderer {
    // 侧面材质：四周面
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/beam/side.png");
    // 正面材质：前后面
    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/beam/front.png");

    private static final RenderType BEAM_FRONT_TYPE = RenderTypes.GB_BEAM.apply(FRONT);

    // 浅灰白色（220,220,220,255）
    static int r=220, g=220, b=220,a = 255;

    /**
     * 渲染 GB
     * @param partialTicks 部分刻时间（用于平滑动画）
     */
    public static  void render(GasterBlaster entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight){
        poseStack.pushPose(); // 在这里压栈
        float size = entity.getSize();
        float halfSize = size * 0.5f;
        byte segments = Config.segments(size);
        float partialSize = 0.0f;
        int fireTick = entity.getFireTick();
        int shotTick = entity.getShotTick();
        int decayTick = entity.getDecayTick();
        int discardTick = decayTick + 3;
        poseStack.translate(0,entity.getMonthHeight(),0);
        if(entity.tickCount < fireTick){
            partialSize = Mth.lerp((partialTicks + entity.tickCount) / fireTick, 0, halfSize*0.75f);
            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialSize, segments, r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
        }else{
            Vec3 dir = entity.getEnd().subtract(entity.getStart());
            poseStack.mulPose(Axis.YP.rotationDegrees(RotUtils.yRotD(dir) + 90f));
            poseStack.mulPose(Axis.XP.rotationDegrees(RotUtils.xRotD(dir) + 90f)); // 要渲染的胶囊体默认是竖向的Y轴的，需要旋转到Z轴
            if(entity.tickCount < shotTick) {
                partialSize = Mth.lerp((partialTicks + entity.tickCount)/shotTick, 0, halfSize);
            } else if(entity.tickCount < decayTick) {
                partialSize =  halfSize + (float) Math.sin((entity.tickCount + partialTicks) * 0.5f) * 0.05f;
            } else if(entity.tickCount < discardTick) {
                partialSize = Mth.lerp( (entity.tickCount + partialTicks )/ discardTick,halfSize,0);
                LogUtils.getLogger().debug("光束半径大小：{},tickCount：{},discardTick：{}", partialSize,entity.tickCount,discardTick);
            }
            RenderUtils.renderCapsule(poseStack,buffer.getBuffer(BEAM_FRONT_TYPE),partialSize,(float) dir.length(),segments, r, g, b, a,OverlayTexture.NO_OVERLAY,packedLight);
        }
        poseStack.popPose();
    }

    /**
     * 渲染Pro GB
     * @param partialTicks 部分刻时间（用于平滑动画）
     */
    public static void render(GasterBlasterPro entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
//        poseStack.pushPose(); // 在这里压栈
//        float size = entity.getSize();
//        float halfSize = size * 0.5f;
//        byte segments = Config.segments(size);
//        float partialSize = 0.0f;
//        int fireTick = entity.getFireTick();
//        poseStack.translate(0,entity.getMonthHeight(),0);
//        if(entity.tickCount < fireTick){
//            partialSize = Mth.lerp((entity.tickCount + partialTicks) / fireTick, 0, halfSize*0.75f);
//            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialSize, segments, r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
//        }else if(!Vec3.ZERO.equals(entity.getEnd())){
//            Vec3 dir = entity.getEnd().subtract(entity.getStart());
//            poseStack.mulPose(Axis.YP.rotationDegrees(RotUtils.yRotD(dir) + 90f));
//            poseStack.mulPose(Axis.XP.rotationDegrees(RotUtils.xRotD(dir) + 90f)); // 要渲染的胶囊体默认是竖向的Y轴的，需要旋转到Z轴
//            if(entity.tickCount < fireTick + 2) {
//                partialSize = Mth.lerp((partialTicks + entity.tickCount - fireTick)/2, partialSize, halfSize);
//            } else if(entity.tickCount < entity.getDecayTick()) {
//                partialSize =  halfSize + (float) Math.sin((entity.tickCount + partialTicks) * 0.5f) * 0.05f;
//            } else {
//                partialSize = Mth.lerp( (entity.tickCount + partialTicks )/ 3,partialSize,0);
//            }
//            RenderUtils.renderCapsule(poseStack,buffer.getBuffer(BEAM_FRONT_TYPE),partialSize,(float) dir.length(),segments, r, g, b, a,OverlayTexture.NO_OVERLAY,packedLight);
//        }
//        poseStack.popPose();
    }

}