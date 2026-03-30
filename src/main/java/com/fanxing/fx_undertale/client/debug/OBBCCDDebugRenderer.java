package com.fanxing.fx_undertale.client.debug;

import com.fanxing.fx_undertale.common.phys.OBB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class OBBCCDDebugRenderer {
    private static final int SAMPLES = 32;   // 插值步数
    private static final float LINE_WIDTH = 2.0f;
    private static boolean isShowCCDDebug = false;

    /**
     * 切换CCD调试显示状态
     */
    public static void toggleCCDDebug() {
        isShowCCDDebug = !isShowCCDDebug;
    }

    /**
     * 检查是否显示CCD调试
     */
    public static boolean isShowCCDDebug() {
        return isShowCCDDebug;
    }

    /**
     * 渲染旋转扫掠体（局部坐标系）
     * @param poseStack 当前渲染栈（已包含实体变换）
     * @param consumer VertexConsumer
     * @param startOBB 起始OBB（局部坐标）
     * @param angularVelocity 角速度向量（方向=旋转轴，大小=旋转角度，弧度，已按时间缩放）
     * @param pivot 旋转中心（局部坐标，通常为 Vec3.ZERO）
     * @param colorStart 起始颜色 (0xRRGGBB)
     * @param colorEnd 结束颜色
     */
    public static void renderSweptOBB(PoseStack poseStack, VertexConsumer consumer,
                                      OBB startOBB, Vector3f angularVelocity, Vec3 pivot,
                                      int colorStart, int colorEnd) {
        // 注意：angularVelocity 已经是按时间缩放过的弧度值
        for (int i = 0; i <= SAMPLES; i++) {
            float t = (float) i / SAMPLES;
            OBB obb = startOBB.rotateByAngularVelocity(new Vector3f(angularVelocity).mul(t), pivot);
            int color = lerpColor(colorStart, colorEnd, t);
            drawOBBWireframe(poseStack, consumer, obb, color);
        }
    }

    private static void drawOBBWireframe(PoseStack poseStack, VertexConsumer consumer,
                                         OBB obb, int color) {
        Vec3[] vertices = obb.getVertices(); // 局部坐标
        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0}, // bottom
                {4,5},{5,6},{6,7},{7,4}, // top
                {0,4},{1,5},{2,6},{3,7}  // vertical
        };
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.6f;

        for (int[] edge : edges) {
            Vec3 v1 = vertices[edge[0]];
            Vec3 v2 = vertices[edge[1]];
            consumer.addVertex(poseStack.last().pose(), (float)v1.x, (float)v1.y, (float)v1.z)
                    .setNormal(poseStack.last(),0,0,1)
                    .setColor(r, g, b, a);
            consumer.addVertex(poseStack.last().pose(), (float)v2.x, (float)v2.y, (float)v2.z)
                    .setNormal(poseStack.last(),0,0,1)
                    .setColor(r, g, b, a);
        }
    }


    /**
     * 绘制扫掠体（保守AABB）
     */
    public static void drawSweptAABB(PoseStack poseStack, VertexConsumer consumer,
                                     AABB aabb, Vec3 viewPos, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        drawAABBWireframe(poseStack, consumer, aabb, viewPos, r, g, b, 0.3f);
    }

    private static void drawAABBWireframe(PoseStack poseStack, VertexConsumer consumer,
                                          AABB aabb, Vec3 viewPos, float r, float g, float b, float a) {
        Vector3f min = new Vector3f((float)(aabb.minX - viewPos.x), (float)(aabb.minY - viewPos.y), (float)(aabb.minZ - viewPos.z));
        Vector3f max = new Vector3f((float)(aabb.maxX - viewPos.x), (float)(aabb.maxY - viewPos.y), (float)(aabb.maxZ - viewPos.z));
        Vector3f[] corners = {
                new Vector3f(min.x, min.y, min.z), new Vector3f(max.x, min.y, min.z),
                new Vector3f(max.x, min.y, max.z), new Vector3f(min.x, min.y, max.z),
                new Vector3f(min.x, max.y, min.z), new Vector3f(max.x, max.y, min.z),
                new Vector3f(max.x, max.y, max.z), new Vector3f(min.x, max.y, max.z)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] e : edges) {
            consumer.addVertex(poseStack.last().pose(), corners[e[0]].x, corners[e[0]].y, corners[e[0]].z).setColor(r,g,b,a);
            consumer.addVertex(poseStack.last().pose(), corners[e[1]].x, corners[e[1]].y, corners[e[1]].z).setColor(r,g,b,a);
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        int r1 = (c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int r2 = (c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        int r = (int)(r1 + (r2-r1)*t);
        int g = (int)(g1 + (g2-g1)*t);
        int b = (int)(b1 + (b2-b1)*t);
        return (r<<16)|(g<<8)|b;
    }

    /**
     * 渲染碰撞点（红色球体）
     * @param poseStack 当前渲染栈
     * @param consumer VertexConsumer
     * @param point 碰撞点位置（局部坐标）
     * @param color 颜色 (0xRRGGBB)
     * @param size 碰撞点大小
     */
    public static void renderCollisionPoint(PoseStack poseStack, VertexConsumer consumer, Vec3 point, int color, float size) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 1.0f;

        // 绘制十字标记
        float h = size / 2f;
        
        // X轴方向的线
        consumer.addVertex(poseStack.last().pose(), (float)(point.x - h), (float)point.y, (float)point.z)
                .setNormal(poseStack.last(), 1, 0, 0)
                .setColor(r, g, b, a);
        consumer.addVertex(poseStack.last().pose(), (float)(point.x + h), (float)point.y, (float)point.z)
                .setNormal(poseStack.last(), 1, 0, 0)
                .setColor(r, g, b, a);
        
        // Y轴方向的线
        consumer.addVertex(poseStack.last().pose(), (float)point.x, (float)(point.y - h), (float)point.z)
                .setNormal(poseStack.last(), 0, 1, 0)
                .setColor(r, g, b, a);
        consumer.addVertex(poseStack.last().pose(), (float)point.x, (float)(point.y + h), (float)point.z)
                .setNormal(poseStack.last(), 0, 1, 0)
                .setColor(r, g, b, a);
        
        // Z轴方向的线
        consumer.addVertex(poseStack.last().pose(), (float)point.x, (float)point.y, (float)(point.z - h))
                .setNormal(poseStack.last(), 0, 0, 1)
                .setColor(r, g, b, a);
        consumer.addVertex(poseStack.last().pose(), (float)point.x, (float)point.y, (float)(point.z + h))
                .setNormal(poseStack.last(), 0, 0, 1)
                .setColor(r, g, b, a);
    }
}