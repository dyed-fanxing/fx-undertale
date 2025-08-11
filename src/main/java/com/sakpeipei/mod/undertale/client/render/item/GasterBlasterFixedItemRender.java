package com.sakpeipei.mod.undertale.client.render.item;

import com.sakpeipei.mod.undertale.item.GasterBlasterFixedItem;
import com.sakpeipei.mod.undertale.client.model.item.GasterBlasterItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GasterBlasterFixedItemRender extends GeoItemRenderer<GasterBlasterFixedItem> {
    public GasterBlasterFixedItemRender() {
        super(new GasterBlasterItemModel());
    }
}
