package com.fanxing.fx_undertale.client.gui.screen;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.client.gui.component.GasterBlasterEffectPreview;
import com.fanxing.fx_undertale.entity.boss.sans.SansAi;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.lib.FxLib;
import com.fanxing.lib.client.gui.Placement;
import com.fanxing.lib.client.gui.component.*;
import com.fanxing.lib.client.gui.layout.FlexBoxLayout;
import com.fanxing.lib.client.gui.renderer.BackgroundRenderer;
import com.fanxing.lib.client.gui.screen.SimpleEditBoxScreen;
import com.fanxing.lib.client.gui.utils.Buttons;
import com.fanxing.lib.item.compoent.ColorPalette;
import com.fanxing.lib.net.packet.ColorPalettesPacket;
import com.fanxing.lib.net.packet.ColorSchemePacket;
import com.fanxing.lib.registry.DataComponentsFxLib;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GasterBlasterConfigScreen extends Screen {
    public static final int COLOR_SWATCH_SIZE = 20;
    public static final int PADDING = 5;
    public static final int TOP_HEIGHT = 250;
    // 数据副本
    protected ColorPicker colorPicker;
    protected RadioGroup<Integer, ColorSwatch> schemeGroup;
    protected RadioGroup<List<Integer>, ColorPaletteWidget> presetGroup;
    protected FlexBoxLayout topLayout;
    protected FlexBoxLayout presetLayout;
    protected HeaderAndFooterLayout rootLayout;
    protected GasterBlasterEffectPreview previewWidget;


    public GasterBlasterConfigScreen(ItemStack stack, GasterBlaster entity) {
        super(Component.literal("GB炮配置"));
        // 左侧面板（滑块和按钮）
        FlexBoxLayout colorPickerPanel = createColorPickerPanel(stack.get(DataComponentsFxLib.COLOR_SCHEME));
        // 2.2 实体预览
        previewWidget = new GasterBlasterEffectPreview(0, 0, 250, TOP_HEIGHT, entity, schemeGroup);
        Button savePreset = new Button.Builder(
                Component.translatable("selectWorld.edit.save").append(Component.translatable("gui.fx_lib.preset")),
                (t) -> savePresetPalettes()).build();
        rootLayout = new HeaderAndFooterLayout(this, TOP_HEIGHT, savePreset.getHeight());
        // 2.4 底部预设栏
        presetLayout = creatPresetColorPalette(Objects.requireNonNullElse(stack.get(DataComponentsFxLib.COLOR_PALETTES),new ArrayList<>()));

        topLayout = new FlexBoxLayout(width, TOP_HEIGHT).horizontal().justifyContent(FlexBoxLayout.JustifyContent.SPACE_AROUND).padding(PADDING, 0);
        topLayout.addChild(previewWidget);
        topLayout.addChild(colorPickerPanel);

        rootLayout.addToHeader(topLayout);
        rootLayout.addToContents(presetLayout);
        rootLayout.addToFooter(savePreset);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        rootLayout.arrangeElements();
        topLayout.setWidth(width);
        presetLayout.setWidth(width);
        presetLayout.setHeight(rootLayout.getContentHeight());
        rootLayout.arrangeElements();

        // 添加背景渲染器（放在子组件注册之前，确保背景先渲染）
        addRenderableOnly(BackgroundRenderer.textured(presetLayout, 0));
        rootLayout.visitWidgets(this::addRenderableWidget);
    }

    private FlexBoxLayout createColorPickerPanel(List<Integer> colors) {
        int width = 180;
        FlexBoxLayout layout = new FlexBoxLayout(width, 0).vertical().justifyContent(FlexBoxLayout.JustifyContent.SPACE_EVENLY).gap(8);
        // 色块单选组
        schemeGroup = new RadioGroup<>();
        FlexBoxLayout swatchBarLayout = new FlexBoxLayout(width, 0).justifyContent(FlexBoxLayout.JustifyContent.SPACE_EVENLY).gap(8);
        String[] labels = {"beam_inner", "beam_outer"};
        for (int i = 0; i < colors.size(); i++) {
            int color = colors.get(i);
            ColorSwatch swatch = new ColorSwatch(0, 0, 24, color, s -> {
                schemeGroup.select(s);
                colorPicker.setColor(s.getValue());
            }, Component.translatable("gui." + FxUndertale.MOD_ID + ".gb." + labels[i]));
            swatchBarLayout.addChild(swatch);
            schemeGroup.addOption(swatch);
        }
        schemeGroup.selectFirst();

        // 颜色选择器
        colorPicker = new ColorPicker(0, 0, width, width, schemeGroup.getSelected().getValue(), color -> schemeGroup.getSelected().setColor(color));
        FlexBoxLayout buttonLayout = new FlexBoxLayout(0, 0, width, 0).horizontal().justifyContent(FlexBoxLayout.JustifyContent.SPACE_EVENLY);
        int buttonWidth = width / 3 - 6;
        buttonLayout.addChild(new Button.Builder(Component.translatable("selectWorld.edit.save"), btn -> saveCurrentColor()).size(buttonWidth, 20).build());
        buttonLayout.addChild(new Button.Builder(Component.translatable("gui." + FxLib.MOD_ID + ".add").append(
                Component.translatable("gui." + FxLib.MOD_ID + ".preset")), btn -> savePresetColor()).size(buttonWidth, 20).build());
        buttonLayout.addChild(new Button.Builder(Component.translatable("controls.reset"), btn -> {
            List<ColorSwatch> options = schemeGroup.getOptions();
            for (int i = 0; i < options.size(); i++) {
                options.get(i).setColor(SansAi.ENERGY_AQUA.get(i));
            }
        }).size(buttonWidth, 20).build());
        layout.addChild(colorPicker);
        layout.addChild(swatchBarLayout);
        layout.addChild(buttonLayout);
        return layout;
    }

    private FlexBoxLayout creatPresetColorPalette(List<ColorPalette> palettes) {
        FlexBoxLayout layout = new FlexBoxLayout(width, 0).horizontal()
                .justifyContent(FlexBoxLayout.JustifyContent.CENTER)
                .alignItems(FlexBoxLayout.AlignItems.CENTER)
                .flexWrap(FlexBoxLayout.FlexWrap.WRAP)
                .padding(5, 5).gap(8);
        presetGroup = new RadioGroup<>();
        for (ColorPalette cp : palettes) {
            ColorPaletteWidget palette = createColorPaletteWidget(cp.label().getString(), cp.colors());
            presetGroup.addOption(palette);
            layout.addChild(createColorPaletteDeletePopup(palette));
        }
        return layout;
    }

    private void refreshPresetColorPaletteByGroup() {
        List<ColorPaletteWidget> options = presetGroup.getOptions();
        presetLayout.visitWidgets(this::removeWidget);
        presetLayout.getChildren().clear();
        presetGroup = new RadioGroup<>();
        for (ColorPaletteWidget palette : options) {
            presetGroup.addOption(palette);
            // 彩虹特殊判断
//            if (palette.getValue().stream().mapToInt(Integer::intValue).sum() < 0) presetLayout.addChild(palette);
            presetLayout.addChild(createColorPaletteDeletePopup(palette));
        }
        presetLayout.arrangeElements();
        presetLayout.visitWidgets(this::addRenderableWidget);
    }

    public InlinePopup<ColorPaletteWidget> createColorPaletteDeletePopup(ColorPaletteWidget palette) {
        return new InlinePopup<>(0, 0, palette, Buttons.delete(() -> {
            presetGroup.getOptions().remove(palette);
            refreshPresetColorPaletteByGroup();
        }), Placement.BOTTOM, 1);
    }

    protected ColorPaletteWidget createColorPaletteWidget(String name, List<Integer> colors) {
        return ColorPaletteWidget.horizontal(0, 0, COLOR_SWATCH_SIZE, COLOR_SWATCH_SIZE, colors, this::onPressColorPalette, Component.literal(name));
    }


    public void onPressColorPalette(ColorPaletteWidget pal) {
        List<Integer> colors = pal.getValue();
        List<ColorSwatch> options = schemeGroup.getOptions();
        for (int idx = 0; idx < colors.size(); idx++) {
            options.get(idx).setColor(colors.get(idx));
        }
        colorPicker.setColor(schemeGroup.getSelected().getValue());
        presetGroup.select(pal);
    }

    protected void saveCurrentColor() {
        PacketDistributor.sendToServer(new ColorSchemePacket(schemeGroup.getValues()));
        onClose();
    }

    private void savePresetColor() {
        minecraft.setScreen(new SimpleEditBoxScreen(this, Component.translatable("gui." + FxLib.MOD_ID + ".color_preset.tooltip"), this::doSavePreset));
    }

    private void doSavePreset(String name) {
        presetGroup.addOption(createColorPaletteWidget(name, schemeGroup.getValues()));
        //刷新预设栏
        refreshPresetColorPaletteByGroup();
    }

    private void savePresetPalettes() {
        PacketDistributor.sendToServer(new ColorPalettesPacket(presetGroup.getOptions().stream().map((t) -> new ColorPalette(t.getMessage(), t.getValue())).toList()));
        onClose();
    }


}