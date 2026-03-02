package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.projectile.FlyingBone;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class FlyingBoneModel extends DefaultedEntityGeoModel<FlyingBone> {
    public FlyingBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "bone"), true);
    }

    @Override
    public @Nullable RenderType getRenderType(FlyingBone animatable, ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}
