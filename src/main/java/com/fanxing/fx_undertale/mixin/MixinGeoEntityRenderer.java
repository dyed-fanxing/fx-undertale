package com.fanxing.fx_undertale.mixin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(value = GeoEntityRenderer.class, remap = false)
public abstract class MixinGeoEntityRenderer {

    @Unique
    private static final Logger DEBUG_LOGGER = LoggerFactory.getLogger("GeoDebug");

    @Unique
    private static final AtomicLong DEBUG_COUNTER = new AtomicLong(0);



}