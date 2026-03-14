package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import java.util.Optional;

public class SansModel extends DefaultedEntityGeoModel<Sans> {
    private static final Logger log = LoggerFactory.getLogger(SansModel.class);
    public SansModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "sans"), true);
    }

    @Override
    public void setCustomAnimations(Sans animatable, long instanceId, AnimationState<Sans> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        Optional<GeoBone> rotationBoneLeft = getBone("rotation_bone_left");
        Optional<GeoBone> rotationBoneRight = getBone("rotation_bone_right");
        rotationBoneLeft.ifPresent(geoBone -> geoBone.setHidden(!Sans.ANIM_BONE_ROTATION.equals(animationState.getController().getCurrentRawAnimation())));
        rotationBoneRight.ifPresent(geoBone -> geoBone.setHidden(!Sans.ANIM_BONE_ROTATION.equals(animationState.getController().getCurrentRawAnimation())));
    }
}
