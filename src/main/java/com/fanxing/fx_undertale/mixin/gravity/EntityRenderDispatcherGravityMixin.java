package com.fanxing.fx_undertale.mixin.gravity;

import com.fanxing.fx_undertale.entity.capability.OBBHolder;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
 * @author FanXing
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
        if(entity instanceof OBBHolder) return offset;
        Direction gravity = entity.getData(AttachmentTypes.GRAVITY);
        if (gravity == Direction.DOWN) return offset;
        else return GravityUtils.localToWorld(gravity,offset);
    }

    @Redirect(method = "renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;FFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLineBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDDDDFFFF)V",
                    ordinal = 0))
    private static void onRenderEyeLineBox(PoseStack poseStack, VertexConsumer consumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a, @Local(ordinal = 0, argsOnly = true) Entity entity,@Local(ordinal = 0) AABB aabb){
        if(entity instanceof OBBHolder) return;
        Direction gravity = entity.getData(AttachmentTypes.GRAVITY);
        switch (gravity){
            case DOWN -> LevelRenderer.renderLineBox(poseStack, consumer, minX, minY,minZ, maxX,maxY, maxZ, r, g, b, a);
            case UP -> LevelRenderer.renderLineBox(poseStack, consumer, minX, -maxY,minZ, maxX,-minY, maxZ,  r, g, b, a);
            case EAST ->  LevelRenderer.renderLineBox(poseStack, consumer, -maxY, aabb.minY,minZ,-minY,aabb.maxY, maxZ,  r, g, b, a);
            case WEST ->  LevelRenderer.renderLineBox(poseStack, consumer, minY, aabb.minY,minZ,maxY,aabb.maxY, maxZ,  r, g, b, a);
            case SOUTH -> LevelRenderer.renderLineBox(poseStack, consumer, minX, aabb.minY,-maxY, maxX,aabb.maxY, -minY,  r, g, b, a);
            case NORTH -> LevelRenderer.renderLineBox(poseStack, consumer, minX, aabb.minY,minY, maxX,aabb.maxY, maxY,  r, g, b, a);
        }
    }

    @ModifyArg(method = "renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;FFFF)V",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderVector(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Vector3f;Lnet/minecraft/world/phys/Vec3;I)V"),index = 2)
    private static Vector3f viewVectorEyeHeightVector3f(Vector3f eyePosOffset, @Local(argsOnly = true, ordinal = 0) Entity entity) {
        if(entity instanceof OBBHolder) return eyePosOffset;
        Direction gravity = entity.getData(AttachmentTypes.GRAVITY);
        if(gravity == Direction.DOWN) return eyePosOffset;
        else return GravityUtils.localToWorld(gravity,eyePosOffset);
    }


    @Inject(method = "renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;FFFF)V", at = @At("TAIL"))
    private static void onRenderHitbox(PoseStack poseStack, VertexConsumer consumer, Entity entity, float partialTicks, float r, float g, float b, CallbackInfo ci) {
        if(entity instanceof OBBHolder) return;
        Direction gravity = entity.getData(AttachmentTypes.GRAVITY);
        if(gravity != Direction.DOWN) {
            Vec3i normal = gravity.getNormal();
            Vector3f up = GravityUtils.getUp(gravity);
            Vector3f forward = GravityUtils.getForward(gravity);
            Vector3f right = GravityUtils.getRight(gravity);
            LevelRenderer.renderLineBox(poseStack, consumer,0,0,0,right.x, right.y, right.z,1.0F, 0, 0, 1.0F);
            LevelRenderer.renderLineBox(poseStack, consumer,0,0,0,up.x, up.y, up.z,0, 1.0F, 0, 1.0F);
            LevelRenderer.renderLineBox(poseStack, consumer,0,0,0,forward.x, forward.y, forward.z,0, 0, 1.0F, 1.0F);
            LevelRenderer.renderLineBox(poseStack, consumer,0,0,0,normal.getX(), normal.getY(), normal.getZ(),0, 0, 0F, 1.0F);
        }
    }

}