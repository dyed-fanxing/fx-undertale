package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.projectile.GroundBoneProjectile;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GroundBoneProjectileModel extends DefaultedEntityGeoModel<GroundBoneProjectile> {
    public GroundBoneProjectileModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "bone"));
    }




    @Override
    public void setCustomAnimations(GroundBoneProjectile animatable, long instanceId, AnimationState<GroundBoneProjectile> animationState) {
        GeoBone up = this.getBone("up").get();
        GeoBone body = this.getBone("body").get();
        BoneSnapshot upInitial = up.getInitialSnapshot();
        float heightScale = animatable.getHeightScale();
        body.setScaleY((heightScale - 0.1875f) / 0.8125f);
        up.setPosY(upInitial.getOffsetY() + (heightScale - 1) * 16);
    }
}
