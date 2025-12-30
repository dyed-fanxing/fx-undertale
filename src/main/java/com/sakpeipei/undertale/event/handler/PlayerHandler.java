package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.Undertale;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * @author Sakqiongzi
 * @since 2025-10-19 21:40
 */
@EventBusSubscriber(modid = Undertale.MODID)
public class PlayerHandler {
    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Post event) {
    }
}
