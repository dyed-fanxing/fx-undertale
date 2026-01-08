package com.sakpeipei.undertale.client.model.entity;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class SansModel extends DefaultedEntityGeoModel<Sans> {
    public SansModel() {
        super(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "sans"), true);
    }
}
