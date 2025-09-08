package com.sakpeipei.mod.undertale.effect;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
/**
 * @author Sakqiongzi
 * @since 2025-09-08 22:08
 * KR业报管理
 */
public class KarmaManager {
    private static final double MAX_KR = 0.45;
    // NBT数据键名
    private static final String KARMA_VALUE_KEY = "karma_value";

    // 设置KR值（0-40）
    public static void setKarmaValue(LivingEntity entity, double value) {
        entity.getPersistentData().putDouble(KARMA_VALUE_KEY, Mth.clamp(value, 0, MAX_KR));
    }
    // 增加KR值
    public static void addKarmaValue(LivingEntity entity, double amount) {
        setKarmaValue(entity, getKarmaValue(entity) + amount);
    }
    // 减少KR值
    public static void subtractKarmaValue(LivingEntity entity, double amount) {
        setKarmaValue(entity, getKarmaValue(entity) - amount);
    }
    private static double getKarmaValue(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.contains(KARMA_VALUE_KEY)) {
            return persistentData.getDouble(KARMA_VALUE_KEY);
        }else{
            persistentData.putDouble(KARMA_VALUE_KEY,0);
            return 0;
        }
    }

    // 移除KR
    public static void removeKarma(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.contains(KARMA_VALUE_KEY)) {
            persistentData.remove(KARMA_VALUE_KEY);
        }
    }

    // 检查是否有Karma数据
    public static boolean hasKarma(LivingEntity entity) {
        return entity.getPersistentData().contains(KARMA_VALUE_KEY);
    }
}