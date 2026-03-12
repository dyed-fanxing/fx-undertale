package com.fanxing.fx_undertale.common;

import com.fanxing.fx_undertale.event.ProjectileDodgeEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.NeoForge;

/**
 * @author FanXing
 * @since 2025-10-13 23:11
 */
public class CommonHooks {
    public static boolean onProjectileDodge(Entity target, Projectile projectile) {
        return NeoForge.EVENT_BUS.post(new ProjectileDodgeEvent(target, projectile)).isCanceled();
    }
}
