package com.fanxing.fx_undertale.mixin.gravity.projectile;

import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 使用受重力影响的拥有者的角度和位置
 * 局部 → 世界
 */
@Mixin(FishingHook.class)
public abstract class FishingHookGravityMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V", at = @At("RETURN"))
    private void setPosAndDeltaMovementByOwnerEyePosAndRot(Player player, Level level, int p_37108_, int p_37109_, CallbackInfo ci) {
        Direction gravity = player.getData(AttachmentTypes.GRAVITY);
        if (gravity != Direction.DOWN) {
            FishingHook hook = (FishingHook) (Object) this;
            // 1. 世界空间的视线和眼睛位置
            Vec3 look = player.getViewVector(1.0F);
            Vec3 eyePos = player.getEyePosition(1.0F);
            // 2. 发射位置：眼睛 + 视线方向 * 0.3
            Vec3 spawnPos = eyePos.add(look.scale(0.3));
            // 3. 世界角度
            float worldYaw = (float) (Mth.atan2(look.x, look.z) * (180.0F / Math.PI));
            float worldPitch = (float) (Mth.atan2(-look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * (180.0F / Math.PI));
            // 4. 强制覆盖所有属性！
            hook.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, worldYaw, worldPitch);
            hook.setDeltaMovement(look.scale(0.6));
            // 5. 同步角度
            hook.yRotO = worldYaw;
            hook.xRotO = worldPitch;
        }
    }
}