package com.sakpeipei.mod.undertale.client.model.entity;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.summon.GasterBlasterFixed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class GasterBlasterFixedModel extends DefaultedEntityGeoModel<GasterBlasterFixed> {
    public GasterBlasterFixedModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID,"gaster_blaster"));
    }
}
