package com.sakpeipei.mod.undertale.client.model.entity;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.projectile.GroundBoneProjectile;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GroundBoneProjectileModel extends DefaultedEntityGeoModel<GroundBoneProjectile> {
    public GroundBoneProjectileModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "bone"));
    }
}
