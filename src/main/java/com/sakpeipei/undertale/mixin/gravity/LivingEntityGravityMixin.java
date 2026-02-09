package com.sakpeipei.undertale.mixin.gravity;

import com.llamalad7.mixinextras.sugar.Local;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin {
    @Shadow
    @Final
    protected static EntityDimensions SLEEPING_DIMENSIONS;

    @Shadow
    protected abstract EntityDimensions getDefaultDimensions(Pose p_316700_);

    private static final Logger log = LoggerFactory.getLogger(LivingEntityGravityMixin.class);

    @Inject(method = "jumpFromGround", at = @At("TAIL"), cancellable = true)
    private void jumpFromGround(CallbackInfo ci, @Local(ordinal = 0) float jumpPower) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) {
            log.info("跳跃强度：{}", jumpPower);
        }
    }
}