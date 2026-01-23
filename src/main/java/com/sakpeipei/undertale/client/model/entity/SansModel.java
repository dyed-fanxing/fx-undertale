package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.Optional;

public class SansModel extends DefaultedEntityGeoModel<Sans> {
    public SansModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "sans"), true);
    }

    @Override
    public void setCustomAnimations(Sans animatable, long instanceId, AnimationState<Sans> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}
