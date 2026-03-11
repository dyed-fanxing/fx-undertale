package com.sakpeipei.undertale.client.entity.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Config;
import com.sakpeipei.undertale.common.RenderTypes;
import com.sakpeipei.undertale.common.ResourceLocations;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.utils.RenderUtils;
import com.sakpeipei.undertale.utils.RotUtils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

public class GasterBlasterBeamRenderer {
    private static final RenderType BEAM_NO_TRANSPARENCY = RenderTypes.ENTITY_BEAM_NO_CULL.apply(ResourceLocations.WHITE_TEXTURE, false);
    private static final RenderType BEAM_NO_TRANSPARENCY_TRIANGLE_STRIP = RenderTypes.ENTITY_BEAM_NO_CULL_TRIANGLE_STRIP.apply(ResourceLocations.WHITE_TEXTURE, false);
    private static final RenderType BEAM_ENERGY_SWIRL = RenderType.energySwirl(ResourceLocations.BEAM_FLOW_TEXTURE, 0, 0);

    private static final RenderType RAY = RenderTypes.ENERGY_TRIANGLES.apply(ResourceLocations.WHITE_TEXTURE, false);

    // 红色（毁灭风格）
    public static final int[][] RED = {
            {255, 80, 80, 255},     // 内层：亮红
            {150, 0, 0, 255}        // 外层：暗红半透明
    };
    // 紫色（毁灭风格）
    public static final int[][] PURPLE = {
            {200, 0, 255, 255},     // 内层：亮紫
            {100, 0, 150, 255}      // 外层：深紫半透明
    };
    // 蓝色（激光风格）
    public static final int[][] SANS_BLUE = {
            {255, 255, 255, 255},   // 内层：纯白（最亮）
            {100, 150, 220, 255}
    };
    // 橙色（熔岩风格）
    public static final int[][] ORANGE = {
            {255, 160, 0, 255},     // 内层：亮橙
            {200, 80, 0, 100}       // 外层：暗橙半透明
    };
    private static final Logger log = LoggerFactory.getLogger(GasterBlasterBeamRenderer.class);

    /**
     * 渲染 GB
     *
     * @param partialTick 部分刻，客户端插值
     */
    public static void render(GasterBlaster animatable, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int[][] color) {
        poseStack.pushPose(); // 在这里压栈
        float size = animatable.getSize();
        float radius = size * 0.5f;
        int segments = Config.COMMON.segments.getAsInt();
        float partialSize = 0f;
        int fireTick = animatable.getFireTick();
        int shotTick = animatable.getShotTick();
        int decayTick = animatable.getDecayTick();
        int discardTick = decayTick + 3;
        float animTick = animatable.tickCount + partialTick;

        poseStack.translate(0, animatable.getEyeHeight(), 0);
        if (animatable.tickCount < fireTick) {
            partialSize = Mth.lerp(animTick/ fireTick, 0, radius * 0.75f);
            poseStack.pushPose();
            RenderUtils.renderSphere(poseStack.last(), buffer.getBuffer(BEAM_NO_TRANSPARENCY), partialSize * 0.5f, segments, color[0][0], color[0][1], color[0][2], color[0][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
            RenderUtils.renderSphere(poseStack.last(), buffer.getBuffer(BEAM_ENERGY_SWIRL), partialSize, segments, color[1][0], color[1][1], color[1][2], color[1][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

            renderLightRay(poseStack,buffer,animatable.getUUID(),radius*(animatable.isFollow()?1.5f:1f),animTick,partialSize,color);
        } else {
            float offset = animTick * 0.3f;
            float length = animatable.getLength();
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
            RenderUtils.renderCapsule(poseStack.last(), buffer.getBuffer(BEAM_NO_TRANSPARENCY_TRIANGLE_STRIP), buffer.getBuffer(BEAM_NO_TRANSPARENCY), partialSize * 0.5f, length, segments, color[0][0], color[0][1], color[0][2], color[0][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
            RenderUtils.renderCapsule(poseStack.last(),
                    buffer.getBuffer(RenderTypes.energySwirlTriangleStrip(ResourceLocations.BEAM_FLOW_TEXTURE, 0, -offset)),
                    buffer.getBuffer(RenderType.energySwirl(ResourceLocations.BEAM_FLOW_TEXTURE, 0, -offset)),
                    partialSize, length, segments, color[1][0], color[1][1], color[1][2], color[1][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 1f, length);
        }
        poseStack.popPose();
    }
    public static void renderLightRay(PoseStack poseStack, MultiBufferSource buffer, UUID uuid, float radius, float animTick, float partialSize,int[][] color) {
        // 使用实体UUID作为基础随机种子
        long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        // 渲染所有在生命周期内的光线
        for (int i = 0;; i++) {
            Random rayRandom = new Random(seed + i * 12345L); //伪随机，确保每条光线在生命周期内随机到的起点位置相同
            int rayLifeTime = rayRandom.nextInt(11);  // 每个光线的生命周期（tick）

            // 基于索引计算这条光线的开始时间，startTime > animTick控制在rayLifeTime生命周期内最大可以渲染的射线数量
            float startTime = i * 0.15f/radius;
            if (startTime > animTick) break;  // 还没开始的光线不渲染

            // 计算光线的进度
            float rayProgress = (animTick - startTime) / rayLifeTime;
            if (rayProgress > 1) continue;  // 已经结束的光线不渲染

            //光线长度
            float rayOuterLength = radius*(rayRandom.nextFloat()*3.0f+1.5f);                   // 光线外端点距离球心的长度
            float rayWidth = radius * rayRandom.nextFloat()*0.03f+0.03f;                       // 光线宽度
            float rayLength = rayOuterLength-partialSize;                                      // 光线长度
            float partialRayLength = Mth.lerp(rayProgress, rayLength, 0);
            double theta = rayRandom.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * rayRandom.nextDouble() - 1) - Math.PI / 2;
            // 球表面或外端点位置
            Vec3 outerPos = new Vec3(
                    Math.cos(theta) * Math.cos(phi),
                    Math.sin(phi),
                    Math.sin(theta) * Math.cos(phi)
            ).scale(partialSize+partialRayLength);
            // 当前外端点距离（从 outerRadius 线性减小到 radius）
            if (partialRayLength < 0.01f) continue; // 太近跳过

            Vec3 rayDir = outerPos.normalize().reverse();  // 从外向内
            poseStack.pushPose();
            // 移动到端点位置
            poseStack.translate(outerPos.x, outerPos.y, outerPos.z);
            // 旋转让 Z 轴指向光线方向（从外向球心），这样圆在 XY 平面，垂直于光线
            poseStack.mulPose(RotUtils.rotation(new Vec3(0, 0, 1), rayDir));
            poseStack.scale(rayWidth, rayWidth, Mth.lerp(rayProgress, rayWidth, partialRayLength));
            RenderUtils.drawCircle(poseStack.last(), buffer.getBuffer(RAY), new Vec3(0, 0, -0.5), 1.0f, 16, new Vec3(0, 0, 1),
                    color[1][0], color[1][1], color[1][2], (int) (color[1][3] * 0.8f * (1.0f - rayProgress * 0.5f)), OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
    }

}