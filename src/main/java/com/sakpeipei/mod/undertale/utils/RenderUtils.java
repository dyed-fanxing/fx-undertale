package com.sakpeipei.mod.undertale.utils;

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
    public static void renderCube(VertexConsumer builder,float size, PoseStack.Pose pose, int r, int g, int b, int a,int overlay, int light) {
        renderCube(builder,size,size,size,pose,r,g,b,a,overlay,light);
    }
    /**
     * 简易立方体
     */
    public static void renderCube(VertexConsumer builder, float length, float width, float height, PoseStack.Pose pose, int r, int g, int b, int a, int overlay, int light) {
        Matrix4f matrix = pose.pose();
        length = length * 0.5f; width = width * 0.5f; height = height * 0.5f;
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
        float halfWidth = width*0.5f,halfHeight = height*0.5f;
        //顶面
        drawXZOrYZQuad(from.add(0,halfHeight,0),to.add(0,halfHeight,0),
                halfWidth,0,pose,consumer,r,g,b,a,v,overlay,light,0,1,0);
        //底面
        drawXZOrYZQuad(from.subtract(0, halfHeight, 0 ),to.subtract(0, halfHeight, 0 ),
                halfWidth,0,pose,consumer,r,g,b,a,v,overlay,light,0,-1,0);
        //左面
        drawXZOrYZQuad(from.add(halfWidth,0,0),to.add(halfWidth,0,0),
                0,halfHeight,pose,consumer,r,g,b,a,v,overlay,light,-1,0,0);
        //右面
        drawXZOrYZQuad(from.subtract(halfWidth,0,0),to.subtract(halfWidth,0,0),
                0,halfHeight,pose,consumer,r,g,b,a,v,overlay,light,1,0,0);
    }

    /**
     * 正交
     */
    public static void renderBillboardQuad(Vec3 from, Vec3 to,float halfWidth, float halfHeight,PoseStack.Pose pose,VertexConsumer consumer, int r, int g, int b, int a,float v,int overlay, int packedLight) {
        drawXZOrYZQuad(from,to,halfWidth,0,pose,consumer,r,g,b,a,v,overlay,packedLight,0,1,0);
        drawXZOrYZQuad(from,to,0,halfHeight,pose,consumer,r,g,b,a,v,overlay,packedLight,1,0,0);
    }

    /**
     * 根据起点和终点坐标向局部z轴画xz或yz四边形，逆时针，左下，右下，右上，左上
     * @param from 起点
     * @param to 终点
     * @param halfWidth 半宽
     * @param halfHeight 半高
     * @param pose 当前矩阵
     * @param consumer
     * rgba：颜色；uv：UV坐标；overlay（uv1）：覆盖层；light（uv2）：光照层；normal：法线
     */
    public static void drawXZOrYZQuad(Vec3 from,Vec3 to,float halfWidth,float halfHeight,PoseStack.Pose pose,
                                 VertexConsumer consumer, int r,int g,int b,int a,float v,int overlay,int light,
                                 float normalX,float normalY,float normalZ) {
        Matrix4f matrix = pose.pose();
        // 绘制四边形（逆时针顺序）
        // form  两个方法确定两个点，只要其中一个轴的位置固定，例：from.x + halfWidth 或from.x - halfWidth固定在(-1,1,-1)
        // 即form.x = -1,halfWidth = 0,z = -1，那么变换通过y轴即可得到 (-1,1,-1) 和 (-1,-1,-1)
        consumer.addVertex(matrix, (float)(from.x + halfWidth), (float)(from.y + halfHeight), (float) from.z)
                .setColor(r,g,b,a).setUv(0, 0).setOverlay(overlay).setLight(light)
                .setNormal(pose,normalX,normalY,normalZ);
        consumer.addVertex(matrix, (float)(from.x - halfWidth), (float)(from.y - halfHeight), (float) from.z)
                .setColor(r,g,b,a).setUv(1, 0).setOverlay(overlay).setLight(light)
                .setNormal(pose,normalX,normalY,normalZ);
        // to 同理， 在form的x和y的条件下，z = 1，那么得到(-1,-1,1) 和 (-1,1,1)，逆时针的四个点确定一个侧面
        consumer.addVertex(matrix, (float)(to.x - halfWidth), (float)(to.y - halfHeight), (float) to.z)
                .setColor(r,g,b,a).setUv(1, v).setOverlay(overlay).setLight(light)
                .setNormal(pose,normalX,normalY,normalZ);
        consumer.addVertex(matrix, (float)(to.x + halfWidth), (float)(to.y + halfHeight), (float) to.z)
                .setColor(r,g,b,a).setUv(0, v).setOverlay(overlay).setLight(light)
                .setNormal(pose,normalX,normalY,normalZ);
    }

    /**
     * 前后面（端面）绘制
     */
    public static void drawXYQuad(Vec3 center, float width, float height, PoseStack.Pose pose,
                                  VertexConsumer consumer, int r, int g, int b, int a,float v,int overlay, int light,
                                  float normalX,float normalY,float normalZ) {
        float halfWidth = width*0.5f,halfHeight = height*0.5f;
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, (float)(center.x + halfWidth), (float)(center.y + halfHeight), (float)center.z)
                .setColor(r, g, b, a).setUv(0f, 0f).setOverlay(overlay).setLight(light)
                .setNormal(pose,normalX,normalY,normalZ);
        consumer.addVertex(matrix, (float)(center.x - halfWidth), (float)(center.y + halfHeight), (float)center.z)
                .setColor(r, g, b, a).setUv(1f, 0f).setOverlay(overlay).setLight(light)
                .setNormal(pose,normalX,normalY,normalZ);
        consumer.addVertex(matrix, (float)(center.x - halfWidth), (float)(center.y - halfHeight), (float)center.z)
                .setColor(r, g, b, a).setUv(1f, v).setOverlay(overlay).setLight(light)
                .setNormal(pose,normalX,normalY,normalZ);
        consumer.addVertex(matrix, (float)(center.x + halfWidth), (float)(center.y - halfHeight), (float)center.z)
                .setColor(r, g, b, a).setUv(0f, v).setOverlay(overlay).setLight(light)
                .setNormal(pose,normalX,normalY,normalZ);

    }





    /**
     * 修正的圆柱体渲染（正确的逆时针顺序）
     */
    public static void renderCylinder(VertexConsumer builder, float radius, float height, int segments,
                                         PoseStack.Pose pose, int overlay, int light) {
        float halfHeight = height * 0.5f;
        float twoPi = (float) (2.0 * Math.PI);

        System.out.println("\n=== CYLINDER CCW RENDER ===");
        System.out.println("View from OUTSIDE the cylinder");
        float step = Mth.TWO_PI / segments;
        float radian1 = 0,radian2 = step;
        for (int i = 0; i < segments; i++,radian1 += step, radian2 += step) {
            Vec3 frontEdge1 = new Vec3(radius * (float) Math.cos(radian1),radius * (float) Math.sin(radian1),-halfHeight);
            Vec3 frontEdge2 = new Vec3(radius * (float) Math.cos(radian2),radius * (float) Math.sin(radian2),-halfHeight);
            Vec3 backEdge1 = new Vec3(radius * (float) Math.cos(radian1),radius * (float) Math.sin(radian1),-halfHeight);
            Vec3 backEdge2 = new Vec3(radius * (float) Math.cos(radian2),radius * (float) Math.sin(radian2),halfHeight);

            // 法线（从圆柱中心向外）
            Vec3 normal = frontEdge1.normalize();

            // 关键修正：正确的逆时针顺序
            drawQuad(frontEdge1, backEdge1, backEdge2, frontEdge2,  // v1 -> v4 -> v3 -> v2
                    0, 0, 1,1,  // 对应的UV
                    normal, pose, builder, r, g, b, 255, overlay, light);
        }

        // 前面端面
        drawCircleFace(
                new Vec3(0, 0, -halfHeight), radius, segments,
                new Vec3(0, 0, -1), pose, builder,
                255, 255, 0, 255, overlay, light);
        // 后面端面
        drawCircleFace(
                new Vec3(0, 0, halfHeight), radius, segments,
                new Vec3(0, 0, 1), pose, builder,
                0, 255, 255, 255, overlay, light);
    }
    /**
     * 修复的球体渲染（解决三角形缺失问题）
     */
    public static void renderSphere(VertexConsumer builder, float radius, int segments,
                                    PoseStack.Pose pose, int r, int g, int b, int a,
                                    int overlay, int light) {
        segments = Math.max(segments, 8);
        int stacks = Math.max(segments / 2, 8);

        float pi = (float) Math.PI;
        float twoPi = 2.0f * pi;

        // 预计算角度增量
        float deltaPhi = pi / stacks;
        float deltaTheta = twoPi / segments;

        // 预计算sin/cos值数组
        float[] sinPhi = new float[stacks + 1];
        float[] cosPhi = new float[stacks + 1];
        for (int i = 0; i <= stacks; i++) {
            float phi = i * deltaPhi - pi * 0.5f;
            sinPhi[i] = (float) Math.sin(phi);
            cosPhi[i] = (float) Math.cos(phi);
        }

        float[] sinTheta = new float[segments + 1];
        float[] cosTheta = new float[segments + 1];
        for (int j = 0; j <= segments; j++) {
            float theta = j * deltaTheta;
            sinTheta[j] = (float) Math.sin(theta);
            cosTheta[j] = (float) Math.cos(theta);
        }

        for (int i = 0; i < stacks; i++) {
            float y1 = radius * sinPhi[i];
            float y2 = radius * sinPhi[i + 1];

            float r1 = radius * cosPhi[i];
            float r2 = radius * cosPhi[i + 1];

            // UV的V坐标
            float vCoord1 = (float) i / stacks;
            float vCoord2 = (float) (i + 1) / stacks;

            for (int j = 0; j < segments; j++) {
                // UV的U坐标
                float u1 = (float) j / segments;
                float u2 = (float) (j + 1) / segments;

                // 计算顶点坐标
                float x1 = r1 * cosTheta[j];
                float z1 = r1 * sinTheta[j];

                float x2 = r1 * cosTheta[j + 1];
                float z2 = r1 * sinTheta[j + 1];

                float x3 = r2 * cosTheta[j + 1];
                float z3 = r2 * sinTheta[j + 1];

                float x4 = r2 * cosTheta[j];
                float z4 = r2 * sinTheta[j];

                // 创建顶点
                Vec3 v1 = new Vec3(x1, y1, z1);
                Vec3 v2 = new Vec3(x2, y1, z2);
                Vec3 v3 = new Vec3(x3, y2, z3);
                Vec3 v4 = new Vec3(x4, y2, z4);

                // 计算法线
                Vec3 normal1 = v1.normalize();

                // 绘制两个三角形（确保正确的顶点顺序）
                // 三角形1: v1 -> v2 -> v3 (逆时针)
                drawTriangle(v1, v2, v3,
                        u1, vCoord1, u2, vCoord1, u2, vCoord2,
                        normal1, pose, builder, r, g, b, a, overlay, light);

                // 三角形2: v1 -> v3 -> v4 (逆时针) - 这是关键修正！
                // 之前可能是 v1 -> v4 -> v3，导致顺时针顺序
                drawTriangle(v1, v3, v4,
                        u1, vCoord1, u2, vCoord2, u1, vCoord2,
                        normal1, pose, builder, r, g, b, a, overlay, light);
            }
        }
    }


    /**
     * 修复的圆形端面绘制（确保逆时针顶点顺序）
     */
    public static void drawCircleFace(Vec3 center, float radius, int segments,Vec3 normal,
                                      PoseStack.Pose pose, VertexConsumer consumer,
                                      int r, int g, int b, int a,
                                      int overlay, int light) {
        float step = Mth.TWO_PI / segments;
        float radian1 = 0,radian2 = step;
        for (int i = 0; i < segments; i++,radian1 += step, radian2 += step) {
            Vec3 edge1 = center.add(radius * (float) Math.cos(radian1),radius * (float) Math.sin(radian1),0);
            Vec3 edge2 = center.add(radius * (float) Math.cos(radian2),radius * (float) Math.sin(radian2),0);
            // 关键：center -> edge2 -> edge1 必须是逆时针
            drawTriangle(center, edge2, edge1,
                    0, 0,0,1,1,1,
                    normal, pose, consumer, r, g, b, a, overlay, light);
        }
    }


    /**
     * 绘制三角形（确保正确的顶点顺序 - 逆时针）
     */
    public static void drawTriangle(Vec3 v1, Vec3 v2, Vec3 v3,
                                    float u1, float v1y, float u2, float v2y, float u3, float v3y,
                                    Vec3 normal,
                                    PoseStack.Pose pose, VertexConsumer consumer,
                                    int r, int g, int b, int a,
                                    int overlay, int light) {
        Matrix4f matrix = pose.pose();
        // 顶点1
        consumer.addVertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z)
                .setColor(r, g, b, a)
                .setUv(u1, v1y)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);

        // 顶点2
        consumer.addVertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z)
                .setColor(r, g, b, a)
                .setUv(u2, v2y)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);

        // 顶点3
        consumer.addVertex(matrix, (float) v3.x, (float) v3.y, (float) v3.z)
                .setColor(r, g, b, a)
                .setUv(u3, v3y)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }


    /**
     * 调试版圆柱体渲染（交替颜色显示，检查哪些三角形缺失）
     */
    public static void renderCylinderDebug(VertexConsumer builder, float radius, float height, int segments,
                                           PoseStack.Pose pose, int overlay, int light) {
        float halfHeight = height * 0.5f;
        float twoPi = (float) (2.0 * Math.PI);

        System.out.println("=== CYLINDER DEBUG RENDER ===");
        System.out.println("radius=" + radius + ", height=" + height + ", segments=" + segments);

        // 1. 渲染侧面 - 使用交替颜色
        for (int i = 0; i < segments; i++) {
            float angle1 = twoPi * i / segments;
            float angle2 = twoPi * (i + 1) / segments;

            float x1 = radius * (float) Math.cos(angle1);
            float y1 = radius * (float) Math.sin(angle1);
            float x2 = radius * (float) Math.cos(angle2);
            float y2 = radius * (float) Math.sin(angle2);

            // 顶点
            Vec3 frontBottomLeft = new Vec3(x1, y1, -halfHeight);
            Vec3 frontBottomRight = new Vec3(x2, y2, -halfHeight);
            Vec3 backTopRight = new Vec3(x2, y2, halfHeight);
            Vec3 backTopLeft = new Vec3(x1, y1, halfHeight);

            // 法线
            Vec3 normal = new Vec3(x1, y1, 0).normalize();

            // UV坐标
            float u1 = (float) i / segments;
            float u2 = (float) (i + 1) / segments;

            // 交替颜色：奇数为红色，偶数为绿色
            int r, g, b;
            if (i % 2 == 0) {
                r = 255; g = 0; b = 0;  // 红色
                System.out.println("Segment " + i + ": RED");
            } else {
                r = 0; g = 255; b = 0;  // 绿色
                System.out.println("Segment " + i + ": GREEN");
            }

            // 绘制侧面四边形
            drawQuad(frontBottomLeft, frontBottomRight, backTopRight, backTopLeft,
                    u1, 1.0f, u2, 1.0f, u2, 0.0f, u1, 0.0f,
                    normal, pose, builder, r, g, b, 255, overlay, light);
        }

        // 2. 渲染前后面 - 也使用交替颜色
        System.out.println("\nRendering end caps...");

        // 前面端面：三角形交替颜色
        drawCircleFaceDebug(
                new Vec3(0, 0, -halfHeight), radius, segments,
                new Vec3(0, 0, -1), pose, builder, overlay, light, "FRONT");

        // 后面端面：三角形交替颜色
        drawCircleFaceDebug(
                new Vec3(0, 0, halfHeight), radius, segments,
                new Vec3(0, 0, 1), pose, builder, overlay, light, "BACK");
    }

    /**
     * 调试版圆形端面绘制（交替颜色）
     */
    public static void drawCircleFaceDebug(Vec3 center, float radius, int segments,
                                           Vec3 normal, PoseStack.Pose pose,
                                           VertexConsumer consumer, int overlay, int light,
                                           String capName) {
        System.out.println("\nDrawing " + capName + " cap: " + segments + " triangles");

        // 找到垂直向量
        Vec3 up, right;
        if (Math.abs(normal.y) > 0.9) {
            right = new Vec3(1, 0, 0);
        } else {
            right = new Vec3(0, 1, 0);
        }

        up = normal.cross(right).normalize();
        right = up.cross(normal).normalize();

        float twoPi = (float) (2.0 * Math.PI);

        for (int i = 0; i < segments; i++) {
            float angle1 = twoPi * i / segments;
            float angle2 = twoPi * (i + 1) / segments;

            Vec3 edge1 = center
                    .add(right.scale(radius * (float) Math.cos(angle1)))
                    .add(up.scale(radius * (float) Math.sin(angle1)));

            Vec3 edge2 = center
                    .add(right.scale(radius * (float) Math.cos(angle2)))
                    .add(up.scale(radius * (float) Math.sin(angle2)));

            // 交替颜色
            int r, g, b;
            if (i % 2 == 0) {
                r = 255; g = 255; b = 0;  // 黄色
                System.out.println("  Triangle " + i + ": YELLOW");
            } else {
                r = 0; g = 255; b = 255;  // 青色
                System.out.println("  Triangle " + i + ": CYAN");
            }

            drawTriangle(center, edge1, edge2,
                    0.5f, 0.5f,
                    (float)(0.5 + 0.5 * Math.cos(angle1)),
                    (float)(0.5 + 0.5 * Math.sin(angle1)),
                    (float)(0.5 + 0.5 * Math.cos(angle2)),
                    (float)(0.5 + 0.5 * Math.sin(angle2)),
                    normal, pose, consumer, r, g, b, 255, overlay, light);
        }
    }
}
