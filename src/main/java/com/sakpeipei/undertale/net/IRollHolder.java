package com.sakpeipei.undertale.net;

import org.spongepowered.asm.mixin.Unique;

public interface IRollHolder {
    @Unique
    void undertale$setRoll(float roll);
    @Unique
    float undertale$getRoll(float serverRoll);
}
