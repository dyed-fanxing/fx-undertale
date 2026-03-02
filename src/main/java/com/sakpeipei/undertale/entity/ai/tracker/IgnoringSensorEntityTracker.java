package com.sakpeipei.undertale.entity.ai.tracker;

import com.sakpeipei.undertale.entity.ai.sensing.SensorTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class IgnoringSensorEntityTracker implements PositionTracker {
    protected final Entity entity;
    protected final boolean trackEyeHeight;

    public IgnoringSensorEntityTracker(Entity entity, boolean trackEyeHeight) {
        this.entity = entity;
        this.trackEyeHeight = trackEyeHeight;
    }

    public @NotNull Vec3 currentPosition() {
        return this.trackEyeHeight ? this.entity.position().add(0.0F, this.entity.getEyeHeight(), 0.0F) : this.entity.position();
    }

    public @NotNull BlockPos currentBlockPosition() {
        return this.entity.blockPosition();
    }

    public boolean isVisibleBy(@NotNull LivingEntity entity) {
        if (this.entity instanceof LivingEntity target) {
            if(target.isAlive()) {
                return true;
            }
            return false;
        } else {
            return true;
        }
    }

    public Entity getEntity() {
        return this.entity;
    }

    public String toString() {
        return "IgnoringSensorEntityTracker for " + this.entity;
    }
}
