package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.summon.GroundBone;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GroundBoneModel extends DefaultedEntityGeoModel<GroundBone> {
    public GroundBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "bone"));
    }
}
