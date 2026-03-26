package com.fanxing.fx_undertale.utils;

import net.minecraft.network.FriendlyByteBuf;

public class ByteBufUtils {
    public static float[] readFloatArray(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        float[] array = new float[len];
        for (int i = 0; i < len; i++) {
            array[i] = buf.readFloat();
        }
        return array;
    }
    public static void writeFloatArray(FriendlyByteBuf buf, float[] arr) {
        buf.writeVarInt(arr.length);
        for (float f : arr) buf.writeFloat(f);
    }
}
