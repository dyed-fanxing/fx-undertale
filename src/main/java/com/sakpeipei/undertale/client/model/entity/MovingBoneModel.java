package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.summon.MovingGroundBone;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class MovingBoneModel extends DefaultedEntityGeoModel<MovingGroundBone> {
    public MovingBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "bone"));
    }

    @Override
    public void setCustomAnimations(MovingGroundBone animatable, long instanceId, AnimationState<MovingGroundBone> animationState) {
        GeoBone up = this.getBone("up").get();
        GeoBone body = this.getBone("body").get();
        BoneSnapshot upInitial = up.getInitialSnapshot();
        float heightScale = animatable.getGrowScale();
        body.setScaleY((heightScale - 0.1875f) / 0.8125f);
        body.setScaleY((heightScale - 0.1875f) / 0.8125f);
        up.setPosY(upInitial.getOffsetY() + (heightScale - 1) * 16);
    }
}
