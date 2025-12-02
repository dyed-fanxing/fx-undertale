package com.sakpeipei.mod.undertale.entity.common;

import net.minecraft.sounds.SoundEvent;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个动画类型组成
 */
public class RoundAnim<T> implements AnimType<T> {
    private final AnimType<T> animType;
    private int rounds;


    public RoundAnim(int rounds, AnimType<T> animType) {
        this.rounds = rounds;
        this.animType = animType;
    }

    @Override
    public byte getId() {
        return animType.getId();
    }

    @Override
    public T getAction() {
        return animType.getAction();
    }

    @Override
    public SoundEvent getSoundEvent() {
        return animType.getSoundEvent();
    }

    @Override
    public int getDuration() {
        return animType.getDuration();
    }

    @Override
    public boolean isTriggerAnim() {
        return animType.isTriggerAnim();
    }

    @Override
    public int getCd() {
        return animType.getCd();
    }

    @Override
    public boolean shouldHitAt(int currentTick) {
        return animType.shouldHitAt(currentTick);
    }

    @Override
    public boolean shouldPlaySoundAt(int currentTick) {
        return animType.shouldPlaySoundAt(currentTick);
    }


    @Override
    public boolean isCompeted() {
        if(animType.isCompeted()){
            return --rounds == 0;
        }
        return false;
    }
    public int getRounds() {
        return rounds;
    }
    public void setRounds(int rounds) {
        this.rounds = rounds;
    }
}