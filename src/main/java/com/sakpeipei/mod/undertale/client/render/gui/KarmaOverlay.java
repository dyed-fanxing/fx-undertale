package com.sakpeipei.mod.undertale.client.render.gui;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
/**
 * @author Sakqiongzi
 * @since 2025-09-09 23:23
 */
@OnlyIn(Dist.CLIENT)
public class KarmaOverlay implements IGuiOverlay {
    private static final ResourceLocation KARMA_ICONS = new ResourceLocation("undertale", "textures/gui/karma_icons.png");

    @Override
    public void render(ExtendedGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.options.hideGui) return;

        // 检查玩家是否有KR效果
        player.getEffect(com.sakpeipei.mod.undertale.registry.MobEffectRegistry.KARMA.get()).ifPresent(effect -> {
            if (effect instanceof com.sakpeipei.mod.undertale.effect.KarmaMobEffectInstance karmaEffect) {
                renderKarmaOverlay(guiGraphics, screenWidth, screenHeight, karmaEffect.getValue());
            }
        });
    }

    private void renderKarmaOverlay(GuiGraphics guiGraphics, int screenWidth, int screenHeight, byte karmaValue) {
        if (karmaValue <= 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        int left = screenWidth / 2 + 91; // HP栏右侧
        int top = screenHeight - 39; // 与HP栏对齐

        RenderSystem.enableBlend();

        // 绘制KR背景图标
        guiGraphics.blit(KARMA_ICONS, left, top, 0, 0, 9, 9);

        // 根据KR值绘制叠加层数
        int layers = Math.min((karmaValue + 9) / 10, 4); // 计算层数（1-4）
        for (int i = 0; i < layers; i++) {
            int yOffset = -i * 2; // 每层向上偏移2像素
            guiGraphics.blit(KARMA_ICONS, left, top + yOffset, 9, 0, 9, 9);
        }

        // 绘制KR数值文本
        String text = String.valueOf(karmaValue);
        guiGraphics.drawString(minecraft.font, text, left + 12, top + 1, 0xFF8000FF, false);

        RenderSystem.disableBlend();
    }
}