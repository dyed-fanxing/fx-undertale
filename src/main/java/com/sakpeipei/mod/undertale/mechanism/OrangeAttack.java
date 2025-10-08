package com.sakpeipei.mod.undertale.mechanism;

import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.util.Color;

/**
 * 橙色攻击
 * @author yujinbao
 * @since 2025/9/24 10:19
 */
public class OrangeAttack implements ColorAttack{

    @Override
    public boolean canHitEntity(Entity target) {
        double d0 = target.getX() - target.xo;
        double d1 = target.getZ() - target.zo;
        return d0 * d0 + d1 * d1 <= (double)2.5000003E-7F;
    }

    @Override
    public Color getColor() {
        return new Color(0xFFFCA600);
    }
}
