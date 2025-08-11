package com.sakpeipei.mod.undertale.client.model.item;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.item.GasterBlasterProItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class GasterBlasterProItemModel extends DefaultedItemGeoModel<GasterBlasterProItem> {
    public GasterBlasterProItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "gaster_blaster"));
    }
}
