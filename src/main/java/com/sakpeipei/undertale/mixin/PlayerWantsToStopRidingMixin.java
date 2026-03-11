package com.sakpeipei.undertale.mixin;

import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerWantsToStopRidingMixin {
    @Unique
    private long undertale$lastShiftPressTime = 0;
    @Unique
    private boolean undertale$shiftWasPressed = false;
    @Unique
    private static final long DOUBLE_CLICK_TIME = 200;

    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    protected void wantsToStopRiding(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player)(Object) this;
        Entity vehicle = player.getVehicle();
        // 只处理骑GB的情况
        if (!(vehicle instanceof GasterBlaster)) {
            return;
        }

        boolean isShiftPressed = player.isShiftKeyDown();
        long now = System.currentTimeMillis();
        // 只在按键从松开变为按下时处理（上升沿触发）
        if (isShiftPressed && !undertale$shiftWasPressed) {
            if (now - undertale$lastShiftPressTime < DOUBLE_CLICK_TIME) {
                // 双击！允许下车
                cir.setReturnValue(true);
                cir.cancel();
                undertale$lastShiftPressTime = 0;
            } else {
                // 单击，记录时间但不允许下车
                undertale$lastShiftPressTime = now;
                cir.setReturnValue(false);
                cir.cancel();
            }
        } else {
            // 其他情况（按住不放、松开）都不允许下车
            cir.setReturnValue(false);
            cir.cancel();
        }
        // 更新按键状态
        undertale$shiftWasPressed = isShiftPressed;
    }
}
