package com.sakpeipei.undertale.event.handler;

import com.mojang.logging.LogUtils;
import com.sakpeipei.undertale.Undertale;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * @author Sakqiongzi
 * @since 2025-10-19 21:40
 */
@EventBusSubscriber(modid = Undertale.MOD_ID)
public class PlayerHandler {
    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Post event) {
    }
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
    }
    @SubscribeEvent
    public static void onPlayer(PlayerEvent.PlayerLoggedInEvent event) {
//        event.getEntity().getPersistentData().remove(GasterBlasterProItem.GASTER_BLASTER_PRO_KEY);
//        LOGGER.info("玩家数据移除Pro后{}",event.getEntity().getPersistentData());
        LogUtils.getLogger().info("玩家数据{}",event.getEntity().getPersistentData());

    }
}
