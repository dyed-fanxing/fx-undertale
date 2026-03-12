package com.fanxing.fx_undertale.client.screen;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.common.phys.LocalDirection;
import com.fanxing.fx_undertale.menu.GravitySelectionMenu;
import com.fanxing.fx_undertale.net.packet.GravitySelectionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class GravitySelectionScreen extends AbstractContainerScreen<GravitySelectionMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "textures/gui/gravity_selection.png");

    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int START_X = 20;
    private static final int START_Y = 20;
    private static final int COLUMN_WIDTH = 70;
    private static final int ROW_HEIGHT = 30;

    public GravitySelectionScreen(GravitySelectionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        addDirectionButton(LocalDirection.UP, Component.translatable("direction." + FxUndertale.MOD_ID + ".up"), 0, 0);
        addDirectionButton(LocalDirection.DOWN, Component.translatable("direction." + FxUndertale.MOD_ID + ".down"), 0, 1);
        addDirectionButton(LocalDirection.LEFT, Component.translatable("direction." + FxUndertale.MOD_ID + ".left"), 1, 0);
        addDirectionButton(LocalDirection.RIGHT, Component.translatable("direction." + FxUndertale.MOD_ID + ".right"), 1, 1);
        addDirectionButton(LocalDirection.FRONT, Component.translatable("direction." + FxUndertale.MOD_ID + ".front"), 2, 0);
        addDirectionButton(LocalDirection.BACK, Component.translatable("direction." + FxUndertale.MOD_ID + ".back"), 2, 1);
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui." + FxUndertale.MOD_ID + ".close"),
                button -> this.onClose()
        ).bounds(this.leftPos + this.imageWidth - 60, this.topPos + this.imageHeight - 30, 50, 20).build());
    }

    private void addDirectionButton(LocalDirection direction, Component text, int column, int row) {
        int x = this.leftPos + START_X + column * COLUMN_WIDTH;
        int y = this.topPos + START_Y + row * ROW_HEIGHT;

        this.addRenderableWidget(Button.builder(text, button -> {
            PacketDistributor.sendToServer(new GravitySelectionPacket(direction));
            System.out.println("发送方向: " + direction.name());
            this.onClose();
        }).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui." + FxUndertale.MOD_ID + ".select_direction"),
                8, 6, 0x404040, false);
    }
}