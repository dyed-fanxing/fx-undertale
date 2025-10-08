package com.sakpeipei.mod.undertale.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.mod.undertale.client.model.entity.SansModel;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SansRender extends GeoEntityRenderer<Sans> {
    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
    }
}
