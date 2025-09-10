//package com.sakpeipei.mod.undertale.client.gui;
//
//import net.minecraft.client.gui.Gui;
//import net.minecraft.resources.ResourceLocation;
//import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
//
///**
// * @author Sakqiongzi
// * @since 2025-09-10 18:59
// */
//public class CustomHeartTypes {
//
//    // 扩展紫色心类型
//    public static final Gui.HeartType PURPLE = EnumHelper..fromOptional() IExtensibleEnum.create(
//            Gui.HeartType.class,
//            "PURPLE",
//            // 普通模式纹理
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/purple_full"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/purple_full_blinking"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/purple_half"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/purple_half_blinking"),
//            // 硬核模式纹理
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/purple_hardcore_full"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/purple_hardcore_full_blinking"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/purple_hardcore_half"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/purple_hardcore_half_blinking")
//    );
//
//    // 您还可以创建其他颜色
//    public static final Gui.HeartType BLUE = IExtensibleEnum.create(
//            Gui.HeartType.class,
//            "BLUE",
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/blue_full"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/blue_full_blinking"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/blue_half"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/blue_half_blinking"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/blue_hardcore_full"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/blue_hardcore_full_blinking"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/blue_hardcore_half"),
//            ResourceLocation.fromNamespaceAndPath("yourmod", "hud/heart/blue_hardcore_half_blinking")
//    );
//}