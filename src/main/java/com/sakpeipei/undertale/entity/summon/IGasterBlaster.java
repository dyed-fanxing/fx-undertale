package com.sakpeipei.undertale.entity.summon;

import net.minecraft.world.level.Level;

public interface IGasterBlaster{

    Level level();

    float getSize();

    /**
     * 是否正在开火
     */
    boolean isFire();
}

