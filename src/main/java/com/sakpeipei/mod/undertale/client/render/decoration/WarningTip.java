package com.sakpeipei.mod.undertale.client.render.decoration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.mod.undertale.utils.ColorUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.swing.plaf.ColorUIResource;
import java.awt.*;


/**
 * @author yujinbao
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public class WarningTip extends Decoration {
    private final AABB area;
    private final float r, g, b, a;

    public WarningTip(AABB area, int lifetime, float r, float g, float b,float a) {
        super(lifetime);
        this.area = area;
        this.r = r; this.g = g; this.b = b;this.a = a;
    }
    public WarningTip(AABB area, int lifetime, int color) {
        this(area,lifetime,ColorUtils.getRed(color),ColorUtils.getGreen(color),ColorUtils.getBlue(color),ColorUtils.getAlpha(color));
    }
    @Override
    protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        LevelRenderer.renderLineBox(poseStack, consumer, area, r, g, b, a);
    }

    @Override
    protected AABB getBoundingBox() {
        return area;
    }
}
