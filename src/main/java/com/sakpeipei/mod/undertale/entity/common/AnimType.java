package com.sakpeipei.mod.undertale.entity.common;

import net.minecraft.sounds.SoundEvent;

/**
 * @author Sakqiongzi
 * @since 2025-11-23 21:08
 * 服务端动画类型接口
 */
public interface AnimType<T> {
    byte getId();
    T getAction();
    SoundEvent getSoundEvent();
    int getDuration();
    boolean isTriggerAnim();
    int getCd();

    void addDuration(int increment);

    // 在该tick是否判定
    boolean shouldHitAt(int currentTick);
    // 在该tick是否播放音效
    boolean shouldPlaySoundAt(int currentTick);


}
