package com.sakpeipei.undertale.mixin.roll;

import com.sakpeipei.undertale.entity.IRollable;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 添加roll变量的相关判断和处理
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerRollMixin {
    @Unique
    private float undertale$rollLast;
    @Unique
    public float undertale$getRollLast() {
        return undertale$rollLast;
    }

    public void setRollLast(float rollLast) {
        this.undertale$rollLast = rollLast;
    }

    @ModifyVariable(method = "sendPosition", at = @At(value = "STORE"),ordinal = 2)
    private boolean flag2InSendPosition(boolean flag2) {
        return flag2 || ((IRollable)this).undertale$getRoll() != undertale$rollLast;
    }
    @Inject(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F",shift = At.Shift.AFTER, ordinal = 4))
    private void rollLastInSendPositionAfterGetXRot(CallbackInfo ci) {
        this.undertale$rollLast = ((IRollable)this).undertale$getRoll();
    }
}
