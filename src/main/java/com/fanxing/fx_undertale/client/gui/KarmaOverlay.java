package com.fanxing.fx_undertale.client.gui;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * KR
 */
@EventBusSubscriber(modid = FxUndertale.MOD_ID, value = Dist.CLIENT)
public class KarmaOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        if(player.getData(AttachmentTypes.KARMA_TAG)){
            GuiGraphics guiGraphics = event.getGuiGraphics();
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            // 血条位置（参考原版 Gui#renderHealthLevel）
            String text = "KR";

            int textWidth = mc.font.width(text);
            int bloodBarX = width / 2 - 88;          // 血条左边界
            int y = height - 37;              // 血条顶部 Y 坐标
            // KR 显示在血条左侧，间距 4 像素，与血条顶部对齐
            int color = 0xFFFFFF;
            int x = bloodBarX - textWidth - 2;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
            guiGraphics.pose().translate(-x, -y, 0);
            guiGraphics.drawString(mc.font, text, x, y, color);
            guiGraphics.pose().popPose();
        }
    }

}