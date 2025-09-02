package com.sakpeipei.mod.undertale.client.model.entity;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GasterBlasterFixedModel extends DefaultedEntityGeoModel<GasterBlasterFixed> {
    public GasterBlasterFixedModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"gaster_blaster"));
    }
}
