package com.sakpeipei.undertale.client.render.entity.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Config;
import com.sakpeipei.undertale.common.RenderTypes;
import com.sakpeipei.undertale.common.ResourceLocations;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import com.sakpeipei.undertale.utils.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.openjdk.nashorn.internal.ir.CallNode;

import java.util.Random;

public class GasterBlasterBeamRenderer {
    private static final RenderType BEAM_NO_TRANSPARENCY = RenderTypes.ENTITY_BEAM_NO_CULL.apply(ResourceLocations.WHITE_TEXTURE,false);
    private static final RenderType BEAM_ENERGY_SWIRL = RenderType.energySwirl(ResourceLocations.BEAM_FLOW_TEXTURE,0,0);
    private static final RenderType RAY_ENERGY_SWIRL = RenderType.energySwirl(ResourceLocations.WHITE_TEXTURE,0,0);
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

    /**
     * 渲染 GB
     * @param partialTick 部分刻，客户端插值
     */
    public static  void render(GasterBlaster animatable, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight,int[][] color){
        poseStack.pushPose(); // 在这里压栈
        float size = animatable.getSize();
        float radius = size*0.5f;
        int segments = Config.COMMON.segments.getAsInt();
        float partialSize = 0f;
        int fireTick = animatable.getFireTick();
        int shotTick = animatable.getShotTick();
        int decayTick = animatable.getDecayTick();
        int discardTick = decayTick + 3;
        poseStack.translate(0,animatable.getEyeHeight(),0);
        float offset = (animatable.tickCount + partialTick)*0.3f;
        // 在 render 方法中获取相机
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        if(animatable.tickCount < fireTick){
            partialSize = Mth.lerp((partialTick + animatable.tickCount) / fireTick, 0, radius*0.75f);
            poseStack.pushPose();
            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_NO_TRANSPARENCY), partialSize*0.5f, segments, color[0][0], color[0][1], color[0][2], color[0][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_ENERGY_SWIRL), partialSize, segments, color[1][0], color[1][1], color[1][2], color[1][3], OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);

// ========== 最终修正：面向相机的椭圆光线（端点收缩，四边形模式，内圈小半径） ==========
            int rayCount = 30;                                  // 光线数量
            float outerRadius = radius * 3.0f;                   // 外端点起始距离
            float progress = (animatable.tickCount + partialTick) / fireTick; // 0~1
            float shortAxis = radius * 0.12f;                     // 椭圆短轴（细长）
            int ellipseSegments = 16;                             // 椭圆分段数（16个四边形）

// 生成固定随机方向和延迟（基于实体UUID）
            long seed = animatable.getUUID().getMostSignificantBits() ^ animatable.getUUID().getLeastSignificantBits();
            Random random = new Random(seed);
            Vec3[] directions = new Vec3[rayCount];
            float[] delays = new float[rayCount];
            for (int i = 0; i < rayCount; i++) {
                double theta = random.nextDouble() * 2 * Math.PI;
                double phi = Math.acos(2 * random.nextDouble() - 1) - Math.PI/2;
                directions[i] = new Vec3(
                        Math.cos(theta) * Math.cos(phi),
                        Math.sin(phi),
                        Math.sin(theta) * Math.cos(phi)
                ).normalize();
                delays[i] = random.nextFloat() * 0.7f;            // 随机延迟 0~0.7
            }

            for (int i = 0; i < rayCount; i++) {
                Vec3 dir = directions[i];
                float rayProgress = (progress - delays[i]) / (1 - delays[i]);
                if (rayProgress <= 0 || rayProgress > 1) continue;

                // 当前外端点距离（从 outerRadius 线性减小到 0）
                float r = Mth.lerp(rayProgress, outerRadius, 0f);
                if (r < 0.01f) continue; // 太近跳过

                Vec3 outer = dir.scale(r);          // 外端点
                Vec3 inner = Vec3.ZERO;              // 内端点（原点）
                Vec3 mid = outer.add(inner).scale(0.5f); // 椭圆中点
                Vec3 lightDir = inner.subtract(outer).normalize(); // 指向中心的方向（即 -dir）

                // 视线方向（从椭圆中点指向相机）
                Vec3 viewDir = cameraPos.subtract(mid).normalize();

                // 计算局部坐标系：
                // Z轴（法线）指向相机
                Vec3 normal = viewDir;
                // X轴（长轴）取 lightDir 在垂直于法线的平面上的投影
                Vec3 axisX = lightDir.subtract(normal.scale(lightDir.dot(normal))).normalize();
                if (axisX.lengthSqr() < 0.001) {
                    axisX = new Vec3(1, 0, 0).subtract(normal.scale(normal.x)).normalize();
                    if (axisX.lengthSqr() < 0.001) {
                        axisX = new Vec3(0, 1, 0).subtract(normal.scale(normal.y)).normalize();
                    }
                }
                // Y轴（短轴）由 Z 和 X 叉积得到
                Vec3 axisY = normal.cross(axisX).normalize();

                // 构建旋转矩阵：局部X->axisX, 局部Y->axisY, 局部Z->normal
                Matrix3f rotMat = new Matrix3f(
                        (float)axisX.x, (float)axisY.x, (float)normal.x,
                        (float)axisX.y, (float)axisY.y, (float)normal.y,
                        (float)axisX.z, (float)axisY.z, (float)normal.z
                );
                Quaternionf rot = new Quaternionf().setFromUnnormalized(rotMat).normalize();

                // 外层颜色，固定半透明
                int rCol = color[1][0];
                int gCol = color[1][1];
                int bCol = color[1][2];
                int aCol = (int) (color[1][3] * 0.8f);

                poseStack.pushPose();
                poseStack.translate(mid.x, mid.y, mid.z);
                poseStack.mulPose(rot);
                // 缩放：X轴为光线长度（r），Y轴为短轴，Z轴不变（法线方向）
                poseStack.scale(r, shortAxis, 1.0f);

                // ----- 绘制圆盘（四边形网格，内圈半径极小，避免退化） -----
                VertexConsumer builder = buffer.getBuffer(RenderType.entityTranslucent(ResourceLocations.WHITE_TEXTURE));
                PoseStack.Pose lastPose = poseStack.last();
                Matrix4f matrix = lastPose.pose();

                float deltaAngle = Mth.TWO_PI / ellipseSegments;
                float innerRad = 0.01f; // 很小的内半径，视觉上接近中心，但形成有效四边形
                float outerRad = 1.0f;   // 外半径，缩放后为实际大小

                for (int j = 0; j < ellipseSegments; j++) {
                    float angle1 = j * deltaAngle;
                    float angle2 = (j + 1) * deltaAngle;

                    float cos1 = Mth.cos(angle1);
                    float sin1 = Mth.sin(angle1);
                    float cos2 = Mth.cos(angle2);
                    float sin2 = Mth.sin(angle2);

                    // 内圈两点
                    float ix1 = innerRad * cos1;
                    float iy1 = innerRad * sin1;
                    float ix2 = innerRad * cos2;
                    float iy2 = innerRad * sin2;
                    // 外圈两点
                    float ox1 = outerRad * cos1;
                    float oy1 = outerRad * sin1;
                    float ox2 = outerRad * cos2;
                    float oy2 = outerRad * sin2;

                    // 四边形顶点顺序：外圈1 -> 外圈2 -> 内圈2 -> 内圈1 （顺时针，确保正面朝上）
                    builder.addVertex(matrix, ox1, oy1, 0)
                            .setColor(rCol, gCol, bCol, aCol)
                            .setUv(1, 0)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT)
                            .setNormal(lastPose, 0, 0, 1);
                    builder.addVertex(matrix, ox2, oy2, 0)
                            .setColor(rCol, gCol, bCol, aCol)
                            .setUv(1, 1)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT)
                            .setNormal(lastPose, 0, 0, 1);
                    builder.addVertex(matrix, ix2, iy2, 0)
                            .setColor(rCol, gCol, bCol, aCol)
                            .setUv(0, 1)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT)
                            .setNormal(lastPose, 0, 0, 1);
                    builder.addVertex(matrix, ix1, iy1, 0)
                            .setColor(rCol, gCol, bCol, aCol)
                            .setUv(0, 0)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT)
                            .setNormal(lastPose, 0, 0, 1);
                }
                // ----- 结束绘制 -----

                poseStack.popPose();
            }
// ========== 结束 ==========
        }else{
            float length = animatable.getLength();
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick,-animatable.yRotO,-animatable.getYRot())));
            // 要渲染的胶囊体默认是竖向的Y轴的，需要旋转到Z轴
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick,animatable.xRotO+90f,animatable.getXRot() + 90f)));
            if(animatable.tickCount < shotTick) {
                partialSize = Mth.lerp((partialTick + animatable.tickCount)/shotTick, 0, radius);
            } else if(animatable.tickCount < decayTick) {
                partialSize =  radius + (float) Math.sin((animatable.tickCount + partialTick) * 0.5f) * 0.05f;
            } else if(animatable.tickCount < discardTick) {
                partialSize = Mth.lerp( (animatable.tickCount + partialTick )/ discardTick,radius,0);
            }
            poseStack.pushPose();
            RenderUtils.renderCapsule(poseStack,buffer.getBuffer(BEAM_NO_TRANSPARENCY),partialSize*0.5f,length,segments, color[0][0], color[0][1], color[0][2], color[0][3],OverlayTexture.NO_OVERLAY,LightTexture.FULL_BRIGHT);
            poseStack.popPose();
            RenderUtils.renderCapsule(poseStack,buffer.getBuffer(RenderType.energySwirl(ResourceLocations.BEAM_FLOW_TEXTURE,0,-offset)),partialSize,length,segments,color[1][0], color[1][1], color[1][2], color[1][3],OverlayTexture.NO_OVERLAY,LightTexture.FULL_BRIGHT,1f,length);
        }
        poseStack.popPose();
    }

    private static Quaternionf rotationBetweenVectors(Vec3 from, Vec3 to) {
        float dot = (float) from.dot(to);
        if (dot > 0.9999f) return new Quaternionf().identity();
        if (dot < -0.9999f) {
            // 180度翻转，找一个垂直轴
            Vec3 axis = new Vec3(1, 0, 0).cross(from);
            if (axis.lengthSqr() < 0.001) axis = new Vec3(0, 1, 0).cross(from);
            axis = axis.normalize();
            return new Quaternionf().fromAxisAngleRad((float)axis.x, (float)axis.y, (float)axis.z, (float)Math.PI);
        }
        Vec3 axis = from.cross(to).normalize();
        float angle = (float) Math.acos(dot);
        return new Quaternionf().fromAxisAngleRad((float)axis.x, (float)axis.y, (float)axis.z, angle);
    }
    /**
     * 渲染Pro GB
     * @param partialTick 部分刻时间（用于平滑动画）
     */
    public static void render(GasterBlasterPro animatable, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
//        poseStack.pushPose(); // 在这里压栈
//        float size = animatable.getSize();
//        float radius = size * 0.5f;
//        byte segments = Config.segments(size);
//        float partialSize = 0.0f;
//        int fireTick = animatable.getFireTick();
//        poseStack.translate(0,animatable.getMonthHeight(),0);
//        if(animatable.timer < fireTick){
//            partialSize = Mth.lerp((animatable.timer + partialTick) / fireTick, 0, radius*0.75f);
//            RenderUtils.renderSphere(poseStack.last(),buffer.getBuffer(BEAM_FRONT_TYPE), partialSize, segments, r, g, b, a, OverlayTexture.NO_OVERLAY, packedLight);
//        }else if(!Vec3.ZERO.equals(animatable.getEnd())){
//            Vec3 dir = animatable.getEnd().subtract(animatable.getStart());
//            poseStack.mulPose(Axis.YP.rotationDegrees(RotUtils.yRotD(dir) + 90f));
//            poseStack.mulPose(Axis.XP.rotationDegrees(RotUtils.xRotD(dir) + 90f)); // 要渲染的胶囊体默认是竖向的Y轴的，需要旋转到Z轴
//            if(animatable.timer < fireTick + 2) {
//                partialSize = Mth.lerp((partialTick + animatable.timer - fireTick)/2, partialSize, radius);
//            } else if(animatable.timer < animatable.getDecayTick()) {
//                partialSize =  radius + (float) Math.sin((animatable.timer + partialTick) * 0.5f) * 0.05f;
//            } else {
//                partialSize = Mth.lerp( (animatable.timer + partialTick )/ 3,partialSize,0);
//            }
//            RenderUtils.renderCapsule(poseStack,buffer.getBuffer(BEAM_FRONT_TYPE),partialSize,(float) dir.length(),segments, r, g, b, a,OverlayTexture.NO_OVERLAY,packedLight);
//        }
//        poseStack.popPose();
    }



}