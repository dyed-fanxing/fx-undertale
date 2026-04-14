package com.fanxing.fx_undertale.common.phys;

import net.minecraft.world.phys.Vec3;

/**
 * @author Sakpeipei
 * @since 2025/11/28 15:45
 * 局部方向
 */
public enum LocalDirection {
    UP(0,1,0),
    DOWN(0,-1,0),
    LEFT(-1,0,0),
    RIGHT(1,0,0),
    FRONT(0,0,1),
    BACK(0,0,-1);
    private final Vec3 vec3;
    LocalDirection(double x,double y,double z) {
        this.vec3 = new Vec3(x,y,z);
    }

    public Vec3 getVec3() {
        return vec3;
    }
}
