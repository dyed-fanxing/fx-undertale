package com.sakpeipei.undertale.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.common.phys.OBB;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.*;


public class RenderUtils {

    // ========== 立方体 ==========

    /**
     * 立方体：中心点渲染，带UV缩放，与 renderCubeOutline 的顶点定义一致
     */
    public static void renderCube(PoseStack.Pose pose, VertexConsumer builder, float length, float width, float height,
                                  int r, int g, int b, int a, int overlay, int light,
                                  float uScale, float vScale) {
        Matrix4f matrix = pose.pose();
        float l = length * 0.5f;
        float w = width * 0.5f;
        float h = height * 0.5f;

        // 每个面的 UV 坐标乘以缩放系数
        // 前面 (Z-)
        builder.addVertex(matrix, -l, -w, -h).setColor(r, g, b, a).setUv(0, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        builder.addVertex(matrix, -l, w, -h).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        builder.addVertex(matrix, l, w, -h).setColor(r, g, b, a).setUv(uScale, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        builder.addVertex(matrix, l, -w, -h).setColor(r, g, b, a).setUv(uScale, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        // 后面 (Z+)
        builder.addVertex(matrix, -l, -w, h).setColor(r, g, b, a).setUv(uScale, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        builder.addVertex(matrix, l, -w, h).setColor(r, g, b, a).setUv(0, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        builder.addVertex(matrix, l, w, h).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        builder.addVertex(matrix, -l, w, h).setColor(r, g, b, a).setUv(uScale, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        // 左面 (X-)
        builder.addVertex(matrix, -l, -w, -h).setColor(r, g, b, a).setUv(0, vScale).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        builder.addVertex(matrix, -l, -w, h).setColor(r, g, b, a).setUv(uScale, vScale).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        builder.addVertex(matrix, -l, w, h).setColor(r, g, b, a).setUv(uScale, 0).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        builder.addVertex(matrix, -l, w, -h).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        // 右面 (X+)
        builder.addVertex(matrix, l, -w, -h).setColor(r, g, b, a).setUv(uScale, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        builder.addVertex(matrix, l, w, -h).setColor(r, g, b, a).setUv(uScale, 0).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        builder.addVertex(matrix, l, w, h).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        builder.addVertex(matrix, l, -w, h).setColor(r, g, b, a).setUv(0, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        // 上面 (Y+)
        builder.addVertex(matrix, -l, w, -h).setColor(r, g, b, a).setUv(0, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        builder.addVertex(matrix, -l, w, h).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        builder.addVertex(matrix, l, w, h).setColor(r, g, b, a).setUv(uScale, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        builder.addVertex(matrix, l, w, -h).setColor(r, g, b, a).setUv(uScale, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        // 下面 (Y-)
        builder.addVertex(matrix, -l, -w, -h).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        builder.addVertex(matrix, l, -w, -h).setColor(r, g, b, a).setUv(uScale, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        builder.addVertex(matrix, l, -w, h).setColor(r, g, b, a).setUv(uScale, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        builder.addVertex(matrix, -l, -w, h).setColor(r, g, b, a).setUv(0, vScale).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
    }

    public static void renderCube(PoseStack.Pose pose, VertexConsumer builder, float length, float width, float height,
                                  int r, int g, int b, int a, int overlay, int light) {
        renderCube(pose, builder, length, width, height, r, g, b, a, overlay, light, 1f, 1f);
    }

    public static void renderCube(PoseStack.Pose pose, VertexConsumer builder, float size,
                                  int r, int g, int b, int a, int overlay, int light) {
        renderCube(pose, builder, size, size, size, r, g, b, a, overlay, light, 1f, 1f);
    }

    /**
     * 立方体：端面中心向前渲染，带UV缩放，与 renderCubeOutline 的顶点定义一致
     */
    public static void renderCubeFromBackCenter(PoseStack.Pose pose, VertexConsumer consumer,
                                                float length, float width, float height,
                                                int r, int g, int b, int a, int overlay, int light,
                                                float uScale, float vScale) {
        float halfW = width * 0.5f;
        Vec3[] v = new Vec3[8];
        v[0] = new Vec3(-halfW, 0, 0);        // 左下后
        v[1] = new Vec3(halfW, 0, 0);        // 右下后
        v[2] = new Vec3(halfW, 0, length);   // 右下前
        v[3] = new Vec3(-halfW, 0, length);   // 左下前
        v[4] = new Vec3(-halfW, height, 0);   // 左上后
        v[5] = new Vec3(halfW, height, 0);   // 右上后
        v[6] = new Vec3(halfW, height, length); // 右上前
        v[7] = new Vec3(-halfW, height, length); // 左上前

        // 绘制6个面（每个面使用统一法线），UV 乘以缩放
        // 前 (Z+)
        drawQuad(pose, consumer, v[3], v[2], v[6], v[7], 0, 0, 1, 0, 0, uScale, vScale, r, g, b, a, overlay, light);
        // 后 (Z-)
        drawQuad(pose, consumer, v[0], v[1], v[5], v[4], 0, 0, -1, 0, 0, uScale, vScale, r, g, b, a, overlay, light);
        // 左 (X-)
        drawQuad(pose, consumer, v[0], v[4], v[7], v[3], -1, 0, 0, 0, 0, uScale, vScale, r, g, b, a, overlay, light);
        // 右 (X+)
        drawQuad(pose, consumer, v[1], v[2], v[6], v[5], 1, 0, 0, 0, 0, uScale, vScale, r, g, b, a, overlay, light);
        // 上 (Y+)
        drawQuad(pose, consumer, v[4], v[5], v[6], v[7], 0, 1, 0, 0, 0, uScale, vScale, r, g, b, a, overlay, light);
        // 下 (Y-)
        drawQuad(pose, consumer, v[0], v[3], v[2], v[1], 0, -1, 0, 0, 0, uScale, vScale, r, g, b, a, overlay, light);
    }

    public static void renderCubeFromBackCenter(PoseStack.Pose pose, VertexConsumer consumer,
                                                float length, float width, float height,
                                                int r, int g, int b, int a, int overlay, int light) {
        renderCubeFromBackCenter(pose, consumer, length, width, height, r, g, b, a, overlay, light, 1f, 1f);
    }

    public static void renderCubeOutlineByVertex(PoseStack.Pose pose, VertexConsumer consumer,
                                                 float length, float width, float height,
                                                 int r, int g, int b, int a,
                                                 int overlay, int light) {
        Vec3[] vertices = new Vec3[8];
        vertices[0] = new Vec3(0, 0, 0);
        vertices[1] = new Vec3(length, 0, 0);
        vertices[2] = new Vec3(length, 0, width);
        vertices[3] = new Vec3(0, 0, width);
        vertices[4] = new Vec3(0, height, 0);
        vertices[5] = new Vec3(length, height, 0);
        vertices[6] = new Vec3(length, height, width);
        vertices[7] = new Vec3(0, height, width);
        renderLine(pose, consumer, vertices[0], vertices[1], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[1], vertices[2], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[2], vertices[3], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[3], vertices[0], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[4], vertices[5], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[5], vertices[6], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[6], vertices[7], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[7], vertices[4], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[0], vertices[4], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, vertices[1], vertices[5], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, vertices[2], vertices[6], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, vertices[3], vertices[7], 0, 0, 1, r, g, b, a);
    }

    public static void renderCubeOutline(PoseStack.Pose pose, VertexConsumer consumer,
                                         float length, float width, float height,
                                         int r, int g, int b, int a,
                                         int overlay, int light) {
        float halfW = width * 0.5f;
        Vec3[] vertices = new Vec3[8];
        vertices[0] = new Vec3(-halfW, 0, 0);
        vertices[1] = new Vec3(halfW, 0, 0);
        vertices[2] = new Vec3(halfW, 0, length);
        vertices[3] = new Vec3(-halfW, 0, length);
        vertices[4] = new Vec3(-halfW, height, 0);
        vertices[5] = new Vec3(halfW, height, 0);
        vertices[6] = new Vec3(halfW, height, length);
        vertices[7] = new Vec3(-halfW, height, length);
        renderLine(pose, consumer, vertices[0], vertices[1], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[1], vertices[2], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[2], vertices[3], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[3], vertices[0], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[4], vertices[5], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[5], vertices[6], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[6], vertices[7], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[7], vertices[4], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, vertices[0], vertices[4], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, vertices[1], vertices[5], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, vertices[2], vertices[6], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, vertices[3], vertices[7], 0, 0, 1, r, g, b, a);
    }





    /**
     * 胶囊体：竖向，带UV缩放 *********************************************************************************************
     *
     * @param pose               姿态
     * @param sideConsumer       圆柱侧面渲染器（TRIANGLE_STRIP）
     * @param hemisphereConsumer 半球渲染器（QUAD）
     */
    public static void renderCapsule(PoseStack.Pose pose, VertexConsumer sideConsumer, VertexConsumer hemisphereConsumer,
                                     float radius, float length, int segments,
                                     int r, int g, int b, int a,
                                     int overlay, int light,
                                     float uScale, float vScale) {
        int latSegments = segments / 2;
        int hemiLatSegments = latSegments / 2;
        if (hemiLatSegments < 1) hemiLatSegments = 1;

        float deltaTheta = Mth.TWO_PI / segments;
        float deltaPhi = Mth.PI / latSegments;

        float[] ringRadius = new float[latSegments + 1];
        float[] ringY = new float[latSegments + 1];
        for (int i = 0; i <= latSegments; i++) {
            float phi = i * deltaPhi - Mth.HALF_PI;
            float cosPhi = Mth.cos(phi);
            float sinPhi = Mth.sin(phi);
            ringRadius[i] = radius * cosPhi;
            ringY[i] = radius * sinPhi;
        }
        ringY[latSegments / 2] = 0;
        ringRadius[latSegments / 2] = radius;

        // 渲染下半球（使用 QUADS 模式）
        renderHemisphere(pose, hemisphereConsumer, radius, ringRadius, ringY, latSegments, hemiLatSegments,
                segments, deltaTheta, deltaPhi, 0, 0, r, g, b, a, overlay, light, uScale, vScale);

        // 渲染圆柱侧面（使用 TRIANGLE_STRIP）
        renderCylinderSide(pose, sideConsumer, radius, length, segments, r, g, b, a, overlay, light, uScale,vScale);

        // 渲染上半球（使用 QUADS 模式）
        renderHemisphere(pose, hemisphereConsumer, radius, ringRadius, ringY, latSegments, hemiLatSegments,
                segments, deltaTheta, deltaPhi, length, 0.5f, r, g, b, a, overlay, light, uScale, vScale);
    }
    /**
     * 胶囊体：竖向（使用默认 UV 缩放 1.0）
     */
    public static void renderCapsule(PoseStack.Pose pose, VertexConsumer sideConsumer, VertexConsumer hemisphereConsumer,
                                     float radius, float length, int segments,
                                     int r, int g, int b, int a,
                                     int overlay, int light) {
        renderCapsule(pose, sideConsumer, hemisphereConsumer,
                radius, length, segments, r, g, b, a, overlay, light, 1f, 1f);
    }






    /**
     * 圆柱体：竖向，带UV缩放 *********************************************************************************************
     *
     * @param pose         姿态
     * @param sideConsumer 侧面渲染器（TRIANGLE_STRIP）
     * @param capConsumer  顶底面渲染器（TRIANGLE）
     */
    public static void renderCylinder(PoseStack.Pose pose, VertexConsumer sideConsumer, VertexConsumer capConsumer,
                                      float radius, float height, int segments,
                                      int r, int g, int b, int a, int overlay, int light,
                                      float uScale, float vScale) {
        // 侧面（使用 TRIANGLE_STRIP）
        renderCylinderSide(pose, sideConsumer, radius, height, segments, r, g, b, a, overlay, light, uScale, vScale);
        // 顶底面（使用 TRIANGLES）
        drawCircle(pose, capConsumer, new Vec3(0, 0, 0), radius, segments, new Vec3(0, -1, 0),
                r, g, b, a, overlay, light, uScale, vScale);
        drawCircle(pose, capConsumer, new Vec3(0, height, 0), radius, segments, new Vec3(0, 1, 0),
                r, g, b, a, overlay, light, uScale, vScale);
    }
    /**
     * 圆柱体：竖向，带UV缩放（使用默认 UV 缩放 1.0）
     */
    public static void renderCylinder(PoseStack.Pose pose, VertexConsumer sideConsumer, VertexConsumer capConsumer,
                                      float radius, float height, int segments,
                                      int r, int g, int b, int a, int overlay, int light) {
        renderCylinder(pose, sideConsumer, capConsumer, radius, height, segments, r, g, b, a, overlay, light, 1f, 1f);
    }
    public static void renderCylinderOutline(PoseStack.Pose pose, VertexConsumer consumer,
                                             float radius, float height, int segments,
                                             int r, int g, int b, int a, int overlay, int light) {
        float step = Mth.TWO_PI / segments;
        for (int i = 0; i < segments; i++) {
            float theta1 = i * step;
            float theta2 = (i + 1) * step;
            Vec3 up1 = new Vec3(radius * Mth.cos(theta1), 0, radius * Mth.sin(theta1));
            Vec3 up2 = new Vec3(radius * Mth.cos(theta2), 0, radius * Mth.sin(theta2));
            Vec3 down1 = new Vec3(radius * Mth.cos(theta1), height, radius * Mth.sin(theta1));
            Vec3 down2 = new Vec3(radius * Mth.cos(theta2), height, radius * Mth.sin(theta2));
            Vec3 normal = up1.normalize();
            renderLine(pose, consumer, up1, up2, (float) normal.x, (float) normal.y, (float) normal.z, r, g, b, a);
            renderLine(pose, consumer, up1, down1, (float) normal.x, (float) normal.y, (float) normal.z, r, g, b, a);
            renderLine(pose, consumer, down1, down2, (float) normal.x, (float) normal.y, (float) normal.z, r, g, b, a);
        }
    }
    /**
     * 圆柱侧面：竖向，带UV缩放（使用 TRIANGLE_STRIP 模式，最优）
     */
    public static void renderCylinderSide(PoseStack.Pose pose, VertexConsumer consumer,
                                          float radius, float height, int segments,
                                          int r, int g, int b, int a, int overlay, int light,
                                          float uScale, float vScale) {
        float step = Mth.TWO_PI / segments;
        Matrix4f matrix = pose.pose();
        float vBottom = 0.5f * vScale;
        // 交替排列：底、顶、底、顶、...
        for (int i = 0; i <= segments; i++) {
            float theta = i * step;
            float cos = Mth.cos(theta), sin = Mth.sin(theta);
            float u = i / (float) segments * uScale;
            // 底部点
            consumer.addVertex(matrix, radius * cos, 0, radius * sin)
                    .setNormal(pose, cos, 0, sin)
                    .setUv(u, vBottom)
                    .setColor(r, g, b, a)
                    .setOverlay(overlay)
                    .setLight(light);
            // 顶部点
            consumer.addVertex(matrix, radius * cos, height, radius * sin)
                    .setNormal(pose, cos, 0, sin)
                    .setUv(u, vScale)
                    .setColor(r, g, b, a)
                    .setOverlay(overlay)
                    .setLight(light);
        }
    }

    public static void renderCylinderSide(PoseStack.Pose pose, VertexConsumer consumer,
                                          float radius, float height, int segments,
                                          int r, int g, int b, int a, int overlay, int light) {
        renderCylinderSide(pose, consumer, radius, height, segments, r, g, b, a, overlay, light, 1f, 1f);
    }





    /**
     * 球体：带UV缩放（使用 QUADS 模式，最优实现） *********************************************************************************************
     *
     * @param pose     姿态
     * @param consumer 渲染器（QUADS 或 TRIANGLES）
     */
    public static void renderSphere(PoseStack.Pose pose, VertexConsumer consumer,
                                    float radius, int segments,
                                    int r, int g, int b, int a, int overlay, int light,
                                    float uScale, float vScale) {
        int latSegments = segments / 2;
        float deltaTheta = Mth.TWO_PI / segments;
        float deltaPhi = Mth.PI / latSegments;

        for (int i = 0; i < latSegments; i++) {
            float phi1 = i * deltaPhi - Mth.HALF_PI;
            float phi2 = (i + 1) * deltaPhi - Mth.HALF_PI;
            float sinPhi1 = Mth.sin(phi1), cosPhi1 = Mth.cos(phi1);
            float sinPhi2 = Mth.sin(phi2), cosPhi2 = Mth.cos(phi2);
            float r1 = radius * cosPhi1;
            float r2 = radius * cosPhi2;
            float v1 = (float) i / latSegments * vScale;
            float v2 = (float) (i + 1) / latSegments * vScale;

            for (int j = 0; j < segments; j++) {
                float theta1 = j * deltaTheta;
                float theta2 = (j + 1) * deltaTheta;
                float sinTheta1 = Mth.sin(theta1), cosTheta1 = Mth.cos(theta1);
                float sinTheta2 = Mth.sin(theta2), cosTheta2 = Mth.cos(theta2);
                float u1 = (float) j / segments * uScale;
                float u2 = (float) (j + 1) / segments * uScale;

                float x1 = r1 * cosTheta1, y1 = radius * sinPhi1, z1 = r1 * sinTheta1;
                float x2 = r2 * cosTheta1, y2 = radius * sinPhi2, z2 = r2 * sinTheta1;
                float x3 = r2 * cosTheta2, y3 = radius * sinPhi2, z3 = r2 * sinTheta2;
                float x4 = r1 * cosTheta2, y4 = radius * sinPhi1, z4 = r1 * sinTheta2;

                float nx1 = cosPhi1 * cosTheta1, nz1 = cosPhi1 * sinTheta1;
                float nx2 = cosPhi2 * cosTheta1, nz2 = cosPhi2 * sinTheta1;
                float nx3 = cosPhi2 * cosTheta2, nz3 = cosPhi2 * sinTheta2;
                float nx4 = cosPhi1 * cosTheta2, nz4 = cosPhi1 * sinTheta2;

                Matrix4f matrix = pose.pose();
                consumer.addVertex(matrix, x1, y1, z1).setNormal(pose, nx1, sinPhi1, nz1).setUv(u1, v1).setColor(r, g, b, a).setOverlay(overlay).setLight(light);
                consumer.addVertex(matrix, x2, y2, z2).setNormal(pose, nx2, sinPhi2, nz2).setUv(u1, v2).setColor(r, g, b, a).setOverlay(overlay).setLight(light);
                consumer.addVertex(matrix, x3, y3, z3).setNormal(pose, nx3, sinPhi2, nz3).setUv(u2, v2).setColor(r, g, b, a).setOverlay(overlay).setLight(light);
                consumer.addVertex(matrix, x4, y4, z4).setNormal(pose, nx4, sinPhi1, nz4).setUv(u2, v1).setColor(r, g, b, a).setOverlay(overlay).setLight(light);
            }
        }
    }

    /**
     * 球体：带UV缩放（使用默认 UV 缩放 1.0）
     */
    public static void renderSphere(PoseStack.Pose pose, VertexConsumer consumer,
                                    float radius, int segments,
                                    int r, int g, int b, int a, int overlay, int light) {
        renderSphere(pose, consumer, radius, segments, r, g, b, a, overlay, light, 1f, 1f);
    }
    /**
     * 渲染半球（辅助方法，使用 QUADS 模式）
     */
    private static void renderHemisphere(PoseStack.Pose pose, VertexConsumer consumer,
                                         float radius, float[] ringRadius, float[] ringY,
                                         int latSegments, int hemiLatSegments,
                                         int segments, float deltaTheta, float deltaPhi,
                                         float yOffset, float vOffset,
                                         int r, int g, int b, int a, int overlay, int light,
                                         float uScale, float vScale) {
        int startIndex = vOffset < 0.5f ? 0 : latSegments / 2;
        int endIndex = vOffset < 0.5f ? latSegments / 2 : latSegments;

        Matrix4f matrix = pose.pose();

        // 使用 QUADS 模式渲染每一层
        for (int i = startIndex; i < endIndex; i++) {
            float r1 = ringRadius[i];
            float r2 = ringRadius[i + 1];
            float y1 = ringY[i] + yOffset;
            float y2 = ringY[i + 1] + yOffset;
            float v1 = (vOffset < 0.5f ?
                    (float) i / hemiLatSegments * 0.5f :
                    (0.5f + 0.5f * (i - latSegments / 2) / hemiLatSegments)) * vScale;
            float v2 = (vOffset < 0.5f ?
                    (float) (i + 1) / hemiLatSegments * 0.5f :
                    (0.5f + 0.5f * (i + 1 - latSegments / 2) / hemiLatSegments)) * vScale;

            for (int j = 0; j < segments; j++) {
                float theta1 = j * deltaTheta;
                float theta2 = (j + 1) * deltaTheta;
                float cos1 = Mth.cos(theta1), sin1 = Mth.sin(theta1);
                float cos2 = Mth.cos(theta2), sin2 = Mth.sin(theta2);
                float u1 = (float) j / segments * uScale;
                float u2 = (float) (j + 1) / segments * uScale;

                float x1 = r1 * cos1, z1 = r1 * sin1;
                float x2 = r2 * cos1, z2 = r2 * sin1;
                float x3 = r2 * cos2, z3 = r2 * sin2;
                float x4 = r1 * cos2, z4 = r1 * sin2;
                float y3 = y2, y4 = y1;

                float nx1 = cos1 * (r1 / radius), ny1 = (y1 - yOffset) / radius, nz1 = sin1 * (r1 / radius);
                float nx2 = cos1 * (r2 / radius), ny2 = (y2 - yOffset) / radius, nz2 = sin1 * (r2 / radius);
                float nx3 = cos2 * (r2 / radius), ny3 = (y2 - yOffset) / radius, nz3 = sin2 * (r2 / radius);
                float nx4 = cos2 * (r1 / radius), ny4 = (y1 - yOffset) / radius, nz4 = sin2 * (r1 / radius);

                consumer.addVertex(matrix, x1, y1, z1)
                        .setNormal(pose, nx1, ny1, nz1)
                        .setUv(u1, v1)
                        .setColor(r, g, b, a)
                        .setOverlay(overlay)
                        .setLight(light);
                consumer.addVertex(matrix, x2, y2, z2)
                        .setNormal(pose, nx2, ny2, nz2)
                        .setUv(u1, v2)
                        .setColor(r, g, b, a)
                        .setOverlay(overlay)
                        .setLight(light);
                consumer.addVertex(matrix, x3, y3, z3)
                        .setNormal(pose, nx3, ny3, nz3)
                        .setUv(u2, v2)
                        .setColor(r, g, b, a)
                        .setOverlay(overlay)
                        .setLight(light);
                consumer.addVertex(matrix, x4, y4, z4)
                        .setNormal(pose, nx4, ny4, nz4)
                        .setUv(u2, v1)
                        .setColor(r, g, b, a)
                        .setOverlay(overlay)
                        .setLight(light);
            }
        }
    }




    // ========== OBB ========== *********************************************************************************************
    public static void renderOBB(PoseStack poseStack, VertexConsumer consumer, OBB obb,
                                 int r, int g, int b, int a, int packedLight) {
        poseStack.pushPose();
        Vec3 center = obb.getCenter();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(obb.getYaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(obb.getPitch()));
        PoseStack.Pose pose = poseStack.last();
        renderCube(pose, consumer, obb.getLength(), obb.getWidth(), obb.getHeight(),
                r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
        poseStack.popPose();
    }

    public static void renderOBBOutline(PoseStack.Pose pose, VertexConsumer consumer, OBB obb,
                                        int r, int g, int b, int a) {
        renderOBBOutline(pose, consumer, obb, r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public static void renderOBBOutline(PoseStack.Pose pose, VertexConsumer consumer, OBB obb,
                                        float r, float g, float b, float a) {
        Vec3[] verts = obb.getVertices();
        renderLine(pose, consumer, verts[0], verts[1], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, verts[1], verts[3], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, verts[3], verts[2], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, verts[2], verts[0], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, verts[4], verts[5], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, verts[5], verts[7], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, verts[7], verts[6], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, verts[6], verts[4], 0, 1, 0, r, g, b, a);
        renderLine(pose, consumer, verts[0], verts[4], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, verts[1], verts[5], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, verts[2], verts[6], 0, 0, 1, r, g, b, a);
        renderLine(pose, consumer, verts[3], verts[7], 0, 0, 1, r, g, b, a);
    }

    // ******************************************************2D平面***************************************************************//
    /**
     * 四边形：UV缩放
     */
    public static void drawQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                                float normalX, float normalY, float normalZ,
                                float uMin, float vMin, float uMax, float vMax,
                                int r, int g, int b, int a, int overlay, int light,
                                float uScale, float vScale) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(uMin * uScale, vMin * vScale)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(uMin * uScale, vMax * vScale)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        consumer.addVertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(uMax * uScale, vMax * vScale)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        consumer.addVertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(uMax * uScale, vMin * vScale)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
    }
    // 新增带UV缩放的 drawQuad 重载
    public static void drawQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                                float normalX, float normalY, float normalZ,
                                float uMin, float vMin, float uMax, float vMax,
                                int r, int g, int b, int a, int overlay, int light) {
        drawQuad(pose, consumer, p1, p2, p3, p4, normalX, normalY, normalZ, uMin, vMin, uMax, vMax, r, g, b, a, overlay, light, 1f, 1f);
    }

    public static void renderBillboardQuad(Vec3 from, Vec3 to, float halfWidth, float halfHeight,
                                           PoseStack.Pose pose, VertexConsumer consumer,
                                           int r, int g, int b, int a, float v,
                                           int overlay, int light) {
        drawXZOrYZQuad(from, to, halfWidth, 0, pose, consumer, r, g, b, a, v, overlay, light, 0, 1, 0);
        drawXZOrYZQuad(from, to, 0, halfHeight, pose, consumer, r, g, b, a, v, overlay, light, 1, 0, 0);
    }

    public static void drawXZOrYZQuad(Vec3 from, Vec3 to, float halfWidth, float halfHeight,
                                      PoseStack.Pose pose, VertexConsumer consumer,
                                      int r, int g, int b, int a, float v,
                                      int overlay, int light,
                                      float normalX, float normalY, float normalZ) {
        drawQuad(pose, consumer,
                from.add(halfWidth, halfHeight, 0),
                from.add(-halfWidth, -halfHeight, 0),
                to.add(-halfWidth, -halfHeight, 0),
                to.add(halfWidth, halfHeight, 0),
                normalX, normalY, normalZ,
                0, 0, v, v,
                r, g, b, a, overlay, light);
    }

    public static void drawXYQuad(Vec3 center, float width, float height, PoseStack.Pose pose,
                                  VertexConsumer consumer, int r, int g, int b, int a, float v,
                                  int overlay, int light,
                                  float normalX, float normalY, float normalZ) {
        float halfWidth = width * 0.5f, halfHeight = height * 0.5f;
        drawQuad(pose, consumer,
                center.add(halfWidth, halfHeight, 0),
                center.add(-halfWidth, halfHeight, 0),
                center.add(-halfWidth, -halfHeight, 0),
                center.add(halfWidth, -halfHeight, 0),
                normalX, normalY, normalZ,
                0, 0, v, v,
                r, g, b, a, overlay, light);
    }



    // ========== 私有辅助：带逐顶点法线的四边形 ==========
    private static void drawQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float nx1, float ny1, float nz1,
                                 float nx2, float ny2, float nz2,
                                 float nx3, float ny3, float nz3,
                                 float nx4, float ny4, float nz4,
                                 float u1, float v1, float u2, float v2, float u3, float v3, float u4, float v4,
                                 int r, int g, int b, int a, int overlay, int light) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, x1, y1, z1).setNormal(pose, nx1, ny1, nz1).setUv(u1, v1).setColor(r, g, b, a).setOverlay(overlay).setLight(light);
        consumer.addVertex(matrix, x2, y2, z2).setNormal(pose, nx2, ny2, nz2).setUv(u2, v2).setColor(r, g, b, a).setOverlay(overlay).setLight(light);
        consumer.addVertex(matrix, x3, y3, z3).setNormal(pose, nx3, ny3, nz3).setUv(u3, v3).setColor(r, g, b, a).setOverlay(overlay).setLight(light);
        consumer.addVertex(matrix, x4, y4, z4).setNormal(pose, nx4, ny4, nz4).setUv(u4, v4).setColor(r, g, b, a).setOverlay(overlay).setLight(light);
    }


    /**
     * 圆形：带UV缩放（使用 TRIANGLE_FAN 模式，最优）  *********************************************************************************************
     */
    public static void drawCircleTriangleFan(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center, float radius, int segments, Vec3 normal,
                                  int r, int g, int b, int a, int overlay, int light,
                                  float uScale, float vScale) {
        float delta = Mth.TWO_PI / segments;
        Matrix4f matrix = pose.pose();
        // 中心点
        consumer.addVertex(matrix, (float) center.x, (float) center.y, (float) center.z)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                .setUv(0.5f * uScale, 0.5f * vScale)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        // 圆周上的点
        for (int i = 0; i <= segments; i++) {
            float angle = i * delta;
            Vec3 point = center.add(radius * Mth.cos(angle), 0, radius * Mth.sin(angle));
            float u = 0.5f + 0.5f * Mth.cos(angle);
            float v = 0.5f + 0.5f * Mth.sin(angle);

            consumer.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                    .setUv(u * uScale, v * vScale)
                    .setColor(r, g, b, a)
                    .setOverlay(overlay)
                    .setLight(light);
        }
    }
    public static void drawCircleTriangleFan(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center, float radius, int segments, Vec3 normal,
                                  int r, int g, int b, int a, int overlay, int light) {
        drawCircleTriangleFan(pose, consumer, center, radius, segments, normal, r, g, b, a, overlay, light, 1f, 1f);
    }

    /**
     * 用 TRIANGLES 模式绘制圆（每个三角形独立，适合多实例渲染）
     */
    public static void drawCircle(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center, float radius, int segments, Vec3 normal,
                                           int r, int g, int b, int a, int overlay, int light,
                                           float uScale, float vScale) {
        float delta = Mth.TWO_PI / segments;
        Matrix4f matrix = pose.pose();
        for (int i = 0; i < segments; i++) {
            float angle1 = i * delta;
            float angle2 = (i + 1) * delta;

            Vec3 p1 = center;
            Vec3 p2 = center.add(radius * Mth.cos(angle1), 0, radius * Mth.sin(angle1));
            Vec3 p3 = center.add(radius * Mth.cos(angle2), 0, radius * Mth.sin(angle2));

            // 三角形1: center → p2 → p3
            consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                    .setUv(0.5f * uScale, 0.5f * vScale)
                    .setColor(r, g, b, a)
                    .setOverlay(overlay)
                    .setLight(light);
            consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                    .setUv((0.5f + 0.5f * Mth.cos(angle1)) * uScale, (0.5f + 0.5f * Mth.sin(angle1)) * vScale)
                    .setColor(r, g, b, a)
                    .setOverlay(overlay)
                    .setLight(light);
            consumer.addVertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                    .setUv((0.5f + 0.5f * Mth.cos(angle2)) * uScale, (0.5f + 0.5f * Mth.sin(angle2)) * vScale)
                    .setColor(r, g, b, a)
                    .setOverlay(overlay)
                    .setLight(light);
        }
    }
    /**
     * 用 TRIANGLES 模式绘制圆（每个三角形独立，适合多实例渲染）
     */
    public static void drawCircle(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center, float radius, int segments, Vec3 normal,
                                           int r, int g, int b, int a, int overlay, int light) {
        drawCircle(pose,consumer, center, radius, segments, normal, r, g, b, a, overlay, light,1f, 1f);
    }


    /**
     * 绘制单个三角形，连续
     */
    private static void drawTriangle(PoseStack.Pose pose, VertexConsumer consumer,
                                     Vec3 p1, Vec3 p2, Vec3 p3,
                                     float nx, float ny, float nz,
                                     int r, int g, int b, int a,
                                     int overlay, int light) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .setNormal(pose, nx, ny, nz)
                .setUv(0, 0) // 纯色纹理，UV任意
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                .setNormal(pose, nx, ny, nz)
                .setUv(0, 0)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        consumer.addVertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                .setNormal(pose, nx, ny, nz)
                .setUv(0, 0)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
    }

    /**
     * 线段  *********************************************************************************************
     */
    public static void renderLine(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p1, Vec3 p2,
                                  float normalX, float normalY, float normalZ,
                                  float r, float g, float b, float a) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .setNormal(pose, normalX, normalY, normalZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                .setNormal(pose, normalX, normalY, normalZ).setColor(r, g, b, a);
    }
    public static void renderLine(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p1, Vec3 p2,
                                  float normalX, float normalY, float normalZ,
                                  int r, int g, int b, int a) {
        renderLine(pose, consumer, p1, p2, normalX, normalY, normalZ, r / 255f, g / 255f, b / 255f, a / 255f);
    }
}