package com.fanxing.fx_undertale.mixin.gravity;

import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.entity.capability.OBBHolder;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererGravityMixin {
    /**
     * 整体模型旋转：局部到世界
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void render(Entity entity, float p_114486_, float p_114487_, PoseStack poseStack, MultiBufferSource p_114489_, int p_114490_, CallbackInfo ci) {
//        if (entity instanceof OBBHolder)return;
//        Gravity data = entity.getData(AttachmentTypes.GRAVITY);
//        if (data.getGravity() != Direction.DOWN) {
//            poseStack.mulPose(data.getLocalToWorld());
//        }
    }
}
