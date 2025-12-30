package com.sakpeipei.undertale.client.render.item;

import com.sakpeipei.undertale.item.GasterBlasterFixedItem;
import com.sakpeipei.undertale.client.model.item.GasterBlasterItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GasterBlasterFixedItemRender extends GeoItemRenderer<GasterBlasterFixedItem> {
    public GasterBlasterFixedItemRender() {
        super(new GasterBlasterItemModel());
    }
}
