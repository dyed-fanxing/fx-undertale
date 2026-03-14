package com.fanxing.fx_undertale.common;


import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.common.damagesource.CustomNounVerbDeathMessageProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DeathMessageType;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.Supplier;

/**
 * @author FanXing
 * @since 2025-09-11 22:25
 * 枚举代理对象：传递目标枚举类class 和 构造函数的参数
 */
public class EnumParameters {
    // 创建静音 DamageEffects 的代理
    public static final EnumProxy<DamageEffects> SILENT = new EnumProxy<>(
            DamageEffects.class,
            FxUndertale.MOD_ID+":"+"silent",(Supplier<SoundEvent>) () -> null          // 返回 null，表示无音效
    );
    // 自定义名词动词死亡消息类型
    public static final EnumProxy<DeathMessageType> CUSTOM_NOUN_VERB_DEATH = new EnumProxy<>(
            DeathMessageType.class,
            FxUndertale.MOD_ID+":"+"custom_noun_verb_death",new CustomNounVerbDeathMessageProvider()
    );

}