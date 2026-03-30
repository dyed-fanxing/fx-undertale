package com.fanxing.fx_undertale.mixin;

import com.fanxing.fx_undertale.entity.summon.RotationBone;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityBlockMixin {

    @Inject(method = "isDamageSourceBlocked", at = @At("HEAD"), cancellable = true)
    private void onIsDamageSourceBlocked(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        // 检查是否是 RotationBone 的伤害
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity instanceof RotationBone rotationBone) {
            if (!damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD) && self.isBlocking()) {
                Vec3 sourcePosition = damageSource.getSourcePosition();
                if (sourcePosition != null) {
                    Vec3 collisionToEntity = self.position().subtract(sourcePosition).normalize();
                    // 包含仰附的整个视线前方，而原版忽略了pitch，只能检测水平前方
                    Vec3 entityLook = self.getViewVector(1.0f);
                    double dotProduct = collisionToEntity.x * entityLook.x +
                                       collisionToEntity.y * entityLook.y +
                                       collisionToEntity.z * entityLook.z;
                    if (dotProduct < 0.0) {
                        cir.setReturnValue(true);
                        cir.cancel();
                    }
                }
            }
        }
    }
}