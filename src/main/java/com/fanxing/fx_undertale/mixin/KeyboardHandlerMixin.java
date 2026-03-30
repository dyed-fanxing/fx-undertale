package com.fanxing.fx_undertale.mixin;

import com.fanxing.fx_undertale.client.debug.OBBCCDDebugRenderer;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * KeyboardHandler Mixin - F3+~ 切换 OBB CCD 调试显示
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(
            method = "handleDebugKeys",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onHandleDebugKeysReturn(int keyCode, CallbackInfoReturnable<Boolean> cir) {
        // 如果 handleDebugKeys 返回 false（不是原版调试键），检查是否是 F3+~
        if (!cir.getReturnValue()) {
            long glfwWindow = Minecraft.getInstance().getWindow().getWindow();
            boolean f3Pressed = InputConstants.isKeyDown(glfwWindow, 292);
            
            if (f3Pressed && keyCode == 96) { // ~ 键
                // F3+~ 按下，切换 CCD 调试显示
                OBBCCDDebugRenderer.toggleCCDDebug();
                if (Minecraft.getInstance().player != null) {
                    // 使用和原版调试消息一样的格式：[调试]消息内容
                    Minecraft.getInstance().player.sendSystemMessage(
                            net.minecraft.network.chat.Component.empty()
                                    .append(net.minecraft.network.chat.Component.translatable("debug.prefix")
                                            .withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD))
                                    .append(net.minecraft.network.chat.CommonComponents.SPACE)
                                    .append(net.minecraft.network.chat.Component.translatable("fx_undertale.debug.ccd." + 
                                            (OBBCCDDebugRenderer.isShowCCDDebug() ? "on" : "off")))
                    );
                }
                // 返回 true，让 handledDebugKey 被设置为 true
                cir.setReturnValue(true);
            }
        }
    }
}