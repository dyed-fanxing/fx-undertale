package com.sakpeipei.mod.undertale.mechanism;

import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.util.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yujinbao
 * @since 2025/9/24 09:53
 */
public interface ColorAttack {
    Map<Integer, ColorAttack> REGISTRY = new HashMap<>();
    // 注册方法
    static ColorAttack register(ColorAttack attack) {
        REGISTRY.put(attack.getColor().getColor(), attack);
        return attack;
    }
    // 静态初始化块注册所有实例
    ColorAttack WHITE = register(new ColorAttack(){});
    ColorAttack AQUA = register(new AquaAttack());
    ColorAttack ORANGE = register(new OrangeAttack());
    ColorAttack GREEN = register(new GreenAttack());
    ColorAttack RED = register(new ColorAttack() {
        @Override
        public Color getColor() {
            return Color.RED;
        }
    });

    default boolean canHitEntity(Entity target){
        return true;
    }
    default Color getColor(){
        return Color.WHITE; // 白色
    }

    /**
     * 根据颜色值获取对应的ColorAttack实例
     */
    static ColorAttack getInstance(int color) {
        return REGISTRY.getOrDefault(color, WHITE);
    }

}
