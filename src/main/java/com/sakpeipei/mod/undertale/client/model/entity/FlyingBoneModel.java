package com.sakpeipei.mod.undertale.client.model.entity;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import com.sakpeipei.mod.undertale.entity.projectile.FlyingBone;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class FlyingBoneModel extends DefaultedEntityGeoModel<FlyingBone> {
    public FlyingBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "bone"), true);
    }
}
