package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.projectile.RotationBone;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class RotationBoneModel extends DefaultedEntityGeoModel<RotationBone> {
    private static final Logger log = LoggerFactory.getLogger(RotationBoneModel.class);

    public RotationBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "bone"), true);
    }

    @Override
    public @Nullable RenderType getRenderType(RotationBone animatable, ResourceLocation texture) {
        return RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_DEPTH.apply(texture,true);
    }
}
