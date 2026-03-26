package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.GroundBone;
import com.fanxing.fx_undertale.utils.AnimationUtils;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import com.zigythebird.playeranimcore.animation.Animation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GroundBoneModel extends GrowableBoneModel<GroundBone> {
    private static final Logger log = LoggerFactory.getLogger(GroundBoneModel.class);

    public GroundBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "ground_bone"));
    }

    @Override
    public void setCustomAnimations(GroundBone animatable, long instanceId, AnimationState<GroundBone> animationState) {
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
