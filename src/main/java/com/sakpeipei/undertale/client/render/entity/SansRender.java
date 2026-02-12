package com.sakpeipei.undertale.client.render.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.client.model.entity.SansModel;
import com.sakpeipei.undertale.client.render.layer.SansEyesLayer;
import com.sakpeipei.undertale.client.render.layer.SansFatigueLayer;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SansRender extends GeoEntityRenderer<Sans> {
    public SansRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SansModel());
        this.renderLayers.addLayer(new SansEyesLayer(this, ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "textures/entity/sans_eyes.png")));
        this.renderLayers.addLayer(new SansFatigueLayer(this));
    }

}
