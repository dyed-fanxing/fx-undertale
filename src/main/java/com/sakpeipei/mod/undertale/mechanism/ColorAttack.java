package com.sakpeipei.mod.undertale.mechanism;

import net.minecraft.world.entity.Entity;

/**
 * @author yujinbao
 * @since 2025/9/24 09:53
 */
public interface ColorAttack {
    ColorAttack WHITE = new ColorAttack(){};
    ColorAttack AQUA = new AquaAttack();
    ColorAttack ORANGE = new OrangeAttack();
    ColorAttack GREEN = new GreenAttack();
    ColorAttack RED = new ColorAttack() {
        @Override
        public int getColor() {
            return ColorAttack.super.getColor();
        }
    };

    default boolean canHitEntity(Entity target){
        return true;
    }
    default int getColor(){
        return 0;
    }
}
