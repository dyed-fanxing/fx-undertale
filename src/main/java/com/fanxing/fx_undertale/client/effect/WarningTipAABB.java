package com.fanxing.fx_undertale.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;


/**
 * @author Sakpeipei
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public class WarningTipAABB extends Effect {
    private static final Logger log = LogManager.getLogger(WarningTipAABB.class);
    private final AABB area;
    private final float r, g, b, a;

    public WarningTipAABB(AABB area, int lifetime, float r, float g, float b, float a) {
        super(lifetime);
        this.area = area;
        this.r = r; this.g = g; this.b = b;this.a = a;
    }
    public WarningTipAABB(AABB area, int lifetime, Color color) {
        this(area,lifetime,color.getRed()/255.0f, color.getGreen()/255.0f, color.getBlue()/255.0f, color.getAlpha()/255.0f);
    }
    @Override
    protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
        // 确保这个方法被正确重写
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        LevelRenderer.renderLineBox(poseStack, consumer, area, r,g,b,a);
    }

    @Override
    protected AABB getBoundingBox() {
        return area;
    }
}
