package com.fanxing.fx_undertale.client.render.entity.summon;

import com.fanxing.fx_undertale.FxUndertale;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.fanxing.fx_undertale.Config;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.common.ResourceLocations;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.fx_undertale.utils.RenderUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.UUID;

public class GasterBlasterBeamRenderer {
    private static final RenderType BEAM_NO_TRANSPARENCY = RenderTypes.BEAM_NO_CULL.apply(ResourceLocations.WHITE_TEXTURE, false);
//    private static final RenderType BEAM_NO_TRANSPARENCY_TRIANGLE_STRIP = RenderTypes.BEAM_NO_CULL_TRIANGLE_STRIP.apply(ResourceLocations.WHITE_TEXTURE, false);
    private static final RenderType BEAM_NO_TRANSPARENCY_TRIANGLE_STRIP = RenderTypes.BEAM_NO_CULL_TRIANGLE_STRIP.apply(ResourceLocations.WHITE_TEXTURE, false);
    private static final RenderType BEAM_ENERGY_OUTER = RenderTypes.ENERGY_BEAM.apply(ResourceLocations.WHITE_TEXTURE);
    private static final RenderType BEAM_ENERGY_OUTER_TRIANGLE_STRIP = RenderTypes.ENERGY_BEAM_TRIANGLE_STRIP.apply(ResourceLocations.WHITE_TEXTURE);
    public static final ResourceLocation FLOW_STRIPE = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,"textures/misc/white_stripe.png");
    private static final RenderType BEAM_FLOW_TRIANGLE_STRIP = RenderTypes.ENERGY_BEAM_TRIANGLE_STRIP.apply(FLOW_STRIPE);
    private static final RenderType BEAM_FLOW = RenderTypes.ENERGY_BEAM.apply(FLOW_STRIPE);

    private static final RenderType RAY = RenderTypes.ENERGY_TRIANGLES.apply(ResourceLocations.WHITE_TEXTURE, false);
    public static final float INNER_SCALE = 0.6f;
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
            {240, 255, 255, 255},   // 内层 能量层：纯白（最亮）
            {0, 50, 120, 255},      // 外层 泛光层：深蓝
            { 200,200,200, 150}    // 外层 流动条纹层：淡蓝
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
    public static void render(GasterBlaster animatable, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int[][] color) throws NoSuchFieldException, IllegalAccessException {
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
            RenderUtils.renderSphere(poseStack.last(), buffer.getBuffer(BEAM_ENERGY_OUTER), partialSize, segments, color[1][0], color[1][1], color[1][2], color[1][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            animatable.sphereRayEmitter.stretchCircleRender(poseStack, buffer.getBuffer(RAY), animTick,radius*(animatable.isFollow()?1.5f:1f), partialSize,color[1]);
//            renderLightRay(poseStack,buffer,animatable.getUUID(),radius,animTick,partialSize,color);
        } else {
            float offset = (animTick * 0.3f) % 1.0f;
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
            RenderType crashType = BEAM_NO_TRANSPARENCY;
            RenderType noCrashType = BEAM_NO_TRANSPARENCY; // 或者其他你测试过不崩溃的
            System.out.println("CRASH TYPE: " + crashType);
            System.out.println("  - canConsolidate: " + crashType.canConsolidateConsecutiveGeometry());
            System.out.println("  - sortOnUpload: " + crashType.sortOnUpload);
            System.out.println("  - format: " + crashType.format());
            System.out.println("NO CRASH TYPE: " + noCrashType);
            System.out.println("  - canConsolidate: " + noCrashType.canConsolidateConsecutiveGeometry());
            System.out.println("  - sortOnUpload: " + noCrashType.sortOnUpload);
            System.out.println("  - format: " + noCrashType.format());
            RenderUtils.renderCapsule(poseStack.last(), buffer.getBuffer(BEAM_NO_TRANSPARENCY), buffer.getBuffer(BEAM_NO_TRANSPARENCY), partialSize * INNER_SCALE, length, segments, color[0][0], color[0][1], color[0][2], color[0][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            RenderUtils.renderCapsule(poseStack.last(), buffer.getBuffer(BEAM_ENERGY_OUTER_TRIANGLE_STRIP), buffer.getBuffer(BEAM_ENERGY_OUTER), partialSize , length, segments, color[1][0], color[1][1], color[1][2], color[1][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            RenderUtils.renderCapsule(poseStack.last(), buffer.getBuffer(BEAM_FLOW_TRIANGLE_STRIP), buffer.getBuffer(BEAM_FLOW), partialSize , length, segments, color[2][0], color[2][1], color[2][2], color[2][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT,
                    1f,length*0.5f,-offset);
            poseStack.popPose();
        }
        poseStack.popPose();
        VertexConsumer sideConsumer = buffer.getBuffer(BEAM_NO_TRANSPARENCY_TRIANGLE_STRIP);
        if (sideConsumer instanceof BufferBuilder bb) {
            Field f = BufferBuilder.class.getDeclaredField("building");
            f.setAccessible(true);
            boolean building = f.getBoolean(bb);
            System.out.println("[DEBUG] sideConsumer hash=" + System.identityHashCode(bb) + ", building=" + building);
            if (!building) {
                // 打印 mode 和 format
                Field modeF = BufferBuilder.class.getDeclaredField("mode");
                modeF.setAccessible(true);
                Field formatF = BufferBuilder.class.getDeclaredField("format");
                formatF.setAccessible(true);
                System.out.println("  mode=" + modeF.get(bb) + ", format=" + formatF.get(bb));
            }
        }
    }
//    public static void renderLightRay(PoseStack poseStack, MultiBufferSource buffer, UUID uuid, float radius, float animTick, float partialSize,int[][] color) {
//
//        // 使用实体UUID作为基础随机种子
//        long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
//        // 渲染所有在生命周期内的光线
//        for (int i = 0;; i++) {
//            Random rayRandom = new Random(seed + i * 12345L); //伪随机，确保每条光线在生命周期内随机到的起点位置相同
//            int rayLifeTime = rayRandom.nextInt(11);  // 每个光线的生命周期（tick）
//
//            // 基于索引计算这条光线的开始时间，startTime > animTick控制在rayLifeTime生命周期内最大可以渲染的射线数量
//            float startTime = i * 0.15f/radius;
//            if (startTime > animTick) break;  // 还没开始的光线不渲染
//
//            // 计算光线的进度
//            float rayProgress = (animTick - startTime) / rayLifeTime;
//            if (rayProgress > 1) continue;  // 已经结束的光线不渲染
//
//
//
//            //光线长度
//            float rayOuterLength = radius*(rayRandom.nextFloat()*3.0f+1.5f);                   // 光线外端点距离球心的长度
//            float rayWidth = radius * rayRandom.nextFloat()*0.03f+0.01f;                       // 光线宽度
//            float rayLength = rayOuterLength-partialSize;                                      // 光线长度
//            float partialRayLength = Mth.lerp(rayProgress, rayLength, 0);
//            double theta = rayRandom.nextDouble() * 2 * Math.PI;
//            double phi = Math.acos(2 * rayRandom.nextDouble() - 1) - Math.PI / 2;
//            // 球表面或外端点位置
//            Vec3 outerPos = new Vec3(
//                    Math.cos(theta) * Math.cos(phi),
//                    Math.sin(phi),
//                    Math.sin(theta) * Math.cos(phi)
//            ).scale(partialSize+partialRayLength);
//            // 当前外端点距离（从 outerRadius 线性减小到 radius）
//            if (partialRayLength < 0.01f) continue; // 太近跳过
//
//            Vec3 rayDir = outerPos.normalize().reverse();  // 从外向内
//            poseStack.pushPose();
//            // 移动到端点位置
//            poseStack.translate(outerPos.x, outerPos.y, outerPos.z);
//            // 旋转让 Z 轴指向光线方向（从外向球心），这样圆在 XY 平面，垂直于光线
//            poseStack.mulPose(RotUtils.rotation(new Vec3(0, 0, 1), rayDir));
//            poseStack.scale(rayWidth, rayWidth, Mth.lerp(rayProgress, rayWidth, partialRayLength));
//            RenderUtils.renderCircle(poseStack.last(), buffer.getBuffer(RAY), new Vec3(0, 0, -0.5), 1.0f, 16, new Vec3(0, 0, 1),
//                    color[1][0], color[1][1], color[1][2], (int) (color[1][3] * 0.8f * (1.0f - rayProgress * 0.5f)), OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
//            poseStack.popPose();
//        }
//    }


}