package com.fanxing.fx_undertale.utils;

import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.FlyingAnimal;

public class EntitySelector {
    /**
     * 目标是否正在飞行
     */
    public static boolean isFlying(LivingEntity t) {
        return !t.onGround() && (t.isNoGravity() || t instanceof FlyingAnimal || t instanceof FlyingMob);
    }
}
