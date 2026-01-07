package com.sakpeipei.undertale.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.common.Config;
import com.sakpeipei.undertale.utils.ColorUtils;
import com.sakpeipei.undertale.utils.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;


/**
 * @author yujinbao
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public class WarningTip extends Effect {
    private final float x,y,z;  // 底部圆心
    private final float radius, height;
    private final int r, g, b, a;

    public WarningTip(float x,float y,float z,float radius, float height, int lifetime, int r, int g, int b, int a) {
        super(lifetime);
        this.x=x; this.y=y; this.z=z;
        this.radius = radius;this.height = height;
        this.r = r;this.g = g;this.b = b;this.a = a;
    }

    public WarningTip(float x,float y,float z,float radius, float height, int lifetime, Color color) {
        this(x,y,z,radius, height, lifetime, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }
    @Override
    protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        RenderUtils.renderCylinderOutline(poseStack.last(), consumer,radius,height, Config.segments(radius),r,g,b,a, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
    @Override
    protected AABB getBoundingBox() {
        // 返回外接立方体（正方形）
        // 半径乘以√2得到外接正方形的半边长
        float halfSize = radius * 1.4142f; // √2 ≈ 1.4142
        return new AABB(
                x - halfSize, y, z - halfSize,
                x + halfSize, y + height, z + halfSize
        );
    }

}
