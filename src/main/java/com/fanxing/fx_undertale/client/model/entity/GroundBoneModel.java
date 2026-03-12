package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.GroundBone;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GroundBoneModel extends DefaultedEntityGeoModel<GroundBone> {
    public GroundBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "bone"));
    }

    @Override
    public void setCustomAnimations(GroundBone animatable, long instanceId, AnimationState<GroundBone> animationState) {
        int delay = animatable.getDelay();
        boolean curve = animatable.isCurve();
        if(curve){
            GeoBone root = this.getBone("root").get();
            root.setHidden(delay >= 0);
        }else{
            GeoBone up = this.getBone("up").get();
            GeoBone body = this.getBone("body").get();
            BoneSnapshot upInitial = up.getInitialSnapshot();
            float heightScale = animatable.getGrowScale();
            body.setScaleY((heightScale - 0.1875f) / 0.8125f);
            up.setPosY(upInitial.getOffsetY() + (heightScale - 1) * 16);
        }
    }

    @Override
    public void applyMolangQueries(AnimationState<GroundBone> animationState, double animTime) {
        GroundBone animatable = animationState.getAnimatable();
        MathParser.setVariable("query."+ FxUndertale.MOD_ID+"_grow_scale", animatable::getGrowScale);
    }

    @Override
    public @Nullable RenderType getRenderType(GroundBone animatable, ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}
