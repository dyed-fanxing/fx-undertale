package com.fanxing.fx_undertale.client.render.component;

import com.fanxing.fx_undertale.utils.RenderUtils;
import com.fanxing.fx_undertale.utils.RotUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;

public class LightRay {
    public record Data(
            Vec3 startPos,      // 起点（局部坐标）
            Vec3 direction,     // 单位方向（从起点指向终点）
            float width,         // 光线宽度（绝对单位）
            float length,       // 总长度
            float startTime,    // 开始时间（tick）
            int lifeTime       // 生命周期（tick）
    ) {}
}