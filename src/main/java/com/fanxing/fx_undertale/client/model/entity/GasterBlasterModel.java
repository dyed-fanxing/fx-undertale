package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.GasterBlaster;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GasterBlasterModel extends DefaultedEntityGeoModel<GasterBlaster> {
    public GasterBlasterModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,"gaster_blaster"));
    }
}
