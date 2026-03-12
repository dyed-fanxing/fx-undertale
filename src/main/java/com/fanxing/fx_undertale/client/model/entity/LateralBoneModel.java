package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.LateralBone;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class LateralBoneModel extends DefaultedEntityGeoModel<LateralBone> {
    public LateralBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "bone"));
    }

    @Override
    public void setCustomAnimations(LateralBone animatable, long instanceId, AnimationState<LateralBone> animationState) {
        GeoBone up = this.getBone("up").get();
        GeoBone body = this.getBone("body").get();
        BoneSnapshot upInitial = up.getInitialSnapshot();
        float heightScale = animatable.getGrowScale();
        body.setScaleY((heightScale - 0.1875f) / 0.8125f);
        up.setPosY(upInitial.getOffsetY() + (heightScale - 1) * 16);
    }

    @Override
    public @Nullable RenderType getRenderType(LateralBone animatable, ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}
