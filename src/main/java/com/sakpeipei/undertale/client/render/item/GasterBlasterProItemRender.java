package com.sakpeipei.undertale.client.render.item;

import com.sakpeipei.undertale.client.model.item.GasterBlasterProItemModel;
import com.sakpeipei.undertale.item.GasterBlasterProItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GasterBlasterProItemRender extends GeoItemRenderer<GasterBlasterProItem> {
    public GasterBlasterProItemRender() {
        super(new GasterBlasterProItemModel());
    }
}
