package com.sakpeipei.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.common.RenderTypes;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.undertale.utils.RenderUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class GasterBlasterBeamRenderer {
    // 侧面材质：四周面
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/beam/side.png");
    // 正面材质：前后面
    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/beam/front.png");

    private static final RenderType BEAM_SIDE_TYPE = RenderType.entityTranslucentEmissive(SIDE);
    private static final RenderType BEAM_FRONT_TYPE = RenderTypes.GB_BEAM.apply(FRONT);

    // 浅灰白色（220,220,220,255）
    static int r=220, g=220, b=220,a = 255;

    // 圆柱和球体的分段数
    private static final byte MIN_SEGMENTS = 32;
    private static final byte MAX_SEGMENTS = 127;

    /**
     * 渲染 GB
     * @param partialTicks 部分刻时间（用于平滑动画）
     */
    public static  void render(GasterBlaster entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight){
        poseStack.pushPose(); // 在这里压栈
        float size = entity.getSize();
        float halfSize = size * 0.5f;
        byte segments = (byte) Mth.clamp(32 + (Math.round(size) - 1) * 2, 16, 127);
        float partialSize = 0.0f;
        short fireTick = entity.getFireTick();
        poseStack.translate(0,entity.getMonthHeight(),0);
        if(entity.tickCount < fireTick){
            partialSize = Mth.lerp((entity.tickCount + partialTicks) / fireTick, 0, halfSize);
            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialSize, segments, r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
        }else if(!Vec3.ZERO.equals(entity.getEnd())){
            Vec3 dir = entity.getEnd().subtract(entity.getStart());
            poseStack.mulPose(Axis.YP.rotationDegrees(RotUtils.yRotD(dir) + 90f));
            poseStack.mulPose(Axis.XP.rotationDegrees(RotUtils.xRotD(dir)));
            if(entity.tickCount < fireTick + 1) {
                partialSize = Mth.lerp(partialTicks, partialSize, halfSize);
            } else if(entity.tickCount < entity.getDecayTick()) {
                partialSize =  halfSize + (float) Math.sin((entity.tickCount + partialTicks) * 0.5f) * 0.05f;
            } else {
                partialSize = Mth.lerp( (entity.tickCount + partialTicks )/ 2,partialSize,0);
            }
            Undertale.LOGGER.debug("渲染GB光束的end:{},dir:{},长度:{},大小:{}",entity.getEnd(),dir,dir.length(),partialSize);
            RenderUtils.renderCapsule(poseStack,buffer.getBuffer(BEAM_FRONT_TYPE),(float) dir.length(),partialSize,segments, r, g, b, a,OverlayTexture.NO_OVERLAY,packedLight);
        }
        poseStack.popPose();
    }

    /**
     * 渲染Pro GB
     * @param partialTicks 部分刻时间（用于平滑动画）
     */
    public static void render(GasterBlasterPro entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
//        poseStack.pushPose(); // 在这里压栈
//        translate(entity,entity.getSize(),poseStack);
//        float size = entity.getSize();
//        float halfSize = size * 0.5f;
//        float partialSize = 0;
//        byte phase = entity.getPhase();
//        switch (phase){
//            case GasterBlasterPro.PHASE_CHARGE -> {
//                partialSize = Mth.lerp((entity.timer + partialTicks) / GasterBlasterPro.MAX_CHARGE, 0, halfSize );
//                // 替换成立方体为球体
//                RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialSize, SEGMENTS,
//                        r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
//                LogUtils.getLogger().info("宽度{}",partialSize);
//                poseStack.popPose();
//                return;
//            }
//            case GasterBlasterPro.PHASE_ANTICIPATION -> partialSize = Mth.lerp(partialTicks,halfSize,size);
//            case GasterBlasterPro.PHASE_FIRE -> partialSize = size + (float) Math.sin((entity.tickCount + partialTicks) * 0.6f) * 0.1f;
//            case GasterBlasterPro.PHASE_DECAY -> partialSize = Mth.lerp((entity.timer + partialTicks) / 2, size, 0);
//        }
//        LogUtils.getLogger().info("宽度{}",partialSize);
//        render(entity.getLength(),partialSize,poseStack,buffer,packedLight);
//        poseStack.popPose();
    }

}