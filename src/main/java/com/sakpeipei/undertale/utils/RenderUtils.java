package com.sakpeipei.undertale.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class RenderUtils {
    /**
     * 简易正方体
     */
    public static void renderCube(VertexConsumer builder, float size, PoseStack.Pose pose, int r, int g, int b, int a, int overlay, int light) {
        renderCube(builder, size, size, size, pose, r, g, b, a, overlay, light);
    }

    /**
     * 简易立方体
     */
    public static void renderCube(VertexConsumer builder, float length, float width, float height, PoseStack.Pose pose, int r, int g, int b, int a, int overlay, int light) {
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
     * 修正的圆柱体渲染（正确的逆时针顺序）
     */
    public static void renderCylinder(PoseStack.Pose pose, VertexConsumer consumer, float radius, float height, int segments,
                                      int r, int g, int b, int overlay, int light) {
        float halfHeight = height * 0.5f;
        System.out.println("\n=== CYLINDER CCW RENDER ===");
        float step = Mth.TWO_PI / segments;
        float radian1 = 0, radian2 = step;
        for (int i = 0; i < segments; i++, radian1 += step, radian2 += step) {
            Vec3 frontEdge1 = new Vec3(radius * (float) Math.cos(radian1), radius * (float) Math.sin(radian1), -halfHeight);
            Vec3 frontEdge2 = new Vec3(radius * (float) Math.cos(radian2), radius * (float) Math.sin(radian2), -halfHeight);
            Vec3 backEdge1 = new Vec3(radius * (float) Math.cos(radian1), radius * (float) Math.sin(radian1), -halfHeight);
            Vec3 backEdge2 = new Vec3(radius * (float) Math.cos(radian2), radius * (float) Math.sin(radian2), halfHeight);

            // 法线（从圆柱中心向外）
            Vec3 normal = frontEdge1.normalize();

            // 关键修正：正确的逆时针顺序
            drawQuad(pose, consumer, frontEdge1, backEdge1, backEdge2, frontEdge2,
                    (float) normal.x, (float) normal.y, (float) normal.z,
                    0, 0, 1, 1,  // 对应的UV
                    r, g, b, 255, overlay, light);
        }

        // 前面端面 （确保逆时针顶点顺序）
        drawCircle(pose, consumer,
                new Vec3(0, 0, -halfHeight), radius, segments,
                new Vec3(0, 0, -1),
                255, 255, 0, 255, overlay, light);
        // 后面端面 （确保逆时针顶点顺序）
        drawCircle(pose, consumer,
                new Vec3(0, 0, halfHeight), radius, segments,
                new Vec3(0, 0, 1),
                0, 255, 255, 255, overlay, light);
    }

    /**
     * 球体渲染
     *
     * @param segments 水平段数，即经线，经度，Theta，θ
     */
    public static void renderSphere(PoseStack.Pose pose, VertexConsumer consumer, float radius, int segments,
                                    int r, int g, int b, int a, int overlay, int light) {
        segments = Math.max(segments, 32);
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

                // 直接绘制两个三角形
                Matrix4f matrix = pose.pose();
                // 三角形1: p1(左上) → p2(左下) → p3(右下)
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


                // 三角形2: p1(左上) → p3(右下) → p4(右上)
                consumer.addVertex(matrix, x1, y1, z1)
                        .setNormal(pose, nx1, sinPhi1, nz1)
                        .setUv(u1, v1)
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
     * 画圆形
     */
    public static void drawCircle(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center, float radius, int segments, Vec3 normal,
                                  int r, int g, int b, int a, int overlay, int light) {
        float delta = Mth.TWO_PI / segments;
        float radian1 = 0, radian2 = delta;
        for (int i = 0; i < segments; i++, radian1 += delta, radian2 += delta) {
            Vec3 edge1 = center.add(radius * (float) Math.cos(radian1), radius * (float) Math.sin(radian1), 0);
            Vec3 edge2 = center.add(radius * (float) Math.cos(radian2), radius * (float) Math.sin(radian2), 0);
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
    }
}
