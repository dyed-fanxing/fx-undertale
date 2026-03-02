package com.sakpeipei.undertale.common;


import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageEffects;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.Supplier;

/**
 * @author Sakqiongzi
 * @since 2025-09-11 22:25
 */
public class EnumParameters {
    // 创建静音 DamageEffects 的代理
    public static final EnumProxy<DamageEffects> SILENT = new EnumProxy<>(
            DamageEffects.class,                        // 目标枚举类
            "undertale:silent",             // 命名空间格式的名称
            (Supplier<SoundEvent>) () -> null           // 返回 null，表示无音效
    );
}