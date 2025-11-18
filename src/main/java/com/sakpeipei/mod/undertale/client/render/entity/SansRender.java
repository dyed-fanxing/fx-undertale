package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.mod.undertale.client.model.entity.SansModel;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SansRender extends GeoEntityRenderer<Sans> {
    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
    }
}
