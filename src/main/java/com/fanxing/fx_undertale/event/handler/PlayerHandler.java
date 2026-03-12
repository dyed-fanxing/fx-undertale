package com.fanxing.fx_undertale.event.handler;

import com.mojang.logging.LogUtils;
import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author FanXing
 * @since 2025-10-19 21:40
 */
@EventBusSubscriber(modid = FxUndertale.MOD_ID)
public class PlayerHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerHandler.class);
    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
    }
    @SubscribeEvent
    public static void onPlayerTickPos(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
    }
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {

    }
    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
    }


    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        LogUtils.getLogger().info("玩家数据{}",event.getEntity().getPersistentData());
    }


}
