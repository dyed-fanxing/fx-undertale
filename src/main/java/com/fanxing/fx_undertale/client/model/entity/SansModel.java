package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SansModel extends DefaultedEntityGeoModel<Sans> {
    private static final Logger log = LoggerFactory.getLogger(SansModel.class);

    public SansModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "sans"), true);
    }


}
