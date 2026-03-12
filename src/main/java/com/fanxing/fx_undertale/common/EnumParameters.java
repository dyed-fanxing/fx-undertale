package com.fanxing.fx_undertale.common;


import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageEffects;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.Supplier;

/**
 * @author FanXing
 * @since 2025-09-11 22:25
 */
public class EnumParameters {
    // 创建静音 DamageEffects 的代理
    public static final EnumProxy<DamageEffects> SILENT = new EnumProxy<>(
            DamageEffects.class,
            FxUndertale.MOD_ID+":silent",             // 命名空间格式的名称
            (Supplier<SoundEvent>) () -> null           // 返回 null，表示无音效
    );
}