package com.fanxing.fx_undertale.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

    /**
     * 将 src 中的所有键值对合并到 dest 中，src 的键会覆盖 dest 的同名键
     * @param dest 目标 NBT（会被修改）
     * @param src  源 NBT
     */
    public static CompoundTag merge(CompoundTag dest, CompoundTag src) {
        for (String key : src.getAllKeys()) {
            Tag tag = src.get(key);
            if (tag != null) {
                dest.put(key, tag.copy()); // 复制一份避免引用问题
            }
        }
        return dest;
    }

    /**
     * 创建一个新的 CompoundTag，先复制 base，再被 override 覆盖
     * @param base 基础 NBT
     * @param override 覆盖 NBT
     * @return 合并后的新 NBT
     */
    public static CompoundTag mergeIntoNew(CompoundTag base, CompoundTag override) {
        CompoundTag result = base.copy();
        merge(result, override);
        return result;
    }
}
