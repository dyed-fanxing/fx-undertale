package com.sakpeipei.mod.undertale.client.render.item;

import com.sakpeipei.mod.undertale.client.model.item.GasterBlasterProItemModel;
import com.sakpeipei.mod.undertale.item.GasterBlasterProItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GasterBlasterProItemRender extends GeoItemRenderer<GasterBlasterProItem> {
    public GasterBlasterProItemRender() {
        super(new GasterBlasterProItemModel());
    }
}
