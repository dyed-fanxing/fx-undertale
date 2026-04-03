package com.fanxing.fx_undertale.client.render.effect;

import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.fanxing.fx_undertale.Config;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.utils.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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
public abstract class WarningTip extends Effect {

    private static final Logger log = LoggerFactory.getLogger(WarningTip.class);
    public static int RED = FastColor.ARGB32.color(200, 255, 80, 80);

    protected final float x, y, z;
    protected final int r, g, b, a;

    public WarningTip(float x, float y, float z, int lifetime, int r, int g, int b, int a) {
        super(lifetime);
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public WarningTip(float x, float y, float z, int lifetime, int color) {
        super(lifetime);
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = FastColor.ARGB32.red(color);
        this.g = FastColor.ARGB32.green(color);
        this.b = FastColor.ARGB32.blue(color);
        this.a = FastColor.ARGB32.alpha(color);
    }

    /**
     * 获取当前透明度（基于 age 和 partialTick 插值）
     *
     * @param partialTick 帧间插值因子
     * @return 0-255 的透明度值
     */
    protected int getAlpha(float partialTick) {
        return (int) (Mth.lerp(getProgress(partialTick), 1.0f, 0) * a);
    }

    protected float getProgress(float partialTick) {
        return (age + partialTick) / lifetime;
    }

    public static class Cylinder extends WarningTip {
        private final float radius, height;
        private final Quaternionf localToWorld;

        public Cylinder(float x, float y, float z, float radius, float height, int lifetime, int color, Direction gravity) {
            super(x, y, z, lifetime, color);
            this.radius = radius;
            this.height = height;
            this.localToWorld = Gravity.getRotation(gravity);
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(localToWorld);
            poseStack.translate(0, 0.01f, 0);

            VertexConsumer sideConsumer = bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP_WHITE);
            VertexConsumer capConsumer = bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_WHITE);
            RenderUtils.renderCylinder(poseStack.last(), sideConsumer, capConsumer,
                    radius, height, Config.COMMON.segments.getAsInt(), r, g, b, getAlpha(partialTick),
                    OverlayTexture.NO_OVERLAY, LightTexture.FULL_SKY);
            poseStack.popPose();
        }

        @Override
        protected AABB getBoundingBox() {
            // 返回外接立方体（正方形）
            float halfSize = radius * 1.4142f;
            Vector3f min = localToWorld.transform(new Vector3f(-halfSize, 0, -halfSize));
            Vector3f max = localToWorld.transform(new Vector3f(halfSize, height, halfSize));
            return new AABB(x + min.x, y + min.y, z + min.z, x + max.x, y + max.y, z + max.z);
        }
    }

    public static class Cube extends WarningTip {
        private final float length, width, height;
        private final float yaw;

        public Cube(float x, float y, float z, float length, float width, float height, float yaw, int lifetime, int color) {
            super(x, y, z, lifetime, color);
            this.length = length;
            this.width = width;
            this.height = height;
            this.yaw = yaw;
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            poseStack.pushPose();
            poseStack.translate(x, y + 0.01f, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            RenderUtils.renderCubeFromBackCenter(poseStack.last(), bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_WHITE), length, width, height, r, g, b, getAlpha(partialTick), OverlayTexture.NO_OVERLAY, LightTexture.FULL_SKY);
            poseStack.popPose();
            // 强制恢复深度写入和深度测试
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend(); // 可选，根据实体需求
        }

        @Override
        public boolean shouldRender(Frustum frustum, double cameraX, double cameraY, double cameraZ) {
            return true;
        }

        protected AABB getBoundingBox() {
            return null;
        }
    }


    public static class Quad extends WarningTip {
        protected float length, width;
        protected float yaw;

        public Quad(float x, float y, float z, int lifetime, int color, float length, float width, float yaw) {
            super(x, y, z, lifetime, color);
            this.length = length;
            this.width = width;
            this.yaw = yaw;
        }


        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            RenderUtils.renderQuadForward(Vec3.ZERO, width, length, poseStack.last(), bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_WHITE), r, g, b, getAlpha(partialTick), OverlayTexture.NO_OVERLAY, LightTexture.FULL_SKY);
            poseStack.popPose();
        }

        @Override
        public boolean shouldRender(Frustum frustum, double cameraX, double cameraY, double cameraZ) {
            return true;
        }

        protected AABB getBoundingBox() {
            return null;
        }
    }

    public static class QuadPrecession extends Quad {

        public QuadPrecession(float x, float y, float z, int lifetime, int color, float length, float width, float yaw) {
            super(x, y, z, lifetime, color, length, width, yaw);
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            int alpha = getAlpha(partialTick);
            if (alpha <= 0) return;
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            float linearProgress = (age + partialTick) / lifetime;
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_WHITE);
            RenderUtils.renderQuadForward(Vec3.ZERO, width, length * Mth.sqrt(linearProgress), poseStack.last(), consumer,
                    r, g, b, alpha, OverlayTexture.NO_OVERLAY, LightTexture.FULL_SKY);
            poseStack.popPose();
        }
    }

    public static class Circle extends WarningTip {
        protected float radius;
        protected int delay;

        public Circle(float x, float y, float z, int lifetime, int color, float radius, int delay) {
            super(x, y, z, lifetime, color);
            this.radius = radius;
            this.delay = delay;
        }

        public Circle(float x, float y, float z, int lifetime, int color, float radius) {
            this(x, y, z, lifetime, color, radius, 0);
        }

        @Override
        public void tick() {
            if (!removed && delay-- < 0) {
                age++;
                if (age >= lifetime) {
                    removed = true;
                }
            }
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            if(delay > 0) return;
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            RenderUtils.drawCircleTriangleFan(poseStack.last(), bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_FAN_WHITE), Vec3.ZERO, radius, 8, r, g, b, getAlpha(partialTick), OverlayTexture.NO_OVERLAY, LightTexture.FULL_SKY);
            poseStack.popPose();
        }

        @Override
        public boolean shouldRender(Frustum frustum, double cameraX, double cameraY, double cameraZ) {
            return true;
        }

        protected AABB getBoundingBox() {
            return null;
        }
    }
    /**
     * 合并进动特效：矩形 + 圆形
     * 矩形从外部向圆心延伸，到达圆边界时圆形出现，两者无缝连接
     */
    public static class QuadCirclePrecession extends Quad {
        private final float radius;    // 圆半径
        private final float maxRectLen; // 矩形最大长度（两个前角落在圆上）
        public QuadCirclePrecession(float x, float y, float z, int lifetime, int color,
                                    float length, float width, float yaw, float radius) {
            super(x, y, z, lifetime, color, length, width, yaw);
            this.radius = radius;
            float halfWidth = width / 2;
            this.maxRectLen = length - (float) Math.sqrt(Math.max(0, radius * radius - halfWidth * halfWidth));
        }
        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            int alpha = getAlpha(partialTick);
            float progress = (age + partialTick) / lifetime;
            float rectLen = maxRectLen * Math.min(1f, progress*3F);
            boolean circleVisible = rectLen >= maxRectLen - 0.001f;
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_WHITE);
            // 矩形
            if (rectLen > 0) {
                float halfWidth = width / 2;
                RenderUtils.renderQuad(poseStack.last(), consumer,
                        -halfWidth, 0, 0,
                        -halfWidth, 0, rectLen,
                        halfWidth, 0, rectLen,
                        halfWidth, 0, 0,
                        0,1,0, 0,1,0, 0,1,0, 0,1,0,
                        0,0, 1,0, 1,1, 0,1,
                        r, g, b, alpha, OverlayTexture.NO_OVERLAY, LightTexture.FULL_SKY);
            }
            // 填充区域（三角形 + 扇形）
            if (circleVisible) {
                Vec3 center = new Vec3(0, 0, length);
                float halfWidth = width / 2;
                Vec3 rightFront = new Vec3( halfWidth, 0, rectLen);
                Vec3 leftFront  = new Vec3(-halfWidth, 0, rectLen);
                VertexConsumer fan = bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_FAN_WHITE);
                Matrix4f mat = poseStack.last().pose();
                // 1. 绘制三角形（圆心、右前角）
                addVertex(fan, mat, center, r, g, b, alpha);
                addVertex(fan, mat, leftFront, r, g, b, alpha);
                addVertex(fan, mat, rightFront, r, g, b, alpha);
                // 计算圆弧角度范围（从右前角到左前角对应的圆上点）
                double angleRight = Math.atan2(rightFront.z - center.z, rightFront.x - center.x);
                double angleLeft  = Math.atan2(leftFront.z - center.z, leftFront.x - center.x);
                if (angleLeft < angleRight) angleLeft += 2 * Math.PI;
                int segments = Config.COMMON.getSegments().getAsInt();
                // 生成圆弧上的细分点（不包括右前角和左前角，因为已经单独添加）
                for (int i = 1; i < segments; i++) {
                    double t = (double) i / segments;
                    double angle = angleRight + t * (angleLeft - angleRight);
                    float px = (float)(center.x + radius * Math.cos(angle));
                    float pz = (float)(center.z + radius * Math.sin(angle));
                    addVertex(fan, mat, new Vec3(px, 0, pz), r, g, b, alpha);
                }
                addVertex(fan, mat, leftFront, r, g, b, alpha);
                addVertex(fan, mat, center, r, g, b, alpha);
            }
            poseStack.popPose();
        }

        private void addVertex(VertexConsumer consumer, Matrix4f mat, Vec3 pos, int r, int g, int b, int a) {
            consumer.addVertex(mat, (float)pos.x, (float)pos.y, (float)pos.z)
                    .setColor(r,g,b,a)
                    .setUv(0,0)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_SKY)
                    .setNormal(0,1,0);
        }
    }


    public static class CurveStrip extends WarningTip {
        private final Function<Float, Vec3> curve;   // t∈[0,1] → 局部坐标（相对位置）
        private final int segments;                  // 分段数（整数）
        private final float radius;                  // 半径缩放
        private final float width;                   // 条带宽度
        private final float yaw;                     // 偏航角

        public CurveStrip(float x, float y, float z, int lifetime, int color,
                          float radius, float width, float yaw, int segments,
                          Function<Float, Vec3> curve) {
            super(x, y, z, lifetime, color);
            this.radius = radius;
            this.width = width;
            this.yaw = yaw;
            this.segments = segments;
            this.curve = curve;
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            int alpha = getAlpha(partialTick);
            if (alpha <= 0) return;
            poseStack.pushPose();
            poseStack.translate(x, y + 0.01f, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP_WHITE);
            Matrix4f matrix = poseStack.last().pose();

            // 关键：线性进度（用于条带长度）
            float linearProgress = (age + partialTick) / lifetime;   // 0 -> 1 匀速增长
            // 收集点集：只到 linearProgress 为止
            List<Vec3> points = new ArrayList<>();
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                if (t > linearProgress) break;   // 截断
                points.add(getWorldPoint(t));
            }
            if (points.size() < 2) {
                poseStack.popPose();
                return;
            }

            // 以下渲染代码不变，使用 points 列表生成条带
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

        /**
         * 返回相对于中心点的局部坐标（已应用半径缩放）
         */
        private Vec3 getWorldPoint(float t) {
            Vec3 local = curve.apply(t).scale(radius);
            return new Vec3(local.x, local.y, local.z);   // 不再加绝对坐标，由 poseStack 的平移负责
        }

        @Override
        protected AABB getBoundingBox() {
            double r = radius + width;
            return new AABB(x - r, y - r, z - r, x + r, y + r, z + r);
        }
    }

    public static class CurveStripPrecession extends WarningTip {
        protected final Function<Float, Vec3> curve;      // t∈[0,1] → 局部坐标（相对位置）
        protected final int segments;                     // 分段数（整数）
        protected final float radius;                     // 半径缩放
        protected final float width;                      // 条带宽度
        protected final float yaw;                        // 偏航角

        public CurveStripPrecession(float x, float y, float z, int lifetime, int color,
                                    float radius, float width, float yaw, int segments,
                                    Function<Float, Vec3> curve) {
            super(x, y, z, lifetime, color);
            this.radius = radius;
            this.width = width;
            this.yaw = yaw;
            this.segments = segments;
            this.curve = curve;
        }

        @Override
        protected float getProgress(float partialTick) {
            return CurvesUtils.riseHoldFallBezier((age + partialTick) / lifetime, 0.8f, 0.2f);
        }

        protected int getAlpha(float partialTick) {
            return (int) (Mth.lerp(getProgress(partialTick), 0f, 1f) * a);
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            int alpha = (int) (Mth.lerp(getProgress(partialTick), 0f, 1f) * a);
            ;

            poseStack.pushPose();
            poseStack.translate(x, y + 0.01f, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

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

        @Override
        protected AABB getBoundingBox() {
            double r = radius + width;
            return new AABB(x - r, y - r, z - r, x + r, y + r, z + r);
        }
    }

    public static class CurveStripPrecessionGravity extends CurveStripPrecession {
        private final Quaternionf localToWorld;

        public CurveStripPrecessionGravity(float x, float y, float z, int lifetime, int color, float radius, float width, float yaw, int segments, Function<Float, Vec3> curve, Direction gravity) {
            super(x, y, z, lifetime, color, radius, width, yaw, segments, curve);
            this.localToWorld = Gravity.getRotation(gravity);
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            poseStack.pushPose();

            int alpha = (int) (Mth.lerp(getProgress(partialTick), 0f, 1f) * a);
            ;

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
