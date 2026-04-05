package com.fanxing.fx_undertale.mixin.gravity;

import com.fanxing.fx_undertale.entity.capability.OBBHolder;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGravityMixin {
    /**
     * 整体模型旋转：局部到世界
     */
    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void onSetupRotations(LivingEntity entity, PoseStack poseStack,float ageInTicks, float rotationYaw,float partialTicks, float scale, CallbackInfo ci) {
        if (entity instanceof OBBHolder)return;
        Direction gravity = entity.getData(AttachmentTypes.GRAVITY);
        if (gravity != Direction.DOWN) {
            poseStack.mulPose(GravityUtils.getLocalToWorldF(gravity));
        }
    }
}