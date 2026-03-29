package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.projectile.FlyingBone;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class FlyingBoneModel extends DefaultedEntityGeoModel<FlyingBone> {
    public FlyingBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "flying_bone"), true);
    }

    @Override
    public @Nullable RenderType getRenderType(FlyingBone animatable, ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}
