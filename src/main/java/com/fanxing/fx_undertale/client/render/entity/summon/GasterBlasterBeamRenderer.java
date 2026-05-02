package com.fanxing.fx_undertale.client.render.entity.summon;

import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.lib.ConfigFxLib;
import com.fanxing.lib.client.render.shape.CapsuleRenderer;
import com.fanxing.lib.client.render.shape.SphereRenderer;
import com.fanxing.lib.client.render.type.BeamRenderType;
import com.fanxing.lib.client.render.type.LightingRenderType;
import com.fanxing.lib.client.render.type.RayRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GasterBlasterBeamRenderer {
    public static final float INNER_SCALE = 0.6f;
    private static final Logger log = LoggerFactory.getLogger(GasterBlasterBeamRenderer.class);

    /**
     * 渲染 GB
     *
     * @param partialTick 部分刻，客户端插值
     */
    public static void render(GasterBlaster animatable,PoseStack poseStack, MultiBufferSource buffer, float partialTick,  List<Integer> colors) {
        poseStack.pushPose(); // 在这里压栈
        float size = animatable.getSize();
        float radius = size * 0.5f;
        int segments = ConfigFxLib.Client.SEGMENTS.getAsInt();
        float partialSize = 0f;
        int fireTick = animatable.getFireTick();
        int shotTick = animatable.getShotTick();
        int decayTick = animatable.getDecayTick();
        int discardTick = decayTick + 3;
        float animTick = animatable.tickCount + partialTick;
        poseStack.translate(0, animatable.getEyeHeight(), 0);
        if (animatable.tickCount < fireTick) {
            partialSize = Mth.lerp(animTick / fireTick, 0, radius * 0.75f);
            poseStack.pushPose();
            SphereRenderer.render(poseStack.last(), buffer.getBuffer(BeamRenderType.ENERGY_BEAM_WHITE),new Vector3f(), partialSize, segments, colors.getLast(), OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            SphereRenderer.render(poseStack.last(), buffer.getBuffer(BeamRenderType.BEAM_TRANSPARENCY_WHITE),new Vector3f(), partialSize * 0.5f, segments, colors.getFirst(), OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
            animatable.sphereRayEmitter.stretchCircleRender(poseStack, buffer.getBuffer(RayRenderTypes.RAY), animTick, radius * (animatable.isFollow() ? 1.5f : 1f), partialSize, colors.getLast());
//            renderLightRay(poseStack,buffer,animatable.getUUID(),radius,animTick,partialSize,color);
        } else {
            float length = animatable.getLength();
            // UV滚动用世界时间
            float flowTime = (animatable.level().getGameTime() & 0xFFFF) + partialTick;
            float offset = (flowTime * 0.2F) % 1.0f;
            int edgeColor;
            int coreColor;
//            if(colors.isEmpty()){
//                // 连续正弦波彩虹，无分段
//                float hue = flowTime * 10.0F;
//                edgeColor = smoothRainbow(hue, 255);                        // 外层：饱和彩虹
//                coreColor = mixRainbowWhite(hue, 255, 0.65F);              // 内层：彩虹+白混合，向中心渐变
//            }else{
//                edgeColor = colors.getLast();
//                coreColor = colors.getFirst();
//            }
            edgeColor = colors.getLast();
            coreColor = colors.getFirst();
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, -animatable.yRotO, -animatable.getYRot())));
            // 要渲染的胶囊体默认是竖向的Y轴的，需要旋转到Z轴
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, animatable.xRotO + 90f, animatable.getXRot() + 90f)));
            if (animatable.tickCount < shotTick) {
                partialSize = Mth.lerp(animTick / shotTick, 0, radius);
            } else if (animatable.tickCount < decayTick) {
                partialSize = radius + (float) Math.sin(animTick * 0.5f) * 0.05f;
            } else if (animatable.tickCount < discardTick) {
                partialSize = Mth.lerp(animTick / discardTick, radius, 0);
            }
            poseStack.pushPose();
            CapsuleRenderer.render(poseStack.last(), buffer, BeamRenderType.ENERGY_FLOW_BEAM_TRIANGLE_STRIP_WHITE, BeamRenderType.ENERGY_FLOW_BEAM_WHITE, new Vector3f(), partialSize, length, segments, edgeColor, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT,
                    3f, length * 0.4F,  0,  -offset);
            CapsuleRenderer.render(poseStack.last(), buffer, BeamRenderType.BEAM_TRANSPARENCY_TRIANGLE_STRIP_WHITE, BeamRenderType.BEAM_TRANSPARENCY_WHITE, new Vector3f(), partialSize * INNER_SCALE, length, segments, coreColor, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** 正弦波彩虹 — 无离散分段，连续过渡 */
    private static int smoothRainbow(float hue, int alpha) {
        float phase = hue * Mth.DEG_TO_RAD;
        float r = Mth.sin(phase) * 0.5F + 0.5F;
        float g = Mth.sin(phase + Mth.TWO_PI / 3F) * 0.5F + 0.5F;   // +120°
        float b = Mth.sin(phase + Mth.TWO_PI * 2F / 3F) * 0.5F + 0.5F; // +240°
        return FastColor.ARGB32.color(alpha, (int)(r * 255), (int)(g * 255), (int)(b * 255));
    }

    /** 彩虹色与白色混合，白色比例越高越接近白（中心发光感） */
    private static int mixRainbowWhite(float hue, int alpha, float whiteRatio) {
        int rainbow = smoothRainbow(hue, alpha);
        int wr = 255 - (int)((255 - FastColor.ARGB32.red(rainbow)) * (1F - whiteRatio));
        int wg = 255 - (int)((255 - FastColor.ARGB32.green(rainbow)) * (1F - whiteRatio));
        int wb = 255 - (int)((255 - FastColor.ARGB32.blue(rainbow)) * (1F - whiteRatio));
        return FastColor.ARGB32.color(alpha, wr, wg, wb);
    }
}
