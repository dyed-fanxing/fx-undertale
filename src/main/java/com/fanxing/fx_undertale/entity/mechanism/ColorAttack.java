package com.fanxing.fx_undertale.entity.mechanism;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sakpeipei
 * @since 2025/9/24 09:53
 */
public class ColorAttack {

    private static final Map<Integer, ColorAttack> REGISTRY = new HashMap<>();

    private final int color;
    // 静态初始化块注册所有实例
    public static ColorAttack WHITE = new ColorAttack(Color.WHITE.getRGB());
    public static ColorAttack RED = new ColorAttack(Color.RED.getRGB());
    public static ColorAttack AQUA = new ColorAttack(Color.CYAN.getRGB()){
        @Override
        public boolean canHitEntity(Entity target) {
            if(target instanceof ServerPlayer player){
                Vec3 knownMovement = player.getKnownMovement();
                return knownMovement.horizontalDistanceSqr() > 1.0E-4 || Mth.square(knownMovement.y) > 0.02;
            }
            double dx = target.getX() - target.xo;
            double dy = target.getY() - target.yo;
            double dz = target.getZ() - target.zo;
            return dx * dx + dy * dy + dz * dz > 2.5000003E-7;
        }
    };
    public static ColorAttack GREEN = new ColorAttack(Color.GREEN.getRGB()){
        @Override
        public boolean canHitEntity(Entity target) {
            return super.canHitEntity(target);
        }
    };
    public static ColorAttack ORANGE = new ColorAttack(0xFFFCA600){
        @Override
        public boolean canHitEntity(Entity target) {
            if(target instanceof ServerPlayer player){
                Vec3 knownMovement = player.getKnownMovement();
                return knownMovement.lengthSqr() == 0;
            }
            double dx = target.getX() - target.xo;
            double dy = target.getY() - target.yo;
            double dz = target.getZ() - target.zo;
            return dx * dx + dy * dy + dz * dz <= 2.5000003E-7;
        }
    };
    public ColorAttack(int color){
        this.color = color;
        REGISTRY.put(color, this); // 自动注册
    }

    public boolean canHitEntity(Entity target){
        return true;
    }

    public int getColor() {
        return color;
    }

    public static ColorAttack of(int color) {
        return REGISTRY.getOrDefault(color, WHITE);
    }
}
