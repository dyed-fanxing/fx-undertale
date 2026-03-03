package com.sakpeipei.undertale.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.Config;
import com.sakpeipei.undertale.common.RenderTypes;
import com.sakpeipei.undertale.common.ResourceLocations;
import com.sakpeipei.undertale.entity.attachment.Gravity;
import com.sakpeipei.undertale.utils.RenderUtils;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;


/**
 * @author Sakpeipei
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public abstract class WarningTip extends Effect {

    public static int RED = FastColor.ARGB32.color(200, 255, 80, 80);

    protected final float x,y,z;
    protected final int r, g, b, baseAlpha;
    public WarningTip(float x,float y,float z,int lifetime, int r, int g, int b, int baseAlpha) {
        super(lifetime);
        this.x=x; this.y=y; this.z=z;
        this.r = r;this.g = g;this.b = b;this.baseAlpha = baseAlpha;
    }


    /**
     * 获取当前透明度（基于 age 和 partialTick 插值）
     * @param partialTick 帧间插值因子
     * @return 0-255 的透明度值
     */
    protected int getAlpha(float partialTick) {
        float currentTime = age + partialTick;          // 浮点时间
        float progress = currentTime / lifetime;        // 已完成比例 (0~1)
        return (int) (Mth.lerp(progress, 1.0f, 0)*baseAlpha);
    }

    public static class Cylinder extends WarningTip {
        private final float radius, height;
        private final Quaternionf localToWorld;

        public Cylinder(float x, float y, float z, float radius, float height, int lifetime, int r, int g, int b, int baseAlpha, Direction gravity) {
            super(x, y, z, lifetime, r, g, b, baseAlpha);
            this.radius = radius;
            this.height = height;
            this.localToWorld = Gravity.getRotation(gravity);
        }

        public Cylinder(float x, float y, float z, float radius, float height, int lifetime, int color, Direction gravity) {
            this(x,y,z,radius, height, lifetime, FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color),gravity);
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            poseStack.pushPose();
            poseStack.translate(x, y+0.01f, z);
            poseStack.mulPose(localToWorld);
            
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
            return new AABB( x+min.x,y+min.y,z+min.z,x+max.x,y+max.y,z+max.z);
        }
    }


    public static class Cube extends WarningTip {
        private final float length, width,height;
        private final float yaw;
        public Cube(float x, float y, float z, float length, float width,float height,float yaw, int lifetime, int r, int g, int b, int a) {
            super(x, y, z, lifetime, r, g, b, a);
            this.length = length;
            this.width = width;
            this.height = height;
            this.yaw = yaw;
        }

        public Cube(float x, float y, float z, float length,float width, float height,float yaw, int lifetime, int color) {
            this(x,y,z,length,width, height,yaw, lifetime, FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color));
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            poseStack.pushPose();
            poseStack.translate(x, y+0.01f, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            RenderUtils.renderCubeFromBackCenter(poseStack.last(), bufferSource.getBuffer(RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_WHITE),length,width,height,r, g, b, getAlpha(partialTick), OverlayTexture.NO_OVERLAY, LightTexture.FULL_SKY);
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
}
