package com.fanxing.fx_undertale.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.NotNull;

public interface IScalable<T extends Entity> extends ISelf<T> {
    float getScale();
    // ========== 尺寸相关 ==========
    default @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return getSelf().getType().getDimensions().scale(getScale());
    }
}
