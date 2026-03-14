package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.projectile.FlyingBone;
import com.fanxing.fx_undertale.entity.projectile.RotationBone;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import java.util.Optional;

public class RotationBoneModel extends DefaultedEntityGeoModel<RotationBone> {
    public RotationBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "bone"), true);
    }

    @Override
    public @Nullable RenderType getRenderType(RotationBone animatable, ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    @Override
    public void setCustomAnimations(RotationBone animatable, long instanceId, AnimationState<RotationBone> animationState) {
        Optional<GeoBone> root = this.getBone("root");
        root.ifPresent(geoBone -> {
            // 设置枢轴点到几何中心（根据你的模型实际尺寸调整）
            geoBone.setPivotX(0);
            geoBone.setPivotY(8); // 例如模型高度是 16 像素，中心可能在 8
            geoBone.setPivotZ(0);
            // 设定旋转速度：每 tick 转 0.1 弧度 ≈ 每秒转 0.1*20 = 2 弧度 ≈ 114 度/秒
            geoBone.setRotZ((float) (animationState.getAnimationTick() * 1));
        });
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}
