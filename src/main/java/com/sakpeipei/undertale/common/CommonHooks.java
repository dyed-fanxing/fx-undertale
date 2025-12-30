package com.sakpeipei.undertale.common;

import com.sakpeipei.undertale.event.ProjectileDodgeEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.NeoForge;

/**
 * @author Sakqiongzi
 * @since 2025-10-13 23:11
 */
public class CommonHooks {
    public static boolean onProjectileDodge(Entity target, Projectile projectile) {
        return NeoForge.EVENT_BUS.post(new ProjectileDodgeEvent(target, projectile)).isCanceled();
    }
}
