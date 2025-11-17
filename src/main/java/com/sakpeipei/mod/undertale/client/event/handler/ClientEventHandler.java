package com.sakpeipei.mod.undertale.client.event.handler;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.client.render.overlay.WarningTipRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * @author yujinbao
 * @since 2025/11/17 16:13
 * 客户端事件处理器
 */
@EventBusSubscriber(modid = Undertale.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    @SubscribeEvent
    private static void onClientTick(ClientTickEvent event) {
        // 每帧更新所有预警的计时器
        WarningTipRenderer.tickAll();
    }
    @SubscribeEvent
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 判断渲染阶段 - 在粒子效果之后渲染
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            WarningTipRenderer.renderAllWarnings(event);
        }
    }
}
