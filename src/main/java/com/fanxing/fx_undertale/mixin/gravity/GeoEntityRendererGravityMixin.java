package com.fanxing.fx_undertale.mixin.gravity;

import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.fanxing.lib.entity.capability.OBBHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mixin(GeoEntityRenderer.class)
public abstract class GeoEntityRendererGravityMixin {
    @Inject(method = "applyRotations(Lnet/minecraft/world/entity/Entity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",at = @At("HEAD"))
    private static  <T extends Entity & GeoAnimatable> void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale, CallbackInfo ci) {
        if(animatable instanceof OBBHolder) return;
        Direction gravity = animatable.getData(AttachmentTypes.GRAVITY);
        if(gravity != Direction.DOWN){
            poseStack.mulPose(GravityUtils.getLocalToWorldF(gravity));
        }
    }

}
