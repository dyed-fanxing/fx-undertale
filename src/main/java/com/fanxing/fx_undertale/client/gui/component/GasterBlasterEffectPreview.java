package com.fanxing.fx_undertale.client.gui.component;

import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import com.fanxing.lib.client.gui.component.ColorSwatch;
import com.fanxing.lib.client.gui.component.LivingEntityPreview;
import com.fanxing.lib.client.gui.component.RadioGroup;
import net.minecraft.client.gui.GuiGraphics;

public class GasterBlasterEffectPreview extends LivingEntityPreview<GasterBlaster> {
    protected RadioGroup<Integer, ColorSwatch> schemeGroup;
    public GasterBlasterEffectPreview(int x, int y, int width, int height, GasterBlaster entity, RadioGroup<Integer, ColorSwatch> schemeGroup) {
        super(x, y, width, height, entity);
        this.schemeGroup = schemeGroup;
    }
    @Override
    protected void preRender(GuiGraphics graphics, float partialTick) {
        entity.colors(schemeGroup.getValues());
    }

    @Override
    protected void afterRender(GuiGraphics graphics, float partialTick) {
//        GasterBlasterBeamRenderer.render(entity, graphics.pose(), graphics.bufferSource(), partialTick,schemeGroup.getValues());
    }
}
