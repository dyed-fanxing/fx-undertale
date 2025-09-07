package com.sakpeipei.mod.undertale.client.model.entity;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.boss.Sans;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SansModel extends DefaultedEntityGeoModel<Sans> {
    public SansModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "sans"), true);
    }
}
