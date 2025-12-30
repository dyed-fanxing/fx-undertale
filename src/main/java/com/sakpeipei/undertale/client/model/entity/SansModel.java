package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SansModel extends DefaultedEntityGeoModel<Sans> {
    public SansModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "sans"), true);
    }
}
