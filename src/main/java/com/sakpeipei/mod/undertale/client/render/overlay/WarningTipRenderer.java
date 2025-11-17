package com.sakpeipei.mod.undertale.client.render.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yujinbao
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public class WarningTipRenderer {
    private static final Map<UUID, WarningTipShape> activeWarnings = new ConcurrentHashMap<>();

    public static void addWarning(WarningTipShape warning) {
        activeWarnings.put(warning.getGroupId(), warning);
    }

    public static void removeWarning(UUID groupId) {
        activeWarnings.remove(groupId);
    }

    public static void tickAll() {
        // 移除已过期的预警
        activeWarnings.entrySet().removeIf(entry -> {
            entry.getValue().tick();
            return !entry.getValue().shouldRender();
        });
    }

    public static void renderAllWarnings(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Frustum frustum = event.getFrustum();

        // 开始渲染批次
        poseStack.pushPose();

        // 关键：调整到世界坐标（减去摄像机位置）
        Vec3 cameraPos = camera.getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // 渲染所有活跃的预警
        for (WarningTipShape warning : activeWarnings.values()) {
            if (shouldRenderWarning(warning, frustum)) {
                renderWarningShape(poseStack, bufferSource, warning);
            }
        }

        // 结束渲染批次
        bufferSource.endBatch();
        poseStack.popPose();
    }

    private static boolean shouldRenderWarning(WarningTipShape warning, Frustum frustum) {
        if (!warning.shouldRender()) return false;

        // 视锥体剔除：只渲染在视野内的预警
        AABB bounds = warning.getBoundingBox();
        return bounds == null || frustum.isVisible(bounds);
    }

    private static void renderWarningShape(PoseStack poseStack, MultiBufferSource bufferSource,
                                           WarningTipShape warning) {
        float alpha = warning.getAlpha();
        int redColor = 0xFF0000; // 红色
        int packedColor = (redColor & 0x00FFFFFF) | ((int)(alpha * 255) << 24);

        Matrix4f poseMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // 获取轮廓点
        List<Vec3> baseOutline = warning.getBaseOutline();
        List<Vec3> topOutline = warning.getTopOutline();

        // 使用线条渲染类型绘制边框
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.LINES);

        // 渲染底部边框
        renderOutline(lineConsumer, poseMatrix, normalMatrix, baseOutline, packedColor);

        // 渲染顶部边框  
        renderOutline(lineConsumer, poseMatrix, normalMatrix, topOutline, packedColor);

        // 渲染垂直连接线
        renderVerticalConnectors(lineConsumer, poseMatrix, normalMatrix, baseOutline, topOutline, packedColor);
    }

    private static void renderOutline(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                      List<Vec3> points, int color) {
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 start = points.get(i);
            Vec3 end = points.get(i + 1);

            consumer.vertex(pose, (float)start.x, (float)start.y, (float)start.z)
                    .color(color)
                    .normal(normal, 0, 1, 0)
                    .endVertex();

            consumer.vertex(pose, (float)end.x, (float)end.y, (float)end.z)
                    .color(color)
                    .normal(normal, 0, 1, 0)
                    .endVertex();
        }
    }

    private static void renderVerticalConnectors(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                                 List<Vec3> base, List<Vec3> top, int color) {
        for (int i = 0; i < base.size(); i++) {
            Vec3 basePoint = base.get(i);
            Vec3 topPoint = top.get(i);

            consumer.vertex(pose, (float)basePoint.x, (float)basePoint.y, (float)basePoint.z)
                    .color(color)
                    .normal(normal, 0, 1, 0)
                    .endVertex();

            consumer.vertex(pose, (float)topPoint.x, (float)topPoint.y, (float)topPoint.z)
                    .color(color)
                    .normal(normal, 0, 1, 0)
                    .endVertex();
        }
    }
}
