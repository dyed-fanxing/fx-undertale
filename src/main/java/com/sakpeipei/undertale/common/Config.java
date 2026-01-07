package com.sakpeipei.undertale.common;

/**
 * @author Sakqiongzi
 * @since 2026-01-08 01:55
 */
public class Config {
    // 圆柱和球体的分段数
    public static final byte MIN_SEGMENTS = 32;
    public static final byte MAX_SEGMENTS = 127;

    public static byte segments(float size) {
        return (byte) Math.max(MIN_SEGMENTS + (Math.round(size) - 1) * 2, MAX_SEGMENTS);
    }
}
