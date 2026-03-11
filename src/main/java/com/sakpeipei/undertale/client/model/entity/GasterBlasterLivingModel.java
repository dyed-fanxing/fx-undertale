package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.summon.GasterBlaster;
import com.sakpeipei.undertale.entity.summon.GasterBlasterLiving;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GasterBlasterLivingModel extends DefaultedEntityGeoModel<GasterBlasterLiving> {
    public GasterBlasterLivingModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,"gaster_blaster"));
    }
}
