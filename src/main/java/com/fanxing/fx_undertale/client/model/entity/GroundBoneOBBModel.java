package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.GroundBoneOBB;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class GroundBoneOBBModel extends GrowableBoneModel<GroundBoneOBB> {
    private static final Logger log = LoggerFactory.getLogger(GroundBoneOBBModel.class);

    public GroundBoneOBBModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "ground_bone_obb"));
    }

    @Override
    public void setCustomAnimations(GroundBoneOBB animatable, long instanceId, AnimationState<GroundBoneOBB> animationState) {
        int delay = animatable.getDelay();
        GeoBone root = this.getBone("root").get();
        if(delay >= 0) {
            root.setHidden(true);
        }else{
            root.setHidden(false);
            super.setCustomAnimations(animatable, instanceId, animationState);
        }
    }
}
