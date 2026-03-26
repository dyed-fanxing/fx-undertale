package com.fanxing.fx_undertale.utils.collsion;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record TimedEntityHit(Entity entity, Vec3 hitPoint, double collisionTime) {
}
