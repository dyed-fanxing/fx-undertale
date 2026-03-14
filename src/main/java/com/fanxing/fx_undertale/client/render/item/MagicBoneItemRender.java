package com.fanxing.fx_undertale.client.render.item;

import com.fanxing.fx_undertale.client.model.item.MagicBoneItemModel;
import com.fanxing.fx_undertale.item.MagicBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MagicBoneItemRender extends GeoItemRenderer<MagicBone> {
    public MagicBoneItemRender() {
        super(new MagicBoneItemModel());
    }
}
