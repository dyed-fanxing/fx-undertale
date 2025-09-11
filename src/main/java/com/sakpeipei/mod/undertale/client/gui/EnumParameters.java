package com.sakpeipei.mod.undertale.client.gui;


import com.sakpeipei.mod.undertale.Undertale;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.minecraft.client.gui.Gui.HeartType;
/**
 * @author Sakqiongzi
 * @since 2025-09-11 22:25
 */
public class EnumParameters {

    public static final EnumProxy<HeartType> KARMA_HEART_PROXY = new EnumProxy<>(
            HeartType.class, // 第一个参数必须是目标枚举类
            // 后续参数按顺序对应构造函数的参数
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "hud/heart/karma_full"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "hud/heart/karma_full_blinking"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "hud/heart/karma_half"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "hud/heart/karma_half_blinking"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "hud/heart/karma_hardcore_full"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "hud/heart/karma_hardcore_full_blinking"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "hud/heart/karma_hardcore_half"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "hud/heart/karma_hardcore_half_blinking")
    );
}