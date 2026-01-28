package com.sakpeipei.undertale.common.mechanism;

import net.minecraft.world.entity.Entity;

import java.awt.*;


/**
 * @author Sakpeipei
 * @since 2025/9/24 10:23
 */
public class GreenAttack implements ColorAttack{
    @Override
    public boolean canHitEntity(Entity target) {
        return ColorAttack.super.canHitEntity(target);
    }

    @Override
    public Color getColor() {
        return Color.GREEN;
    }
}
