package com.fanxing.fx_undertale.entity.summon;

import com.fanxing.fx_undertale.entity.Mountable;
import net.minecraft.world.level.Level;

public interface IGasterBlaster extends Mountable {

    Level level();

    float getSize();

    /**
     * 是否正在开火
     */
    boolean isFire();

    @Override
    default boolean shouldDismountOnDoubleKey() {
        return true;
    }
}

