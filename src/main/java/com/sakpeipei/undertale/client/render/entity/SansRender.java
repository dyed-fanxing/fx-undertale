package com.sakpeipei.undertale.client.render.entity;

import com.sakpeipei.undertale.client.model.entity.SansModel;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SansRender extends GeoEntityRenderer<Sans> {
    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
    }
}
