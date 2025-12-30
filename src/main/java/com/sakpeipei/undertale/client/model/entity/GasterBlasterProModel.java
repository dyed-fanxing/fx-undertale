package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.summon.GasterBlasterPro;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GasterBlasterProModel extends DefaultedEntityGeoModel<GasterBlasterPro> {
    public GasterBlasterProModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"gaster_blaster"));
    }
}
