package com.sakpeipei.undertale.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.utils.ColorUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author yujinbao
 * @since 2025/11/17 14:27
 * 预警提示/攻击提示 渲染器
 */
@OnlyIn(Dist.CLIENT)
public class WarningTip extends Effect {
    private static final Logger log = LogManager.getLogger(WarningTip.class);
    private final float x,y,z;  //圆心
    private final float radius, height;
    private final float r, g, b, a;

    public WarningTip(float x,float y,float z,float radius, float height, int lifetime, float r, float g, float b, float a) {
        super(lifetime);
        this.x=x;this.y=y;this.z=z;
        this.radius = radius;this.height = height;
        this.r = r;this.g = g;this.b = b;this.a = a;
    }

    public WarningTip(float x,float y,float z,float radius, float height, int lifetime, int color) {
        this(x,y,z,radius, height, lifetime, ColorUtils.getRed(color) / 255.0f, ColorUtils.getGreen(color) / 255.0f, ColorUtils.getBlue(color) / 255.0f, ColorUtils.getAlpha(color) / 255.0f);
    }
    @Override
    protected void render(PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, Camera camera) {
        poseStack.pushPose();

        // 计算相对于相机的位置偏移
        Vec3 cameraPos = camera.getPosition();
        double offsetX = x - cameraPos.x;
        double offsetY = y - cameraPos.y;
        double offsetZ = z - cameraPos.z;

        // 应用位置偏移
        poseStack.translate(offsetX, offsetY, offsetZ);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        // 渲染圆柱形警告区域
        renderCylinderOutline(poseStack.last(), consumer, radius, height);
        poseStack.popPose();
    }
    /**
     * 渲染圆柱体轮廓
     */
    private void renderCylinderOutline(PoseStack.Pose pose, VertexConsumer consumer, float radius, float height) {
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
