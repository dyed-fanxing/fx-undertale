package com.sakpeipei.mod.undertale.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Sakqiongzi
 * @since 2025-09-28 01:12
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    private static final Logger log = LogManager.getLogger(EntityRenderDispatcherMixin.class);

    @Inject(method = "render", at = @At("HEAD"))
    public <E extends Entity> void onRenderStart(E entity, double xa, double ya, double za, float p_114389_, float p_114390_, PoseStack p_114391_, MultiBufferSource p_114392_, int p_114393_, CallbackInfo ci) {
        if(entity instanceof FlyingBone bone){
            // 打印实体渲染信息
//            log.info("空间位置变换:({},{},{}) " ,xa,ya, za);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public <E extends Entity> void onRenderEnd(E entity, double camX, double camY, double camZ, float partialTick, float yaw, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if(entity instanceof FlyingBone){
        }
    }
}