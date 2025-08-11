package com.sakpeipei.mod.undertale.utils;


import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RotUtils {
    public static float yRot(double z,double x) {
        return (float) Math.toDegrees(Math.atan2(z, x)) - 90;
    }
    public static float xRot(double y) {
        return (float) -Math.toDegrees(Math.asin(y));
    }

    public static Vec3 dirRot(Vec3 dir, float xRot, float yRot, float zRot) {
           return dir.zRot((zRot - 90) * Mth.DEG_TO_RAD)
                .yRot(-yRot * Mth.DEG_TO_RAD)
                .xRot(-xRot * Mth.DEG_TO_RAD);
    }
    public static float dirZRot(float rot) {
      return ( rot - 90) * Mth.DEG_TO_RAD;
    }
    public static float dirYRot(float rot) {
        return -rot * Mth.DEG_TO_RAD;
    }
    public static float dirXRot(float rot) {
        return -rot * Mth.DEG_TO_RAD;
    }

}
