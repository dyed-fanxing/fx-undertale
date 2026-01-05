package com.sakpeipei.undertale.common;

import org.joml.Vector3f;

/**
 * @author yujinbao
 * @since 2026/1/5 16:40
 */
public class Vec3 extends net.minecraft.world.phys.Vec3 {

    // 局部重力
    protected net.minecraft.world.phys.Vec3 g = new net.minecraft.world.phys.Vec3(0, -1, 0);



    public Vec3(Vector3f p_253821_) {
        super(p_253821_);
    }

    public Vec3(double p_82484_, double p_82485_, double p_82486_) {
        super(p_82484_, p_82485_, p_82486_);
    }


    @Override
    public net.minecraft.world.phys.Vec3 normalize() {
        return super.normalize();
    }

    @Override
    public net.minecraft.world.phys.Vec3 vectorTo(net.minecraft.world.phys.Vec3 p_82506_) {
        return super.vectorTo(p_82506_);
    }

    @Override
    public net.minecraft.world.phys.Vec3 add(net.minecraft.world.phys.Vec3 p_82550_) {

        return super.add(p_82550_);
    }
}
