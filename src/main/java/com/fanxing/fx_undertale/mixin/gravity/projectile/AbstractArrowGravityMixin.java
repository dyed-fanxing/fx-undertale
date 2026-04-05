package com.fanxing.fx_undertale.mixin.gravity.projectile;

import com.fanxing.fx_undertale.utils.GravityUtils;
import com.llamalad7.mixinextras.sugar.Local;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;


/**
 * 使用受重力影响的拥有者的位置
 * 由于发射的初始位置没有使用entity.getEyePosition()，导致必须要再写一次拥有者在重力方向上的眼睛世界位置
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowGravityMixin {
    @ModifyArgs(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;<init>(Lnet/minecraft/world/entity/EntityType;DDDLnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void setPosByOwnerEyePos(Args args, @Local(ordinal = 0,argsOnly = true)LivingEntity owner) {
        Direction gravity = owner.getData(AttachmentTypes.GRAVITY);
        if(gravity != Direction.DOWN){
            Vec3 eyePos = GravityUtils.localToWorld(gravity,0, owner.getEyeHeight()-0.1, 0).add(owner.position());
            args.set(1, eyePos.x);
            args.set(2, eyePos.y);
            args.set(3, eyePos.z);
        }
    }
}
