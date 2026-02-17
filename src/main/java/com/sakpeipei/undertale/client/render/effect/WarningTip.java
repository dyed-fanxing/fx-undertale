package com.sakpeipei.undertale.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.common.Config;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.utils.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;


/**
 * @author Sakpeipei
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public abstract class WarningTip extends Effect {
    protected final float x,y,z;
    protected final int r, g, b, a;
    public WarningTip(float x,float y,float z,int lifetime, int r, int g, int b, int a) {
        super(lifetime);
        this.x=x; this.y=y; this.z=z;
        this.r = r;this.g = g;this.b = b;this.a = a;
    }

    public static class Circle extends WarningTip {
        private final float radius, height;
        private final Quaternionf localToWorld;

        public Circle(float x, float y, float z, float radius, float height, int lifetime, int r, int g, int b, int a, Direction gravity) {
            super(x, y, z, lifetime, r, g, b, a);
            this.radius = radius;
            this.height = height;
            this.localToWorld = GravityData.getRotation(gravity);
        }

        public Circle(float x, float y, float z, float radius, float height, int lifetime, int color, Direction gravity) {
            this(x,y,z,radius, height, lifetime, FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color),gravity);
        }

        @Override
        protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(localToWorld);
            RenderUtils.renderCylinderOutline(poseStack.last(), consumer,radius,height, Config.segments(radius),r,g,b,a, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
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
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            RenderUtils.renderCubeOutline(poseStack.last(),consumer,length,width,height,r,g,b,a, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
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
