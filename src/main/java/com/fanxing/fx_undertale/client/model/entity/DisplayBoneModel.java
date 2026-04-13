package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.DisplayBone;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Optional;

public class DisplayBoneModel extends GrowableBoneModel<DisplayBone> {
    public DisplayBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "display_bone"));
    }

    @Override
    public void setCustomAnimations(DisplayBone animatable, long instanceId, AnimationState<DisplayBone> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        Optional<GeoBone> root = getBone("root");
        if(root.isPresent()) {
            double ticks = animatable.tickCount + animationState.getPartialTick();
            root.ifPresent(geoBone -> geoBone.setRotZ((float) ticks));
        }

    }
}
