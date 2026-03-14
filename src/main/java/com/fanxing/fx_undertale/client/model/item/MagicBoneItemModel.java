package com.fanxing.fx_undertale.client.model.item;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.item.MagicBone;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class MagicBoneItemModel extends DefaultedItemGeoModel<MagicBone> {
    public MagicBoneItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "bone"));
    }
}