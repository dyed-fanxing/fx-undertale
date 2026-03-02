package com.sakpeipei.undertale.common;

import com.sakpeipei.undertale.Undertale;
import net.minecraft.resources.ResourceLocation;

public interface ResourceLocations {
    ResourceLocation WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    ResourceLocation BEAM_FLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID,"textures/misc/beam_flow.png");
}
