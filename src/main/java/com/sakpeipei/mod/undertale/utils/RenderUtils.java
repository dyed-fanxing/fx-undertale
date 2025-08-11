package com.sakpeipei.mod.undertale.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
     * 简易立方体，正方体
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
     *
     * 固定第一个轴
     */
    public static void buildSide(Vec3 from, Vec3 to, float width, float height, PoseStack.Pose pose,
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
}
