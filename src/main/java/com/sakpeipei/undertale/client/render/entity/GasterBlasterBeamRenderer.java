package com.sakpeipei.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.undertale.utils.RenderUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class GasterBlasterBeamRenderer {
    // 侧面材质：四周面
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/beam/side.png");
    // 正面材质：前后面
    private static final ResourceLocation FRONT = ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "textures/entity/beam/front.png");

    private static final RenderType BEAM_SIDE_TYPE = RenderType.entityTranslucentEmissive(SIDE);
    private static final RenderType BEAM_FRONT_TYPE = RenderType.create(
            "gb_beam",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(FRONT, false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );
    // 浅灰白色（220,220,220,255）
    static int r=220, g=220, b=220,a = 255;

    // 圆柱和球体的分段数
    private static final int SEGMENTS = 32;

    /**
     * 渲染固定 GB
     * @param partialTicks 部分刻时间（用于平滑动画）
     */
    public static void render(GasterBlaster entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight){
        poseStack.pushPose(); // 在这里压栈
        float width = entity.getWidth();
        float halfWidth = width * 0.5f;
        translate(entity,entity.getWidth(),poseStack);
        float partialWidth;
        byte charge = entity.getCharge();
        if(entity.tickCount < charge){
            partialWidth = Mth.lerp((entity.tickCount + partialTicks) / charge, 0, halfWidth);
            // 替换成立方体为球体
            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialWidth, SEGMENTS,
                     r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
        }else{
            if(entity.tickCount == entity.getDiscard() ) partialWidth = Mth.lerp(partialTicks,width,0);
            else {
                if(entity.tickCount == charge) partialWidth = Mth.lerp(partialTicks,halfWidth,width);
                else partialWidth = entity.getWidth() + (float) Math.sin((entity.tickCount + partialTicks) * 0.5f) * 0.1f  ;
            }
            render(entity.getLength(),partialWidth * 0.5f,poseStack,buffer,packedLight);
        }
        poseStack.popPose();
    }

    /**
     * 渲染Pro GB
     * @param partialTicks 部分刻时间（用于平滑动画）
     */
    public static void render(GasterBlasterPro entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose(); // 在这里压栈
        translate(entity,entity.getWidth(),poseStack);
        float width = entity.getWidth();
        float halfWidth = width * 0.5f;
        float partialWidth = 0;
        byte phase = entity.getPhase();
        switch (phase){
            case GasterBlasterPro.PHASE_CHARGE -> {
                partialWidth = Mth.lerp((entity.timer + partialTicks) / GasterBlasterPro.MAX_CHARGE, 0, halfWidth );
                // 替换成立方体为球体
                RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialWidth, SEGMENTS,
                        r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
                LogUtils.getLogger().info("宽度{}",partialWidth);
                poseStack.popPose();
                return;
            }
            case GasterBlasterPro.PHASE_ANTICIPATION -> partialWidth = Mth.lerp(partialTicks,halfWidth,width);
            case GasterBlasterPro.PHASE_FIRE -> partialWidth = width + (float) Math.sin((entity.tickCount + partialTicks) * 0.6f) * 0.1f;
            case GasterBlasterPro.PHASE_DECAY -> partialWidth = Mth.lerp((entity.timer + partialTicks) / 2, width, 0);
        }
        LogUtils.getLogger().info("宽度{}",partialWidth);
        render(entity.getLength(),partialWidth,poseStack,buffer,packedLight);
        poseStack.popPose();
    }

    /**
     * 统一变换
     * @param entity 要渲染光束的实体
     * @param poseStack 位姿栈（存储渲染变换矩阵）
     */
    public static void translate(Entity entity,float width,PoseStack poseStack){
        // 当前栈压入的是entity实体的变换矩阵，是实体局部坐标向量
        poseStack.translate(0,0.4 * width,0);
        // 旋转矩阵
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
    }

    public static void render(float length,float halfPartialWidth,PoseStack poseStack, MultiBufferSource buffer, int packedLight){
        poseStack.pushPose(); // 在这里压栈
        length = length + halfPartialWidth;
        poseStack.translate(0,0,-halfPartialWidth);
        VertexConsumer sideVertexBuilder = buffer.getBuffer(BEAM_FRONT_TYPE);
        RenderUtils.renderCylinder(poseStack.last(), sideVertexBuilder, halfPartialWidth, length, SEGMENTS,
                r,g,b,a,OverlayTexture.NO_OVERLAY, packedLight);
        poseStack.popPose(); // 在这里弹栈
    }


}