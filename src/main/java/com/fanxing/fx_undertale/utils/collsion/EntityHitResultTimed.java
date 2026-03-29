package com.fanxing.fx_undertale.utils.collsion;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class EntityHitResultTimed extends EntityHitResult {
    double time;
    public EntityHitResultTimed(Entity entity, Vec3 location, double time) {
        super(entity,location);
        this.time = time;
    }

    public double getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "EntityHitResultTimed{" +
                "time=" + time +
                ", location=" + location +
                '}';
    }
}
