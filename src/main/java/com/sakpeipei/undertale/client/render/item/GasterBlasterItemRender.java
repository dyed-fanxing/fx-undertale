package com.sakpeipei.undertale.client.render.item;

import com.sakpeipei.undertale.item.GasterBlasterItem;
import com.sakpeipei.undertale.client.model.item.GasterBlasterItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GasterBlasterItemRender extends GeoItemRenderer<GasterBlasterItem> {
    public GasterBlasterItemRender() {
        super(new GasterBlasterItemModel());
    }
}
