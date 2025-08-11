package com.sakpeipei.mod.undertale.client.model.item;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.item.GasterBlasterFixedItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class GasterBlasterItemModel extends DefaultedItemGeoModel<GasterBlasterFixedItem> {
    public GasterBlasterItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "gaster_blaster"));
    }
}
