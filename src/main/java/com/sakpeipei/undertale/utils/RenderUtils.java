package com.sakpeipei.undertale.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.common.phys.OBB;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.*;

public class RenderUtils {
    /**
     * 简易正方体
     */
    public static void renderCube( PoseStack.Pose pose,VertexConsumer builder, float size, int r, int g, int b, int a, int overlay, int light) {
        renderCube( pose,builder, size, size, size, r, g, b, a, overlay, light);
    }

    /**
     * 简易立方体
     */
    public static void renderCube(PoseStack.Pose pose,VertexConsumer builder, float length, float width, float height,  int r, int g, int b, int a, int overlay, int light) {
        Matrix4f matrix = pose.pose();
        length = length * 0.5f;
        width = width * 0.5f;
        height = height * 0.5f;
        // 前面 (Z-)
        builder.addVertex(matrix, -length, -width, -height).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        builder.addVertex(matrix, -length, width, -height).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        builder.addVertex(matrix, length, width, -height).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);
        builder.addVertex(matrix, length, -width, -height).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, -1);

        // 后面 (Z+)
        builder.addVertex(matrix, -length, -width, height).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        builder.addVertex(matrix, length, -width, height).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        builder.addVertex(matrix, length, width, height).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        builder.addVertex(matrix, -length, width, height).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);

        // 左面 (X-)
        builder.addVertex(matrix, -length, -width, -height).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        builder.addVertex(matrix, -length, -width, height).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        builder.addVertex(matrix, -length, width, height).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);
        builder.addVertex(matrix, -length, width, -height).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, -1, 0, 0);

        // 右面 (X+)
        builder.addVertex(matrix, length, -width, -height).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        builder.addVertex(matrix, length, width, -height).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        builder.addVertex(matrix, length, width, height).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);
        builder.addVertex(matrix, length, -width, height).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 1, 0, 0);

        // 上面 (Y+)
        builder.addVertex(matrix, -length, width, -height).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        builder.addVertex(matrix, -length, width, height).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        builder.addVertex(matrix, length, width, height).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
        builder.addVertex(matrix, length, width, -height).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);

        // 下面 (Y-)
        builder.addVertex(matrix, -length, -width, -height).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        builder.addVertex(matrix, length, -width, -height).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        builder.addVertex(matrix, length, -width, height).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
        builder.addVertex(matrix, -length, -width, height).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, -1, 0);
    }

    /**
     * 渲染立方体轮廓，通过最小顶点渲染
     * @param pose      当前变换矩阵
     * @param consumer  顶点构建器
     * @param length    X轴方向长度
     * @param width     Z轴方向宽度（注意：根据实际渲染效果调整，此处保持与传入一致）
     * @param height    Y轴方向高度
     * @param r, g, b, a 颜色分量（0-255）
     * @param overlay   叠加纹理（线条渲染无需，保留占位）
     * @param light     光照（线条渲染无需，保留占位）
     */
    public static void renderCubeOutlineByVertex(PoseStack.Pose pose, VertexConsumer consumer,
                                         float length, float width, float height,
                                         int r, int g, int b, int a,
                                         int overlay, int light) {
        // 局部顶点（角点为原点）
        Vec3[] vertices = new Vec3[8];
        vertices[0] = new Vec3(0, 0, 0); // 左下后
        vertices[1] = new Vec3(length, 0, 0); // 右下后
        vertices[2] = new Vec3(length, 0, width); // 右下前
        vertices[3] = new Vec3(0, 0, width); // 左下前
        vertices[4] = new Vec3(0, height, 0); // 左上后
        vertices[5] = new Vec3(length, height, 0); // 右上后
        vertices[6] = new Vec3(length, height, width); // 右上前
        vertices[7] = new Vec3(0, height, width); // 左上前
        // 绘制12条边（法线参数仅占位）
        RenderUtils.renderLine(pose, consumer, vertices[0], vertices[1], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[1], vertices[2], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[2], vertices[3], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[3], vertices[0], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[4], vertices[5], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[5], vertices[6], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[6], vertices[7], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[7], vertices[4], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[0], vertices[4], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[1], vertices[5], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[2], vertices[6], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[3], vertices[7], 0, 0, 1, r, g, b, a);
    }


    /**
     * 渲染立方体轮廓，通过端面的底部中心点向着局部z轴渲染
     */
    public static void renderCubeOutline(PoseStack.Pose pose, VertexConsumer consumer,
                                                 float length, float width, float height,
                                                 int r, int g, int b, int a,
                                                 int overlay, int light) {
        float halfW = width * 0.5f;
        // 顶点定义：原点在端面中心底部，Z正向为长度方向，X轴对称，Y向上
        Vec3[] vertices = new Vec3[8];
        vertices[0] = new Vec3(-halfW, 0, 0);        // 左下后 (Z=0)
        vertices[1] = new Vec3( halfW, 0, 0);        // 右下后
        vertices[2] = new Vec3( halfW, 0, length);   // 右下前 (Z=length)
        vertices[3] = new Vec3(-halfW, 0, length);   // 左下前
        vertices[4] = new Vec3(-halfW, height, 0);   // 左上后
        vertices[5] = new Vec3( halfW, height, 0);   // 右上后
        vertices[6] = new Vec3( halfW, height, length); // 右上前
        vertices[7] = new Vec3(-halfW, height, length); // 左上前

        // 底部四边形（法线向上）
        RenderUtils.renderLine(pose, consumer, vertices[0], vertices[1], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[1], vertices[2], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[2], vertices[3], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[3], vertices[0], 0, 1, 0, r, g, b, a);

        // 顶部四边形
        RenderUtils.renderLine(pose, consumer, vertices[4], vertices[5], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[5], vertices[6], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[6], vertices[7], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[7], vertices[4], 0, 1, 0, r, g, b, a);

        // 垂直边（法线方向任意）
        RenderUtils.renderLine(pose, consumer, vertices[0], vertices[4], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[1], vertices[5], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[2], vertices[6], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[3], vertices[7], 0, 0, 1, r, g, b, a);

    }

    /**
     * 画侧面，四边形，需要四个点，一个固定了z轴的点中心点，再固定其中一个轴，加减另一个轴的1/2宽度或高度，
     * 即可得到在z轴这个面上两个相邻的点，正好可以通过实体的起点和攻击终点两个点的中心点，来确定四个相邻的点，
     * 再按照顺时针或逆时针的顺序即可画出一个侧面的四边形
     * 固定第一个轴
     */
    public static void drawSide(Vec3 from, Vec3 to, float width, float height, PoseStack.Pose pose,
                                VertexConsumer consumer, int r, int g, int b, int a, float v, int overlay, int light) {
        float halfWidth = width * 0.5f, halfHeight = height * 0.5f;
        //顶面
        drawXZOrYZQuad(from.add(0, halfHeight, 0), to.add(0, halfHeight, 0),
                halfWidth, 0, pose, consumer, r, g, b, a, v, overlay, light, 0, 1, 0);
        //底面
        drawXZOrYZQuad(from.subtract(0, halfHeight, 0), to.subtract(0, halfHeight, 0),
                halfWidth, 0, pose, consumer, r, g, b, a, v, overlay, light, 0, -1, 0);
        //左面
        drawXZOrYZQuad(from.add(halfWidth, 0, 0), to.add(halfWidth, 0, 0),
                0, halfHeight, pose, consumer, r, g, b, a, v, overlay, light, -1, 0, 0);
        //右面
        drawXZOrYZQuad(from.subtract(halfWidth, 0, 0), to.subtract(halfWidth, 0, 0),
                0, halfHeight, pose, consumer, r, g, b, a, v, overlay, light, 1, 0, 0);
    }

    /**
     * 正交
     */
    public static void renderBillboardQuad(Vec3 from, Vec3 to, float halfWidth, float halfHeight, PoseStack.Pose pose, VertexConsumer consumer, int r, int g, int b, int a, float v, int overlay, int packedLight) {
        drawXZOrYZQuad(from, to, halfWidth, 0, pose, consumer, r, g, b, a, v, overlay, packedLight, 0, 1, 0);
        drawXZOrYZQuad(from, to, 0, halfHeight, pose, consumer, r, g, b, a, v, overlay, packedLight, 1, 0, 0);
    }

    /**
     * 根据起点和终点坐标向局部z轴画xz或yz四边形，逆时针，左下，右下，右上，左上
     *
     * @param from       起点
     * @param to         终点
     * @param halfWidth  半宽
     * @param halfHeight 半高
     * @param pose       当前矩阵
     * @param consumer   rgba：颜色；uv：UV坐标；overlay（uv1）：覆盖层；light（uv2）：光照层；normal：法线
     */
    public static void drawXZOrYZQuad(Vec3 from, Vec3 to, float halfWidth, float halfHeight, PoseStack.Pose pose,
                                      VertexConsumer consumer, int r, int g, int b, int a, float v, int overlay, int light,
                                      float normalX, float normalY, float normalZ) {
        // form  两个方法确定两个点，只要其中一个轴的位置固定，例：from.x + halfWidth 或from.x - halfWidth固定在(-1,1,-1)
        // 即form.x = -1,halfWidth = 0,z = -1，那么变换通过y轴即可得到 (-1,1,-1) 和 (-1,-1,-1)
        // to 同理， 在form的x和y的条件下，z = 1，那么得到(-1,-1,1) 和 (-1,1,1)，逆时针的四个点确定一个侧面
        drawQuad(pose, consumer,
                from.add(halfWidth, halfHeight, 0),
                from.add(-halfWidth, -halfHeight, 0),
                to.add(-halfWidth, -halfHeight, 0),
                to.add(halfWidth, halfHeight, 0),
                normalX, normalY, normalZ,
                0, 0, v, v,
                r, g, b, a, overlay, light);
    }

    /**
     * 前后面（端面）绘制
     */
    public static void drawXYQuad(Vec3 center, float width, float height, PoseStack.Pose pose,
                                  VertexConsumer consumer, int r, int g, int b, int a, float v, int overlay, int light,
                                  float normalX, float normalY, float normalZ) {
        float halfWidth = width * 0.5f, halfHeight = height * 0.5f;
        drawQuad(pose, consumer,
                center.add((float) (center.x + halfWidth), (float) (center.y + halfHeight), (float) center.z),
                center.add((float) (center.x - halfWidth), (float) (center.y + halfHeight), (float) center.z),
                center.add((float) (center.x - halfWidth), (float) (center.y - halfHeight), (float) center.z),
                center.add((float) (center.x + halfWidth), (float) (center.y - halfHeight), (float) center.z),
                normalX, normalY, normalZ,
                0, 0, v, v,
                r, g, b, a, overlay, light);
    }


    /**
     * 通用四边形绘制方法 - 按四个顶点绘制（逆时针顺序）
     *
     * @param p1       第一个顶点（左下）
     * @param p2       第二个顶点（右下）
     * @param p3       第三个顶点（右上）
     * @param p4       第四个顶点（左上）
     * @param pose     当前矩阵
     * @param consumer 顶点消费者
     * @param r        红色 (0-255)
     * @param g        绿色 (0-255)
     * @param b        蓝色 (0-255)
     * @param a        透明度 (0-255)
     * @param uMin     左下U坐标
     * @param vMin     左下下V坐标
     * @param uMax     右上U坐标
     * @param vMax     左上V坐标
     * @param overlay  覆盖层
     * @param light    光照值
     * @param normalX  法线X
     * @param normalY  法线Y
     * @param normalZ  法线Z
     */
    public static void drawQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                                float normalX, float normalY, float normalZ,
                                float uMin, float vMin, float uMax, float vMax,
                                int r, int g, int b, int a, int overlay, int light) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(uMin, vMin)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(uMin, vMax)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        consumer.addVertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(uMax, vMax)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        consumer.addVertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(uMax, vMin)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
    }

    /**
     * 胶囊体 竖向
     * @param poseStack 栈姿
     * @param consumer 顶点消费者
     * @param length 中间圆柱的轴线长度
     * @param radius 两端半球和圆柱的半径
     * @param segments 分段数量
     */
    public static void renderCapsule(PoseStack poseStack, VertexConsumer consumer,float radius,float length,  byte segments,
                                      int r, int g, int b, int a, int overlay, int light) {
        PoseStack.Pose pose = poseStack.last();
        // 首端球
        RenderUtils.renderSphere(pose,consumer, radius, segments,r, g, b, a, overlay, light);
        // 圆柱
        RenderUtils.renderCylinder(pose, consumer, radius, length, segments,r,g,b,a,overlay, light);
        // 尾端球
        poseStack.translate(0,length,0);
        RenderUtils.renderSphere(pose,consumer, radius, segments,r, g, b, a,overlay, light);
    }
    /**
     * 圆柱体 竖向 轮廓
     */
    public static void renderCylinderOutline(PoseStack.Pose pose, VertexConsumer consumer, float radius, float height, int segments,
                                      int r, int g, int b, int a, int overlay, int light) {
        float step = Mth.TWO_PI / segments;
        float radian1 = 0, radian2 = step;
        for (int i = 0; i < segments; i++, radian1 += step, radian2 += step) {
            Vec3 upPoint1 = new Vec3(radius * (float) Math.cos(radian1), 0, radius * (float) Math.sin(radian1));
            Vec3 upPoint2 = new Vec3(radius * (float) Math.cos(radian2), 0, radius * (float) Math.sin(radian2));
            Vec3 downPoint1 = new Vec3(radius * (float) Math.cos(radian1), height, radius * (float) Math.sin(radian1));
            Vec3 downPoint2 = new Vec3(radius * (float) Math.cos(radian2), height, radius * (float) Math.sin(radian2));
            // 法线（从圆柱中心向外）
            Vec3 normal = upPoint1.normalize();
            renderLine(pose,consumer,upPoint1,upPoint2,(float) normal.x, (float) normal.y, (float) normal.z,r,g,b,a);
            renderLine(pose,consumer,upPoint1,downPoint1,(float) normal.x, (float) normal.y, (float) normal.z,r,g,b,a);
            renderLine(pose,consumer,downPoint1,downPoint2,(float) normal.x, (float) normal.y, (float) normal.z,r,g,b,a);
        }
    }
    /**
     * 圆柱体 竖向
     */
    public static void renderCylinder(PoseStack.Pose pose, VertexConsumer consumer, float radius, float height, int segments,
                                      int r, int g, int b, int a, int overlay, int light) {
        float step = Mth.TWO_PI / segments;
        float radian1 = 0, radian2 = step;
        for (int i = 0; i < segments; i++, radian1 += step, radian2 += step) {
            Vec3 upPoint1 = new Vec3(radius * (float) Math.cos(radian1), 0, radius * (float) Math.sin(radian1));
            Vec3 upPoint2 = new Vec3(radius * (float) Math.cos(radian2), 0, radius * (float) Math.sin(radian2));
            Vec3 downPoint1 = new Vec3(radius * (float) Math.cos(radian1), height, radius * (float) Math.sin(radian1));
            Vec3 downPoint2 = new Vec3(radius * (float) Math.cos(radian2), height, radius * (float) Math.sin(radian2));
            // 法线（从圆柱中心向外）
            Vec3 normal = upPoint1.normalize();
            // 关键修正：正确的逆时针顺序
            drawQuad(pose, consumer, upPoint1, downPoint1, downPoint2, upPoint2,
                    (float) normal.x, (float) normal.y, (float) normal.z,
                    0, 0, 1, 1,  // 对应的UV
                    r, g, b, a, overlay, light);
        }
        // 底面
        drawCircle(pose, consumer,
                new Vec3(0, 0, 0), radius, segments,
                new Vec3(0, -1, 0),
                r, g, b, a, overlay, light);
        // 顶面
        drawCircle(pose, consumer,
                new Vec3(0, height, 0), radius, segments,
                new Vec3(0, 1, 0),
                r, g, b, a, overlay, light);
    }
    /**
     * 球体
     * @param segments 水平段数，即经线，经度，Theta，θ
     */
    public static void renderSphere(PoseStack.Pose pose, VertexConsumer consumer, float radius, byte segments,
                                    int r, int g, int b, int a, int overlay, int light) {
        int latSegments = segments / 2; // 垂直段数，纬线，维度，Phi，Φ
        // 预计算角度增量
        float deltaTheta = Mth.TWO_PI / segments;
        float deltaPhi = Mth.PI / latSegments;

        for (int i = 0; i < latSegments; i++) {
            // 当前层的极角 φ（垂直角度）
            float phi1 = i * deltaPhi - Mth.HALF_PI;      // 上层纬度：从 -π/2 开始
            float phi2 = (i + 1) * deltaPhi - Mth.HALF_PI; // 下层纬度

            // 计算纬度的sin/cos
            float sinPhi1 = Mth.sin(phi1), cosPhi1 = Mth.cos(phi1);
            float sinPhi2 = Mth.sin(phi2), cosPhi2 = Mth.cos(phi2);

            // 当前层平面半径（在xy平面的投影半径）
            float r1 = radius * cosPhi1;
            float r2 = radius * cosPhi2;

            // UV的V坐标（垂直方向）
            float v1 = (float) i / latSegments;      // 上层V坐标
            float v2 = (float) (i + 1) / latSegments; // 下层V坐标

            // 遍历水平方向（经度，一圈）
            for (int j = 0; j < segments; j++) {
                // 当前经度角度 θ（水平角度）
                float theta1 = j * deltaTheta;      // 左边界经度
                float theta2 = (j + 1) * deltaTheta; // 右边界经度

                // 计算经度的sin/cos
                float sinTheta1 = Mth.sin(theta1);
                float cosTheta1 = Mth.cos(theta1);
                float sinTheta2 = Mth.sin(theta2);
                float cosTheta2 = Mth.cos(theta2);

                // UV的U坐标（水平方向）
                float u1 = (float) j / segments;      // 左边界U坐标
                float u2 = (float) (j + 1) / segments; // 右边界U坐标

                // 计算四个顶点的坐标
                // 注意：顶点顺序要逆时针，面向球体外部时
                // 调整后顺序：左上 → 左下 → 右下 → 右上

                // p1: 左上（上层左边界）
                float x1 = r1 * cosTheta1;
                float y1 = radius * sinPhi1;
                float z1 = r1 * sinTheta1;

                // p2: 左下（下层左边界）
                float x2 = r2 * cosTheta1;
                float y2 = radius * sinPhi2;
                float z2 = r2 * sinTheta1;

                // p3: 右下（下层右边界）
                float x3 = r2 * cosTheta2;
                float y3 = radius * sinPhi2;
                float z3 = r2 * sinTheta2;

                // p4: 右上（上层右边界）
                float x4 = r1 * cosTheta2;
                float y4 = radius * sinPhi1;
                float z4 = r1 * sinTheta2;

                // 对应的法线（保持与顶点一致）
                // n1: p1的法线（左上）
                float nx1 = cosPhi1 * cosTheta1;
                float nz1 = cosPhi1 * sinTheta1;

                // n2: p2的法线（左下）
                float nx2 = cosPhi2 * cosTheta1;
                float nz2 = cosPhi2 * sinTheta1;

                // n3: p3的法线（右下）
                float nx3 = cosPhi2 * cosTheta2;
                float nz3 = cosPhi2 * sinTheta2;

                // n4: p4的法线（右上）
                float nx4 = cosPhi1 * cosTheta2;
                float nz4 = cosPhi1 * sinTheta2;

                // 绘制四边形
                Matrix4f matrix = pose.pose();
                consumer.addVertex(matrix, x1, y1, z1)
                        .setNormal(pose, nx1, sinPhi1, nz1)
                        .setUv(u1, v1)
                        .setColor(r, g, b, a)
                        .setOverlay(overlay)
                        .setLight(light);
                consumer.addVertex(matrix, x2, y2, z2)
                        .setNormal(pose, nx2, sinPhi2, nz2)
                        .setUv(u1, v2)
                        .setColor(r, g, b, a)
                        .setOverlay(overlay)
                        .setLight(light);
                consumer.addVertex(matrix, x3, y3, z3)
                        .setNormal(pose, nx3, sinPhi2, nz3)
                        .setUv(u2, v2)
                        .setColor(r, g, b, a)
                        .setOverlay(overlay)
                        .setLight(light);
                consumer.addVertex(matrix, x4, y4, z4)
                        .setNormal(pose, nx4, sinPhi1, nz4)
                        .setUv(u2, v1)
                        .setColor(r, g, b, a)
                        .setOverlay(overlay)
                        .setLight(light);
            }
        }
    }


    /**
     * 画圆形 垂直Y轴，XZ平面
     */
    public static void drawCircle(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center, float radius, int segments, Vec3 normal,
                                  int r, int g, int b, int a, int overlay, int light) {
        float delta = Mth.TWO_PI / segments;
        float radian1 = 0, radian2 = delta;
        for (int i = 0; i < segments; i++, radian1 += delta, radian2 += delta) {
            Vec3 edge1 = center.add(radius * (float) Math.cos(radian1),0 , radius * (float) Math.sin(radian1));
            Vec3 edge2 = center.add(radius * (float) Math.cos(radian2),0 , radius * (float) Math.sin(radian2));
            // 关键：center -> edge2 -> edge1 必须是逆时针
            drawTriangle(pose, consumer, center, edge2, edge1,
                    (float) normal.x, (float) normal.y, (float) normal.z,
                    0, 0, 0, 1, 1, 1,
                    r, g, b, a, overlay, light);
        }
    }


    /**
     * 绘制三角形
     */
    public static void drawTriangle(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p1, Vec3 p2, Vec3 p3,
                                    float normalX, float normalY, float normalZ,
                                    float u1, float v1, float u2, float v2, float u3, float v3,
                                    int r, int g, int b, int a,
                                    int overlay, int light) {
        Matrix4f matrix = pose.pose();
        // 顶点1
        consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(u1, v1)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        // 顶点2
        consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(u2, v2)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        // 顶点3
        consumer.addVertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(u3, v3)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
        // 由于MC的渲染引擎只支持四边形，所以需要再给一个点 顶点1
        consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .setNormal(pose, normalX, normalY, normalZ)
                .setUv(u1, v1)
                .setColor(r, g, b, a)
                .setOverlay(overlay)
                .setLight(light);
    }
    /**
     * 线段，只需两个点，但是渲染的类型得是Line
     */
    public static void renderLine(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p1, Vec3 p2,
                                    float normalX, float normalY, float normalZ,
                                    int r, int g, int b, int a) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setNormal(pose, normalX, normalY, normalZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).setNormal(pose, normalX, normalY, normalZ).setColor(r, g, b, a);
    }
    public static void renderLine(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p1, Vec3 p2,
                                  float normalX, float normalY, float normalZ,
                                  float r, float g, float b, float a) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setNormal(pose, normalX, normalY, normalZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).setNormal(pose, normalX, normalY, normalZ).setColor(r, g, b, a);
    }

//    /**
//     * 最简单的OBB渲染（线框）
//     */
//    public static void renderOBBWireframe(PoseStack poseStack, VertexConsumer consumer, OBB obb,
//                                          int r, int g, int b, int a, int packedLight) {
//        // 获取新的pose
//        PoseStack.Pose pose = poseStack.last();
//        // 获取OBB尺寸
//        float width = obb.getWidth();
//        float length = obb.getLength();
//        float height = obb.getHeight();
//        // 应用变换
//        poseStack.pushPose();
//
//        // 移动到中心 - 修正：去掉负号
//        Vec3 center = obb.getCenter();
//        poseStack.translate(center.x, center.y, center.z);  // 修正这里
//        // 应用旋转
//        float yaw = obb.getYaw();
//        float pitch = obb.getPitch();
//        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
//        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
//
//        // 直接渲染立方体线框
//        renderOBBOutline(pose, consumer, width, length, height, r, g, b, a, packedLight);
//        poseStack.popPose();
//    }

    /**
     * 渲染OBB（实体）
     */
    public static void renderOBBSolid(PoseStack poseStack, VertexConsumer consumer, OBB obb,
                                      int r, int g, int b, int a, int packedLight) {
        PoseStack.Pose pose = poseStack.last();

        // 获取OBB尺寸
        float length = obb.getLength();
        float height = obb.getHeight();
        float width = obb.getWidth();

        // 应用变换
        poseStack.pushPose();

        // 移动到中心 - 这里正确，保持不变
        Vec3 center = obb.getCenter();
        poseStack.translate(center.x, center.y, center.z);

        // 应用旋转
        float yaw = obb.getYaw();
        float pitch = obb.getPitch();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // 获取新的pose
        pose = poseStack.last();

        // 直接渲染立方体
        RenderUtils.renderCube( pose,consumer, length, width, height, r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);

        poseStack.popPose();
    }

    public static void renderOBBOutline(PoseStack.Pose pose, VertexConsumer consumer, OBB obb,int r, int g, int b, int a) {
        // 移动到中心 - 这里正确，保持不变
        Vec3[] vertices = obb.getVertices();
        // 底部四边形（使用(0,1,0)作为法线）
        RenderUtils.renderLine(pose, consumer, vertices[0], vertices[1], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[1], vertices[3], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[3], vertices[2], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[2], vertices[0], 0, 1, 0, r, g, b, a);
        // 顶部四边形（使用(0,1,0)作为法线）
        RenderUtils.renderLine(pose, consumer, vertices[4], vertices[5], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[5], vertices[7], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[7], vertices[6], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[6], vertices[4], 0, 1, 0, r, g, b, a);
        // 垂直边（使用(0,0,1)作为法线）
        RenderUtils.renderLine(pose, consumer, vertices[0], vertices[4], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[1], vertices[5], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[2], vertices[6], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[3], vertices[7], 0, 0, 1, r, g, b, a);
    }
    public static void renderOBBOutline(PoseStack.Pose pose, VertexConsumer consumer, OBB obb,float r, float g, float b, float a) {
        // 移动到中心 - 这里正确，保持不变
        Vec3[] vertices = obb.getVertices();
        // 底部四边形（使用(0,1,0)作为法线）
        RenderUtils.renderLine(pose, consumer, vertices[0], vertices[1], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[1], vertices[3], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[3], vertices[2], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[2], vertices[0], 0, 1, 0, r, g, b, a);
        // 顶部四边形（使用(0,1,0)作为法线）
        RenderUtils.renderLine(pose, consumer, vertices[4], vertices[5], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[5], vertices[7], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[7], vertices[6], 0, 1, 0, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[6], vertices[4], 0, 1, 0, r, g, b, a);
        // 垂直边（使用(0,0,1)作为法线）
        RenderUtils.renderLine(pose, consumer, vertices[0], vertices[4], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[1], vertices[5], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[2], vertices[6], 0, 0, 1, r, g, b, a);
        RenderUtils.renderLine(pose, consumer, vertices[3], vertices[7], 0, 0, 1, r, g, b, a);
    }

}
