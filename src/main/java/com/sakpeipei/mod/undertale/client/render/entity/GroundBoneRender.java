package com.sakpeipei.mod.undertale.client.render.entity;

import com.sakpeipei.mod.undertale.client.model.entity.GroundBoneModel;
import com.sakpeipei.mod.undertale.entity.summon.GroundBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * @author Sakqiongzi
 * @since 2025-08-18 20:58
 */
public class GroundBoneRender extends GeoEntityRenderer<GroundBone> {

    public GroundBoneRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GroundBoneModel());
    }


}
