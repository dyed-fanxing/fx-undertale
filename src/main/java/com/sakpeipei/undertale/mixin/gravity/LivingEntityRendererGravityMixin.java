package com.sakpeipei.undertale.mixin.gravity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
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
    private static final Logger log = LoggerFactory.getLogger(LivingEntityRendererGravityMixin.class);

    /**
     * 身体角度：世界转局部
     */
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "STORE"), ordinal = 2)
    private float f_yBodyRot(float value, LivingEntity entity) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || value == 0) return value;
        return switch (data.getGravity()) {
            case DOWN -> value;
            case UP -> value;
            case NORTH -> 0.0F;
            case SOUTH -> 0.0F;
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }

    /**
     * 头部角度：世界转局部
     */
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "STORE"), ordinal = 3)
    private float f1_yHeadRot(float f1,LivingEntity entity) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || f1 == 0) return f1;
        return switch (data.getGravity()) {
            case DOWN -> f1;
            case UP -> f1;
            case NORTH -> 0.0F;
            case SOUTH -> 0.0F;
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }

    /**
     * 头与身体的差值：世界转到局部
     */
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "STORE",ordinal = 4), ordinal = 4)
    private float f2(float f2,LivingEntity entity) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || f2 == 0) return f2;
        return switch (data.getGravity()) {
            case DOWN -> f2;
            case UP -> -f2; //必须取反，否则不对，试出来的，还没彻底理解
            case NORTH -> 0.0F;
            case SOUTH -> 0.0F;
            case WEST -> 0.0F;
            case EAST -> 0.0F;
        };
    }
    /**
     * 仰俯：世界转到局部
     */
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

    /**
     * 整体模型旋转：世界转到局部
     */
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void onSetupRotations(LivingEntity entity, PoseStack poseStack,float ageInTicks, float rotationYaw,float partialTicks, float scale, CallbackInfo ci) {
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        Direction gravity = data.getGravity();
        if(entity instanceof Player){
            if (gravity != Direction.DOWN) {
                float height = entity.getBbHeight();
                switch (gravity) {
                    case UP -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    case SOUTH -> {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
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
            }
        }
    }
}