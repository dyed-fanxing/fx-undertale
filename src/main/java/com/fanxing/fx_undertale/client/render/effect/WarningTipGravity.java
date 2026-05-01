package com.fanxing.fx_undertale.client.render.effect;

import com.fanxing.fx_undertale.utils.GravityUtils;
import com.fanxing.lib.ConfigFxLib;
import com.fanxing.lib.client.render.effect.Effect;
import com.fanxing.lib.client.render.effect.WarningTip;
import com.fanxing.lib.client.render.shape.CircleRenderer;
import com.fanxing.lib.client.render.shape.CubeRenderer;
import com.fanxing.lib.client.render.shape.CylinderRenderer;
import com.fanxing.lib.client.render.shape.QuadRenderer;
import com.fanxing.lib.client.render.type.RenderTypes;
import com.fanxing.lib.util.CurvesUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * @author Sakpeipei
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public class WarningTipGravity{
    public static class Cylinder extends WarningTip.Cylinder {
        public final Quaternionf quaternionf;

        public Cylinder(float x, float y, float z, float radius, float height, int lifetime, int color, Direction gravity) {
            super(x, y, z, lifetime, color,radius,height);
            this.quaternionf = GravityUtils.getLocalToWorldF(gravity);
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.translate(0, 0.01f, 0);
            poseStack.mulPose(quaternionf);
            CylinderRenderer.render(poseStack.last(),bufferSource, RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP_WHITE, RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_WHITE,new Vector3f(),radius, height, ConfigFxLib.Client.SEGMENTS.getAsInt(), r, g, b, getAlpha(partialTick),
                    OverlayTexture.NO_OVERLAY, LightTexture.FULL_SKY);
            poseStack.popPose();
        }

        @Override
        protected AABB getBoundingBox() {
            // 返回外接立方体（正方形）
            float halfSize = radius * 1.4142f;
            Vector3f min = new Vector3f(-halfSize, 0, -halfSize);
            Vector3f max = new Vector3f(halfSize, height, halfSize);
            return new AABB(x + min.x, y + min.y, z + min.z, x + max.x, y + max.y, z + max.z);
        }
    }

    public static class CurveStripPrecessionGravity extends WarningTip.CurveStripPrecession {
        private final Quaternionf localToWorld;

        public CurveStripPrecessionGravity(float x, float y, float z, int lifetime, int color, float radius, float width, float yaw, int segments, Function<Float, Vec3> curve, Direction gravity) {
            super(x, y, z, lifetime, color, radius, width, yaw, segments, curve);
            this.localToWorld = GravityUtils.getLocalToWorldF(gravity);
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            poseStack.pushPose();

            int alpha = (int) (Mth.lerp(getProgress(partialTick), 0f, 1f) * a);

            poseStack.pushPose();
            poseStack.translate(x, y, z);                      // 1. 先平移到目标世界位置
            poseStack.mulPose(localToWorld);   // 2. 应用重力旋转（使局部Y轴指向重力方向）
            poseStack.translate(0, 0.01f, 0);                 // 3. 沿局部Y轴向上偏移
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw)); // 4. 绕局部Y轴旋转
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP_WHITE);
            Matrix4f matrix = poseStack.last().pose();

            // 使用生命周期的一半进行进动
            float linearProgress = ((age + partialTick) / lifetime) * 2f;
            List<Vec3> points = new ArrayList<>();
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                if (t > linearProgress) break;   // 使用线性进度截断
                points.add(curve.apply(t).scale(radius));
            }
            if (points.size() < 2) {
                poseStack.popPose();
                return;
            }
            // 2. 渲染整条曲线条带（使用衰减透明度）
            for (int i = 0; i < points.size(); i++) {
                Vec3 p = points.get(i);
                Vec3 tangent;
                if (i == 0) {
                    tangent = points.get(1).subtract(p).normalize();
                } else if (i == points.size() - 1) {
                    tangent = p.subtract(points.get(i - 1)).normalize();
                } else {
                    tangent = points.get(i + 1).subtract(points.get(i - 1)).normalize();
                }
                Vec3 normal = new Vec3(0, 1, 0).cross(tangent).normalize();
                Vec3 left = p.add(normal.scale(width / 2));
                Vec3 right = p.add(normal.scale(-width / 2));
                consumer.addVertex(matrix, (float) left.x, (float) left.y, (float) left.z)
                        .setColor(r, g, b, alpha)
                        .setUv(0, 0)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_SKY)
                        .setNormal(poseStack.last(), 0, 1, 0);
                consumer.addVertex(matrix, (float) right.x, (float) right.y, (float) right.z)
                        .setColor(r, g, b, alpha)
                        .setUv(0, 0)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_SKY)
                        .setNormal(poseStack.last(), 0, 1, 0);
            }
            poseStack.popPose();
        }
    }

}
