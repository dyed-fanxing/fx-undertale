package com.sakpeipei.undertale.mixin.gravity.projectile;

import com.llamalad7.mixinextras.sugar.Local;
import com.sakpeipei.undertale.entity.attachment.Gravity;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;


/**
 * 使用受重力影响的拥有者的局部角度，向量
 * 局部 → 世界
 */
@Mixin(Projectile.class)
public abstract class ProjectileGravityMixin {

    @ModifyArgs(method = "shootFromRotation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"))
    public void shootFromRotation(Args args, @Local(ordinal = 0, argsOnly = true) Entity shooter) {
        Gravity data = shooter.getData(AttachmentTypes.GRAVITY);
        if (data.getGravity() != Direction.DOWN) {
            Vec3 shootVec3 = data.localToWorld((double) args.get(0), args.get(1), args.get(2));
            args.set(0, shootVec3.x);
            args.set(1, shootVec3.y);
            args.set(2, shootVec3.z);
        }
    }
}
