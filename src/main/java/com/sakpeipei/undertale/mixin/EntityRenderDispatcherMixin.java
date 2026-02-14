package com.sakpeipei.undertale.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sakpeipei.undertale.common.phys.OBB;
import com.sakpeipei.undertale.entity.IOBBCapability;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import com.sakpeipei.undertale.utils.RenderUtils;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.entity.PartEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Sakqiongzi
 * @since 2025-09-28 01:12
 */
@OnlyIn(Dist.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    private static final Logger log = LogManager.getLogger(EntityRenderDispatcherMixin.class);

    @Shadow
    private static void renderVector(PoseStack poseStack, VertexConsumer consumer, Vector3f p_353068_, Vec3 p_353070_, int p_353032_){
    }

    /**
     * 拦截原版的renderHitbox方法调用
     * 如果实体实现了OBBProvider，就渲染OBB而不是原版AABB
     */
    @Inject(method = "renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;FFFF)V", at = @At("HEAD"),
            cancellable = true)
    private static void onRenderHitbox(PoseStack poseStack, VertexConsumer consumer, Entity entity, float partialTicks, float r, float g, float b, CallbackInfo ci) {
        if (entity instanceof IOBBCapability obbEntity) {
            ci.cancel();
            OBB obb = obbEntity.getOBB().move(-entity.getX(), -entity.getY(), -entity.getZ());
            RenderUtils.renderOBBOutline(poseStack.last(), consumer, obb,r,g,b,1.0f);
            // 2. 如果是多部分实体，渲染各部分（保持原版逻辑）
            if (entity.isMultipartEntity()) {
                double d0 = -Mth.lerp(partialTicks, entity.xOld, entity.getX());
                double d1 = -Mth.lerp(partialTicks, entity.yOld, entity.getY());
                double d2 = -Mth.lerp(partialTicks, entity.zOld, entity.getZ());
                for(PartEntity<?> part : entity.getParts()) {
                    poseStack.pushPose();
                    if(part != null){
                        double d3 = d0 + Mth.lerp(partialTicks, part.xOld, part.getX());
                        double d4 = d1 + Mth.lerp(partialTicks, part.yOld, part.getY());
                        double d5 = d2 + Mth.lerp(partialTicks, part.zOld, part.getZ());
                        poseStack.translate(d3, d4, d5);
                        // 检查部分实体是否也是OBB实体
                        if (part instanceof IOBBCapability partObbEntity) {
                            RenderUtils.renderOBBOutline(poseStack.last(), consumer, partObbEntity.getOBB(),r,g,b,255);
                        } else {
                            // 原版渲染
                            AABB partAABB = part.getBoundingBox().move(-part.getX(), -part.getY(), -part.getZ());
                            LevelRenderer.renderLineBox(poseStack, consumer, partAABB, 0.25F, 1.0F, 0.0F, 1.0F);
                        }
                    }
                    poseStack.popPose();
                }
            }

            // 3. 如果是生物，渲染眼睛高度
            if (entity instanceof LivingEntity) {
                RenderUtils.renderOBBOutline(poseStack.last(), consumer, new OBB(
                        new Vec3(obb.center.x,entity.getEyeHeight(),obb.center.z),
                        obb.xHalfSize,0.01f,obb.zHalfSize,obb.forward,obb.up
                ),r,g,b,1.0f);
            }

            // 4. 如果有载具，渲染乘坐位置
            Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                float f = Math.min(vehicle.getBbWidth(), entity.getBbWidth()) / 2.0F;
                float f2 = 0.0625F;
                Vec3 vec3 = vehicle.getPassengerRidingPosition(entity).subtract(entity.position());
                if(vehicle instanceof IOBBCapability vehicleObbEntity){
                    OBB vehicleObb = vehicleObbEntity.getOBB();
                    Vec3 ridingPosWorld = vehicle.getPassengerRidingPosition(entity);
                    poseStack.pushPose();
                    poseStack.translate(-vehicle.getX(), -vehicle.getY(), -vehicle.getZ());
                    // 待渲染OBB载具
                    // 创建乘坐位置的扁平方框OBB
                    RenderUtils.renderOBBOutline(poseStack.last(), consumer, new OBB(
                            ridingPosWorld,
                            Math.min(obb.xHalfSize, vehicleObb.xHalfSize),  // 宽度
                            0.03125f,  // 很薄（0.0625F的一半，因为OBB是半尺寸）
                            Math.min(obb.zHalfSize, vehicleObb.zHalfSize),  // 长度
                            obb.forward,  // 使用骑手的方向
                            obb.up
                    ), 255, 255, 0, 255);
                    poseStack.popPose();
                }else{
                    LevelRenderer.renderLineBox(poseStack, consumer,
                            vec3.x - (double)f, vec3.y, vec3.z - (double)f,
                            vec3.x + (double)f, vec3.y + (double)0.0625F, vec3.z + (double)f,
                            1.0F, 1.0F, 0.0F, 1.0F);
                }
            }
            // 5. 渲染视线向量
            renderVector(poseStack, consumer,new Vector3f(0.0F, entity.getEyeHeight(), 0.0F),entity.getViewVector(partialTicks).scale(2.0F),-16776961);
        }
    }


}