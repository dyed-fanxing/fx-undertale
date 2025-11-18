package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.mod.undertale.utils.RenderUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class GasterBlasterBeamRenderer{
    // 侧面材质：四周面
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/beam/side.png");
    // 正面材质：前后面
    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/beam/front.png");

    private static final RenderType BEAM_SIDE_TYPE = RenderType.entityCutoutNoCull(SIDE);
    private static final RenderType BEAM_FRONT_TYPE = RenderType.entityCutoutNoCull(FRONT);
    // 浅灰白色（220,220,220,255）
    static int r=220, g=220, b=220,a = 255;
    /**
     * 渲染固定 GB
     * @param partialTicks 部分刻时间（用于平滑动画）
     */
    public static void render(GasterBlasterFixed entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight){
        float width = entity.getWidth();
        float halfWidth = width * 0.5f;
        translate(entity,entity.getWidth(),poseStack);
        float partialWidth;
        if(entity.tickCount < GasterBlasterFixed.CHARGE_TICK){
            partialWidth = Mth.lerp((entity.tickCount + partialTicks) / GasterBlasterFixed.CHARGE_TICK, 0, halfWidth);
            RenderUtils.renderCube(buffer.getBuffer(BEAM_FRONT_TYPE), partialWidth, poseStack.last(), r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
        }else{
            if(entity.tickCount == 46 ) partialWidth = Mth.lerp(partialTicks,width,0);
            else {
                if(entity.tickCount == 18) partialWidth = Mth.lerp(partialTicks,halfWidth,width);
                else partialWidth = entity.getWidth() + (float) Math.sin((entity.tickCount + partialTicks) * 0.5f) * 0.1f  ;
            }
            render(entity.getLength(),partialWidth,poseStack.last(),buffer,packedLight);
        }
        poseStack.popPose();
    }

    /**
     * 渲染Pro GB
     * @param partialTicks 部分刻时间（用于平滑动画）
     */
    public static void render(GasterBlasterPro entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        translate(entity,entity.getWidth(),poseStack);
        float width = entity.getWidth();
        float halfWidth = width * 0.5f;
        float partialWidth = 0;
        byte phase = entity.getPhase();
        switch (phase){
            case GasterBlasterPro.PHASE_CHARGE -> {
                partialWidth = Mth.lerp((entity.timer + partialTicks) / GasterBlasterPro.MAX_CHARGE, 0, halfWidth );
                RenderUtils.renderCube(buffer.getBuffer(BEAM_FRONT_TYPE), partialWidth, poseStack.last(), r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
                LogUtils.getLogger().info("宽度{}",partialWidth);
                poseStack.popPose();
                return;
            }
            case GasterBlasterPro.PHASE_GROW -> partialWidth = Mth.lerp(partialTicks,halfWidth,width);
            case GasterBlasterPro.PHASE_SHOT -> partialWidth = width + (float) Math.sin((entity.tickCount + partialTicks) * 0.6f) * 0.1f;
            case GasterBlasterPro.PHASE_DECAY -> partialWidth = Mth.lerp((entity.timer + partialTicks) / 2, width, 0);
        }
        LogUtils.getLogger().info("宽度{}",partialWidth);
        render(entity.getLength(),partialWidth,poseStack.last(),buffer,packedLight);
        poseStack.popPose();
    }

    /**
     * 统一变换
     * @param entity 要渲染的光束实体
     * @param poseStack 位姿栈（存储渲染变换矩阵）
     */
    public static void translate(Entity entity,float width,PoseStack poseStack){
        // 当前栈压入的是entity实体的变换矩阵，是实体局部坐标向量
        poseStack.pushPose(); // 保存当前变换状态
        // 对上一步压入的实体矩阵做变换，也就是相对实体的y轴移动，调整发射点
        poseStack.translate(0,0.3 * width,0);
        // 旋转矩阵
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
    }
    public static void render(float length,float partialWidth,PoseStack.Pose pose, MultiBufferSource buffer, int packedLight){
        // 光束局部起点
        Vec3 start = Vec3.ZERO;
        // 沿着局部Z轴延申光束长度
        Vec3 end = new Vec3(0, 0, 1).scale(length);
        // 渲染四周
        VertexConsumer sideVertexBuilder = buffer.getBuffer(BEAM_SIDE_TYPE);
        RenderUtils.buildSide(start,end, partialWidth,partialWidth,pose, sideVertexBuilder,r,g,b,a,length,OverlayTexture.NO_OVERLAY, packedLight);

//        // 画前后端面
        VertexConsumer frontVertexBuilder = buffer.getBuffer(BEAM_FRONT_TYPE);
        RenderUtils.drawXYQuad(start,partialWidth,partialWidth,pose, frontVertexBuilder, r,g,b,a,1,OverlayTexture.NO_OVERLAY,packedLight,0,0,-1);
        RenderUtils.drawXYQuad(end,partialWidth,partialWidth,pose, frontVertexBuilder, r,g,b,a,1,OverlayTexture.NO_OVERLAY,packedLight,0,0,1);
    }


}
