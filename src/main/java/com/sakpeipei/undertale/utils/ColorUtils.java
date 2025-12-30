package com.sakpeipei.undertale.utils;

/**
 * @author yujinbao
 * @since 2025/11/18 16:58
 */
public class ColorUtils {
    public static int[] getRGBA(int color) {
        return new int[] {
                (color >> 16) & 0xFF, // R
                (color >> 8) & 0xFF,  // G
                color & 0xFF,         // B
                (color >> 24) & 0xFF  // A
        };
    }
    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }
    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }
    public static int getBlue(int color) {
        return color & 0xFF;
    }
    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }
}
