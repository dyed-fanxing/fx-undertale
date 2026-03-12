package com.fanxing.fx_undertale.client.gui;

import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.resources.ResourceLocation;

/**
 * @author FanXing
 * @since 2025-09-14 18:58
 */
public class KaramHeartType {
    private static final ResourceLocation full = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/karma_full");
    private static final ResourceLocation fullBlinking = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/karma_full_blinking");

    private static final ResourceLocation leftHalf = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/left_karma_half");
    private static final ResourceLocation leftHalfBlinking = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/left_karma_half_blinking");

    private static final ResourceLocation rightHalf = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/right_karma_half");
    private static final ResourceLocation rightHalfBlinking = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/right_karma_half_blinking");



    private static final ResourceLocation hardcoreFull = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/karma_hardcore_full");
    private static final ResourceLocation hardcoreFullBlinking = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/karma_hardcore_full_blinking");

    private static final ResourceLocation leftHardcoreHalf = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/left_karma_hardcore_half");
    private static final ResourceLocation leftHardcoreHalfBlinking = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/left_karma_hardcore_half_blinking");

    private static final ResourceLocation rightHardcoreHalf = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/right_karma_hardcore_half");
    private static final ResourceLocation rightHardcoreHalfBlinking = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/heart/right_karma_hardcore_half_blinking");
    /**
     * 获取纹理
     * @param isHardCore 困难模式
     * @param isBlinking 闪烁
     * @param isHalf 半心
     * @param isRight 右半心，只有在isHalf为true的情况下，最后半颗
     */
    public static ResourceLocation getSprite(boolean isHardCore, boolean isBlinking, boolean isHalf, boolean isRight) {
        if(isHardCore){
            if(isHalf){
                return isBlinking? leftHardcoreHalfBlinking : leftHardcoreHalf;
            }else{
                if(isRight){
                    return isBlinking ? rightHardcoreHalfBlinking : rightHardcoreHalf;

                }else{
                    return isBlinking ? hardcoreFullBlinking : hardcoreFull;
                }
            }
        }else if (isHalf) {
            return isBlinking? leftHalfBlinking : leftHalf;
        }else{
            if (isRight){
                return isBlinking? rightHalfBlinking : rightHalf;
            }else{
                return isBlinking ? fullBlinking : full;
            }
        }
    }
}
