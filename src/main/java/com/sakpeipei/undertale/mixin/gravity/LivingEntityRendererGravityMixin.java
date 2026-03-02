package com.sakpeipei.undertale.mixin.gravity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sakpeipei.undertale.entity.attachment.Gravity;
import com.sakpeipei.undertale.registry.AttachmentTypes;
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
        Gravity data = entity.getData(AttachmentTypes.GRAVITY);
        Direction gravity = data.getGravity();
        if (gravity != Direction.DOWN) {
            poseStack.mulPose(data.getLocalToWorld());
        }
    }
}