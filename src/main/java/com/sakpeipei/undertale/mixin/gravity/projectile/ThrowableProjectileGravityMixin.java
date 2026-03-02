package com.sakpeipei.undertale.mixin.gravity.projectile;

import com.llamalad7.mixinextras.sugar.Local;
import com.sakpeipei.undertale.entity.attachment.Gravity;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * 使用受重力影响的拥有者的位置
 * 由于发射的初始位置没有使用entity.getEyePosition()，导致必须要再写一次拥有者在重力方向上的眼睛世界位置
 */
@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileGravityMixin {
    @ModifyArgs(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;<init>(Lnet/minecraft/world/entity/EntityType;DDDLnet/minecraft/world/level/Level;)V"))
    private static void setPosByOwnerEyePos(Args args, @Local(ordinal = 0, argsOnly = true) LivingEntity owner) {
        Gravity data = owner.getData(AttachmentTypes.GRAVITY);
        if(data.getGravity() != Direction.DOWN){
            Vec3 eyePos = data.localToWorld(0, owner.getEyeHeight()-0.1, 0).add(owner.position());
            args.set(1, eyePos.x);
            args.set(2, eyePos.y);
            args.set(3, eyePos.z);
        }
    }
}
