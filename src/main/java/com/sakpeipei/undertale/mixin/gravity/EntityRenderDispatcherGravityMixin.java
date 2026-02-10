package com.sakpeipei.undertale.mixin.gravity;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.utils.CoordsUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
        GravityData data = entity.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN) {
            return offset;
        }
        return CoordsUtils.transform(offset, data.getLogicToWorld());
    }
}