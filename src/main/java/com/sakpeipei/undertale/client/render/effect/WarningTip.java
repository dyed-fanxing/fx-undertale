package com.sakpeipei.undertale.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.common.Config;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.utils.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
public class WarningTip extends Effect {
    private final float x,y,z;  // 底部圆心
    private final float radius, height;
    private final int r, g, b, a;
    private final Quaternionf localToWorld;
    public WarningTip(float x,float y,float z,float radius, float height, int lifetime, int r, int g, int b, int a,Direction gravity) {
        super(lifetime);
        this.x=x; this.y=y; this.z=z;
        this.radius = radius;this.height = height;
        this.r = r;this.g = g;this.b = b;this.a = a;
        this.localToWorld = GravityData.getRotation(gravity);
    }

    public WarningTip(float x, float y, float z, float radius, float height, int lifetime, int color, Direction gravity) {
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
