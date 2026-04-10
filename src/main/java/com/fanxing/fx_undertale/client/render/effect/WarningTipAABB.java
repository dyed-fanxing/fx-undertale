package com.fanxing.fx_undertale.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;

public class WarningTipAABB extends Effect {
    private final net.minecraft.world.phys.AABB area;
    private final float r, g, b, a;
    public WarningTipAABB(net.minecraft.world.phys.AABB area, int lifetime, float r, float g, float b,float a) {
        super(lifetime);
        this.area = area;
        this.r = r; this.g = g; this.b = b;this.a = a;
    }
    public WarningTipAABB(net.minecraft.world.phys.AABB area, int lifetime, int color) {
        this(area,lifetime, FastColor.ARGB32.red(color),FastColor.ARGB32.green(color),FastColor.ARGB32.blue(color),FastColor.ARGB32.alpha(color));
    }
    @Override
    protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        LevelRenderer.renderLineBox(poseStack, consumer, area, r, g, b, a);
    }
    @Override
    protected net.minecraft.world.phys.AABB getBoundingBox() {
        return area;
    }
}