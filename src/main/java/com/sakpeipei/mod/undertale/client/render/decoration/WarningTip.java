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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;


/**
 * @author yujinbao
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public class WarningTip extends Decoration {
    private static final Logger log = LogManager.getLogger(WarningTip.class);
    private final float radius, height;
    private final float r, g, b, a;

    public WarningTip(float radius, float height, int lifetime, float r, float g, float b, float a) {
        super(lifetime);
        this.radius = radius;
        this.height = height;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public WarningTip(float radius, float height, int lifetime, int color) {
        this(radius, height, lifetime, ColorUtils.getRed(color) / 255.0f, ColorUtils.getGreen(color) / 255.0f, ColorUtils.getBlue(color) / 255.0f, ColorUtils.getAlpha(color) / 255.0f);
    }

    @Override
    protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
        // 确保这个方法被正确重写
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        // 根据给的半径和高度，渲染提示线框
        // 渲染圆柱形警告区域
        renderCylinderOutline(poseStack.last().pose(), consumer, radius, height);
    }
    /**
     * 渲染圆柱体轮廓
     */
    private void renderCylinderOutline(Matrix4f pose, VertexConsumer consumer, float radius, float height) {
        final int segments = 32; // 圆的细分段数
        final float angleStep = (float) (2 * Math.PI / segments);

        // 渲染底部圆环
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) % segments * angleStep;

            float x1 = radius * (float) Math.cos(angle1);
            float z1 = radius * (float) Math.sin(angle1);
            float x2 = radius * (float) Math.cos(angle2);
            float z2 = radius * (float) Math.sin(angle2);

            // 底部圆的边
            consumer.addVertex(pose, x1, 0, z1)
                    .setColor(r, g, b, a);
            consumer.addVertex(pose, x2, 0, z2)
                    .setColor(r, g, b, a);
        }

        // 渲染顶部圆环
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) % segments * angleStep;

            float x1 = radius * (float) Math.cos(angle1);
            float z1 = radius * (float) Math.sin(angle1);
            float x2 = radius * (float) Math.cos(angle2);
            float z2 = radius * (float) Math.sin(angle2);

            // 顶部圆的边
            consumer.addVertex(pose, x1, height, z1).setColor(r, g, b, a);
            consumer.addVertex(pose, x2, height, z2).setColor(r, g, b, a);
        }

        // 渲染侧面竖线
        int verticalLines = Math.min(segments, 8); // 渲染8条竖线
        for (int i = 0; i < verticalLines; i++) {
            float angle = i * (float) (2 * Math.PI / verticalLines);
            float x = radius * (float) Math.cos(angle);
            float z = radius * (float) Math.sin(angle);

            consumer.addVertex(pose, x, 0, z).setColor(r, g, b, a);
            consumer.addVertex(pose, x, height, z).setColor(r, g, b, a);
        }
    }
    @Override
    protected AABB getBoundingBox() {
        // 返回外接立方体（正方形）
        // 半径乘以√2得到外接正方形的半边长
        float halfSize = radius * 1.4142f; // √2 ≈ 1.4142
        return new AABB(
                -halfSize, 0, -halfSize,
                halfSize, height, halfSize
        );
    }

}
