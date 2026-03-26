package com.fanxing.fx_undertale.client.model.entity;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.RotationBone;
import net.minecraft.resources.ResourceLocation;

public class RotationBoneModel extends GrowableBoneModel<RotationBone> {
    public RotationBoneModel() {
        super(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "bone"));
    }
}
