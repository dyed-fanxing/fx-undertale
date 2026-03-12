package com.fanxing.fx_undertale.client.model.item;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.item.GasterBlasterItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class GasterBlasterItemModel extends DefaultedItemGeoModel<GasterBlasterItem> {
    public GasterBlasterItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "gaster_blaster"));
    }
}
