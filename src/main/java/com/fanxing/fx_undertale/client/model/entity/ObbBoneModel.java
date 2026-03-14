package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.common.RenderTypes;
import com.fanxing.fx_undertale.entity.summon.ObbBone;
import com.fanxing.fx_undertale.utils.CurvesUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class ObbBoneModel extends DefaultedEntityGeoModel<ObbBone> {
    public ObbBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "bone"));
    }

    @Override
    public void setCustomAnimations(ObbBone animatable, long instanceId, AnimationState<ObbBone> animationState) {
        int delay = animatable.getDelay();
        GeoBone root = this.getBone("root").get();
        if(delay >= 0) {
            root.setHidden(true);
        }else{
            root.setHidden(false);
            // 获取骨骼
            GeoBone up = this.getBone("up").get();
            GeoBone body = this.getBone("body").get();
            GeoBone down = this.getBone("down").get();
            BoneSnapshot upInitial = up.getInitialSnapshot();
            BoneSnapshot bodyInitial = body.getInitialSnapshot();
            BoneSnapshot downInitial = down.getInitialSnapshot();
            float t = animatable.getGrowScale()*animatable.getProgress(animationState.getPartialTick());
            body.setScaleY(1.125f*t);
            body.setPosY(bodyInitial.getOffsetY()+ 1.5f*t-3f);
            up.setPosY(upInitial.getOffsetY() + 16f*(t-1));
            down.setPosY(downInitial.getOffsetY()+1.5f*t-3f);
        }
    }

    @Override
    public @Nullable RenderType getRenderType(ObbBone animatable, ResourceLocation texture) {
        return RenderTypes.ENTITY_TRANSLUCENT_EMISSIVE_DEPTH.apply(texture,true);
    }
}
