package com.sakpeipei.mod.undertale.mechanism;

import net.minecraft.world.entity.Entity;

/**
 * @author yujinbao
 * @since 2025/9/24 10:23
 */
public class GreenAttack implements ColorAttack{
    @Override
    public boolean canHitEntity(Entity target) {
        return ColorAttack.super.canHitEntity(target);
    }

    @Override
    public int getColor() {
        return 0;
    }
}
