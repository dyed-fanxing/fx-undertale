package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.entity.capability.Growable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public abstract class GrowableBoneModel<T extends GeoAnimatable & Growable> extends DefaultedEntityGeoModel<T > {
    private static final Logger log = LoggerFactory.getLogger(GrowableBoneModel.class);

    public GrowableBoneModel(ResourceLocation assetSubpath) {
        super(assetSubpath);
    }
    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        // 获取骨骼
        GeoBone up = getAnimationProcessor().getBone("up");
        GeoBone body = getAnimationProcessor().getBone("body");
        GeoBone down = getAnimationProcessor().getBone("down");

        BoneSnapshot upInitial = up.getInitialSnapshot();
        BoneSnapshot bodyInitial = body.getInitialSnapshot();
        BoneSnapshot downInitial = down.getInitialSnapshot();
        float growScale = animatable.getGrowScale();
        float t = animatable.getGrowProgress(animationState.getPartialTick());
        body.setScaleY( (growScale - 0.1875f)/0.8125f*t);

        up.setPosY(upInitial.getOffsetY() + 16f*(growScale*t-1));
        body.setPosY(bodyInitial.getOffsetY()+ 3f*(t-1));
        down.setPosY(downInitial.getOffsetY()+3f*(t-1));
    }

    @Override
    public @Nullable RenderType getRenderType(T animatable, ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}
