package com.fanxing.fx_undertale.utils;

import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

public class NbtUtils {
    public static ListTag newDoubleList(double... values) {
        ListTag listTag = new ListTag();
        for (double value : values) {
            listTag.add(DoubleTag.valueOf(value));
        }
        return listTag;
    }
    public static ListTag newDoubleList(Vec3 vec3) {
        ListTag listTag = new ListTag();
        listTag.add(DoubleTag.valueOf(vec3.x));
        listTag.add(DoubleTag.valueOf(vec3.y));
        listTag.add(DoubleTag.valueOf(vec3.z));
        return listTag;
    }
}
