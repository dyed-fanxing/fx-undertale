package com.sakpeipei.mod.undertale.entity.projectile;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * @author Sakqiongzi
 * @since 2025-09-21 21:57
 */
public class AbstractRotateProjectile extends Projectile {
    protected AbstractRotateProjectile(EntityType<? extends AbstractRotateProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }




}
