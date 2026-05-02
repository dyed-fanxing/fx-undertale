package com.fanxing.fx_undertale.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import static com.fanxing.fx_undertale.FxUndertale.MOD_ID;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyBindings {
    public static final String KEY_CATEGORY = "key.category." + MOD_ID;
    public static final KeyMapping GASTER_BLASTER_CONFIG = new KeyMapping(
            "key." + MOD_ID + ".gaster_blaster.config",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_GRAVE,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GASTER_BLASTER_CONFIG);
    }
}