package com.sakpeipei.undertale.mixin.gravity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.utils.CoordsUtils;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGravityMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "STORE"), ordinal = 2)
    private float f_yBodyRot(float value,LivingEntity entity) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || value == 0) return value;
        return switch (data.getGravity()) {
            case DOWN -> value;
            case UP -> -value;
            case NORTH -> 0.0F;
            case SOUTH -> 0.0F;
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "STORE"), ordinal = 3)
    private float f1_yHeadRot(float value,LivingEntity entity) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || value == 0) return value;
        return switch (data.getGravity()) {
            case DOWN -> value;
            case UP -> -value;
            case NORTH -> 0.0F;
            case SOUTH -> 0.0F;
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "STORE"), ordinal = 5)
    private float f6_xRot(float value,LivingEntity entity) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || value == 0) return value;
        return switch (data.getGravity()) {
            case DOWN -> value;
            case UP -> -value;
            case NORTH -> 0.0F;
            case SOUTH -> 0.0F;
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void onSetupRotations(LivingEntity entity, PoseStack poseStack,float ageInTicks, float rotationYaw,float partialTicks, float scale, CallbackInfo ci) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        if(entity instanceof Player){
            // 调试日志
            Logger logger = LoggerFactory.getLogger("GravityDebug");
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
                Transformation transformation = new Transformation(poseStack.last().pose());
//                logger.info("旋转应用完成，平移: {}", transformation.getTranslation());
            }
        }
    }
}