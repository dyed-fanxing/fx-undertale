package com.sakpeipei.undertale.registry;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.menu.GravitySelectionMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class MenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Undertale.MOD_ID);


    public static final Supplier<MenuType<GravitySelectionMenu>> GRAVITY_SELECTION_MENU = MENUS.register("gravity_selection",
                    () -> new MenuType<>(GravitySelectionMenu::new, FeatureFlags.DEFAULT_FLAGS));


    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
