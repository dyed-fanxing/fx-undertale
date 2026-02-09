package com.sakpeipei.undertale.mixin.gravity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGravityMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void onSetupRotations(LivingEntity entity, PoseStack poseStack,float ageInTicks, float rotationYaw,float partialTicks, float scale, CallbackInfo ci) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        if(entity instanceof Player){
            // 调试日志
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("GravityDebug");
            // 检查是不是玩家，以及玩家渲染器是否重写了setupRotations
            if (gravity != Direction.DOWN) {
                float height = entity.getBbHeight();
                switch (gravity) {
                    case UP -> {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    }
                    case NORTH -> {
                        poseStack.translate(0.0F, 0.0F, (height + 0.1F) / scale);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    }
                    case SOUTH -> {
                        poseStack.translate(0.0F, 0.0F, -(height + 0.1F) / scale);
                        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    }
                    case WEST -> {
                        poseStack.translate((height + 0.1F) / scale, 0.0F, 0.0F);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    }
                    case EAST -> {
                        poseStack.translate(-(height + 0.1F) / scale, 0.0F, 0.0F);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    }
                }
                // 记录应用后的poseStack状态（简化）
                com.mojang.math.Transformation transformation = new com.mojang.math.Transformation(poseStack.last().pose());
//                logger.info("旋转应用完成，平移: {}", transformation.getTranslation());
            }
        }
    }
}