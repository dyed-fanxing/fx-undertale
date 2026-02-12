package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GasterBlasterModel extends DefaultedEntityGeoModel<GasterBlaster> {
    public GasterBlasterModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,"gaster_blaster"));
    }
}
