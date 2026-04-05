package com.fanxing.fx_undertale.mixin.gravity.projectile;

import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;


/**
 * 使用受重力影响的拥有者的位置
 * 由于发射的初始位置没有使用entity.getEyePosition()，导致必须要再写一次拥有者在重力方向上的眼睛世界位置
 */
@Mixin(WindChargeItem.class)
public abstract class WindChargeItemGravityMixin {
    @ModifyArgs(method = "use",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/windcharge/WindCharge;<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;DDD)V"))
    private void setPosByOwnerEyePos(Args args){
        Player player = args.get(0);
        Direction gravity = player.getData(AttachmentTypes.GRAVITY);
        if(gravity != Direction.DOWN){
            Vec3 eyePosition = player.getEyePosition();
            args.set(2, eyePosition.x());
            args.set(3, eyePosition.y());
            args.set(4, eyePosition.z());
        }
    }
}
