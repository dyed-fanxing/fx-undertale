package com.sakpeipei.undertale.client.model.item;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.item.GasterBlasterProItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class GasterBlasterProItemModel extends DefaultedItemGeoModel<GasterBlasterProItem> {
    public GasterBlasterProItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "gaster_blaster"));
    }
}
