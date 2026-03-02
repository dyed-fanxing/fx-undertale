package com.sakpeipei.undertale.client.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sakpeipei.undertale.Config;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.attachment.PlayerSoul;
import com.sakpeipei.undertale.entity.persistentData.SoulMode;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 决心值 HUD 渲染类
 * 在快捷栏右侧显示决心图标，包含从下往上的填充动画，
 * 图标颜色随 LV 等级从红色渐变为黑色（屠杀污染），
 * 并支持心形切换时的弹性放大动画。
 */
@EventBusSubscriber(modid = Undertale.MOD_ID, value = Dist.CLIENT)
public class SoulOverlay {
    // 图标基础尺寸（像素）
    private static final int ICON_SIZE = 16;


    public static final ResourceLocation[] RED = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "textures/gui/soul_outline_red.png"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "textures/gui/soul_fill_red.png")
    };
    public static final ResourceLocation[] BLUE = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "textures/gui/soul_outline_blue.png"),
            ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "textures/gui/soul_fill_blue.png")
    };

    // 动画状态
    private static long animationStartTime = 0;          // 动画开始时间（毫秒）
    private static final long ANIMATION_DURATION = 300;  // 动画持续时间（毫秒）
    private static byte lastSoulMode;
    public static ResourceLocation[] getSoulTexture(byte mode){
        return switch (mode){
            case SoulMode.GRAVITY -> BLUE;
            default -> RED;
        };
    }


    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;

        // 从玩家数据获取决心值和 LV
        PlayerSoul soulData = player.getData(AttachmentTypes.PLAYER_SOUL.get());
        int determination = Math.min(100, Math.max(0, soulData.getDetermination()));
        int level = soulData.getLevel();
        int maxLevel = Config.SERVER.maxLv.get();
        if (maxLevel <= 0) maxLevel = 20; // 安全后备

        // 计算颜色变暗因子 t：LV=0 时 t=1.0（最亮），LV=max 时 t=0.0（全黑）
        float t = 1.0f - Math.min(1.0f, (float) level / maxLevel);

        byte soulMode = player.getData(AttachmentTypes.SOUL_MODE);
        ResourceLocation[] soulTexture = getSoulTexture(soulMode);
        if(soulMode != lastSoulMode){
            lastSoulMode = soulMode;
            animationStartTime = System.currentTimeMillis();
        }
        // 计算动画缩放因子（使用弹性缓动 easeOutBack）
        float scale = 1.0f;
        double elapsed = System.currentTimeMillis() - animationStartTime;
        if(elapsed < ANIMATION_DURATION){
            scale = 1.0f + 0.3f * easeOutBack((float) elapsed / ANIMATION_DURATION); // 放大0.3倍
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // 计算快捷栏右侧位置（与之前一致）
        int x = width / 2 + 95;
        int y = height - 22 + (20 - ICON_SIZE) / 2;

        int fillHeight = ICON_SIZE * determination / 100;

        // 获取矩阵栈并应用变换（以图标中心缩放）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        // 平移到图标中心，缩放，再平移回左上角（因为后续绘制以左上角为原点）
        poseStack.translate(x + ICON_SIZE / 2f, y + ICON_SIZE / 2f, 0);
        poseStack.scale(scale, scale, 1.0f);
        poseStack.translate(-ICON_SIZE / 2f, -ICON_SIZE / 2f, 0);


        // 绘制填充（从底部向上裁剪）
        if (fillHeight > 0) {
            RenderSystem.setShaderColor(t, t, t, 1.0f);
            guiGraphics.blit(soulTexture[0],
                    0, ICON_SIZE - fillHeight,          // 目标位置（左上角为原点）
                    ICON_SIZE, fillHeight,                                 // 目标尺寸
                    0, ICON_SIZE - fillHeight,                   // 纹理起始坐标
                    ICON_SIZE, fillHeight,                                 // 纹理区域尺寸
                    ICON_SIZE, ICON_SIZE);                                 // 纹理整体尺寸
        }

        // 绘制轮廓
        RenderSystem.setShaderColor(t, t, t, 1.0f);
        guiGraphics.blit(soulTexture[1],
                0, 0,
                ICON_SIZE, ICON_SIZE,
                0, 0,
                ICON_SIZE, ICON_SIZE,
                ICON_SIZE, ICON_SIZE);

        poseStack.popPose();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // 重置颜色

        // 可选：按住 Shift 时显示决心数值（不受缩放影响）
        if (player.isShiftKeyDown()) {
            int textX = x + ICON_SIZE + 2;
            int textY = y + (ICON_SIZE - mc.font.lineHeight) / 2;
            guiGraphics.drawString(mc.font, String.valueOf(determination), textX, textY, 0xFFFFFF);
        }
    }

    /**
     * 弹性缓动函数 easeOutBack
     * @param t 进度 0~1
     * @return 缩放系数（1.0 + 返回值为实际倍数）
     */
    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }
}