package com.fanxing.fx_undertale.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoAnimatable;

import java.util.concurrent.atomic.AtomicLong;

public final class GeoDebugHelper {
    public static final Logger DEBUG_LOGGER = LoggerFactory.getLogger("GeoDebug");
    public static final AtomicLong DEBUG_COUNTER = new AtomicLong(0);
    public static GeoAnimatable animatable;
    private GeoDebugHelper() {}
}