package com.sakpeipei.undertale.event.handler;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.network.GasterBlasterBeamEndPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * @author Sakqiongzi
 * @since 2026-01-06 21:43
 * 重进客户端时
 */
@EventBusSubscriber(modid = Undertale.MODID)
public class EntityTrackerHandler {
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
    }
}