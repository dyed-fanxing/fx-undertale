package com.sakpeipei.undertale.common.mechanism;

import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.util.Color;


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
    public Color getColor() {
        return new Color(0xFF00C000);
    }
}
