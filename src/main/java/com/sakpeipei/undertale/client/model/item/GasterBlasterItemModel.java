package com.sakpeipei.undertale.client.model.item;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.item.GasterBlasterItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class GasterBlasterItemModel extends DefaultedItemGeoModel<GasterBlasterItem> {
    public GasterBlasterItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "gaster_blaster"));
    }
}
