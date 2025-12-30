package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.summon.GasterBlasterFixed;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GasterBlasterFixedModel extends DefaultedEntityGeoModel<GasterBlasterFixed> {
    public GasterBlasterFixedModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"gaster_blaster"));
    }
}
