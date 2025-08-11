package com.sakpeipei.mod.undertale.client.model.entity;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.GasterBlasterPro;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GasterBlasterProModel extends DefaultedEntityGeoModel<GasterBlasterPro> {
    public GasterBlasterProModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"gaster_blaster"));
    }
}
