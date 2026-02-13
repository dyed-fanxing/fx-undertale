package com.sakpeipei.undertale.mixin.gravity;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Sakqiongzi
 * @since 2025-09-28 01:12
 */
@OnlyIn(Dist.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherGravityMixin {
    /**
     * 根据重力方向渲染偏移
     * @param offset 局部偏移
     */
    @ModifyVariable(method = "render",at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getRenderOffset(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/world/phys/Vec3;"),
            ordinal = 0)
    private Vec3 modifyRenderOffset(Vec3 offset, Entity entity) {
        GravityData data = entity.getData(AttachmentTypes.GRAVITY);
        if (data.getGravity() == Direction.DOWN) return offset;
        else return data.localToWorld(offset);
    }

    @Inject(method = "renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;FFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLineBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDDDDFFFF)V",
                    ordinal = 0), cancellable = true)
    private static void onRenderEyeLineBox(PoseStack poseStack, VertexConsumer consumer, Entity entity, float p_114445_, float p_353064_, float p_353059_, float p_353042_, CallbackInfo ci, @Local(ordinal = 0) AABB aabb){
        GravityData data = entity.getData(AttachmentTypes.GRAVITY);
        if(data.getGravity() != Direction.DOWN){
            ci.cancel();
            switch (data.getGravity()){
                case UP -> LevelRenderer.renderLineBox(poseStack, consumer, aabb.minX, -entity.getEyeHeight() - 0.01F,aabb.minZ, aabb.maxX,-entity.getEyeHeight() + 0.01F, aabb.maxZ, 1.0F, 0.0F, 0.0F, 1.0F);
                case EAST ->  LevelRenderer.renderLineBox(poseStack, consumer, -entity.getEyeHeight() - 0.01F, aabb.minY,aabb.minZ,-entity.getEyeHeight() + 0.01F,aabb.maxY, aabb.maxZ, 1.0F, 0.0F, 0.0F, 1.0F);
                case WEST ->  LevelRenderer.renderLineBox(poseStack, consumer, entity.getEyeHeight() - 0.01F, aabb.minY,aabb.minZ,entity.getEyeHeight() + 0.01F,aabb.maxY, aabb.maxZ, 1.0F, 0.0F, 0.0F, 1.0F);
                case SOUTH -> LevelRenderer.renderLineBox(poseStack, consumer, aabb.minX, aabb.minY,-entity.getEyeHeight() - 0.01F, aabb.maxX,aabb.maxY, -entity.getEyeHeight() + 0.01F, 1.0F, 0.0F, 0.0F, 1.0F);
                case NORTH -> LevelRenderer.renderLineBox(poseStack, consumer, aabb.minX, aabb.minY,entity.getEyeHeight() - 0.01F, aabb.maxX,aabb.maxY, entity.getEyeHeight() + 0.01F, 1.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }


    @ModifyArg(method = "renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;FFFF)V",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderVector(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Vector3f;Lnet/minecraft/world/phys/Vec3;I)V"),index = 2)
    private static Vector3f viewVectorEyeHeightVector3f(Vector3f eyePosOffset, @Local(argsOnly = true, ordinal = 0) Entity entity) {
        GravityData data = entity.getData(AttachmentTypes.GRAVITY);
        if(data.getGravity() == Direction.DOWN) return eyePosOffset;
        else return data.localToWorld(eyePosOffset);
    }

}