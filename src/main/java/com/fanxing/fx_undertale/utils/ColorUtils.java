package com.fanxing.fx_undertale.utils;

import net.minecraft.util.FastColor;

public class ColorUtils {
    public static int rgbaArrayToInt(int[] color) {
        return FastColor.ARGB32.color(color[3],color[0], color[1], color[2]);
    }
    public static int[] rgbaArrayToInt(int[][] color) {
        if (color == null) return null;
        int[] result = new int[color.length];
        for (int i = 0; i < color.length; i++) {
            result[i] = FastColor.ARGB32.color(color[i][3],color[i][0], color[i][1], color[i][2]);
        }
        return result;
    }

    public static int[] intToRGBArray(int color) {
        return new int[]{FastColor.ARGB32.red(color),FastColor.ARGB32.green(color),FastColor.ARGB32.blue(color),FastColor.ARGB32.alpha(color)};
    }
    public static int[][] intToRGBArray(int[] color) {
        if (color == null) return null;
        int[][] result = new int[color.length][];
        for (int i = 0; i < color.length; i++) {
            result[i] = intToRGBArray(color[i]);
        }
        return result;
    }
    public static int[][] intToRGBArrays(int... color) {
        return intToRGBArray(color);
    }

    public static float[] intToFloat(int[] color){
        return new float[]{color[0]/255F, color[1]/255F, color[2]/255F, color[3]/255F};
    }
    // 将二维 int 颜色数组转换为二维 float 数组
    public static float[][] intToFloat(int[][] color) {
        if (color == null) return null;
        float[][] result = new float[color.length][];
        for (int i = 0; i < color.length; i++) {
            result[i] = intToFloat(color[i]);
        }
        return result;
    }

}
