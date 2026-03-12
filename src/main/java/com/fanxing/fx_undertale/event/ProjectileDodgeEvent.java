package com.fanxing.fx_undertale.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * @author FanXing
 * @since 2025-10-13 23:11
 * 弹射物闪避事件
 */
// 自定义事件类
public class ProjectileDodgeEvent extends Event implements ICancellableEvent {
    private final Entity target;
    private final Projectile projectile;

    public ProjectileDodgeEvent(Entity target, Projectile projectile) {
        this.target = target;
        this.projectile = projectile;
    }
    public Entity getTarget() {
        return target;
    }
    public Projectile getProjectile() {
        return projectile;
    }
}