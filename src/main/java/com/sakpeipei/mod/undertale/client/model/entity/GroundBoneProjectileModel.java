package com.sakpeipei.mod.undertale.client.model.entity;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.projectile.GroundBoneProjectile;
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
        GeoBone up = this.getBone("edge-up").get();
        GeoBone body = this.getBone("body").get();
        BoneSnapshot upInitial = up.getInitialSnapshot();
        BoneSnapshot bodyInitial = body.getInitialSnapshot();
        float addHeight = animatable.getHeight() * 16;

        double ySize = bodyInitial.getBone().getCubes().getFirst().size().y;
        body.setScaleY(1.0f + addHeight/ (float)ySize);
        up.setPosY(upInitial.getOffsetY() + addHeight);
    }
}
