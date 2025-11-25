package com.sakpeipei.mod.undertale.entity.common;

import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个动画类型组成
 */
public class RoundSequenceAnim<T> implements AnimType<T> {
    private int round;
    private List<AnimType<T>> steps;
    private int step = 0;



    public RoundSequenceAnim(int round, List<AnimType<T>> steps) {
        this.round = round;
        this.steps = steps;
    }

    public RoundSequenceAnim(List<AnimType<T>> steps) {
        this(0, steps);
    }


    @Override
    public byte getId() {
        return steps.get(step).getId();
    }

    @Override
    public T getAction() {
        return steps.get(step).getAction();
    }

    @Override
    public SoundEvent getSoundEvent() {
        return steps.get(step).getSoundEvent();
    }

    @Override
    public int getDuration() {
        return steps.get(step).getDuration();
    }

    @Override
    public boolean isTriggerAnim() {
        return steps.get(step).isTriggerAnim();
    }

    @Override
    public int getCd() {
        return steps.get(step).getCd();
    }

    @Override
    public boolean shouldHitAt(int currentTick) {
        return steps.get(step).shouldHitAt(currentTick);
    }

    @Override
    public boolean shouldPlaySoundAt(int currentTick) {
        return steps.get(step).shouldPlaySoundAt(currentTick);
    }



    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public List<AnimType<T>> getSteps() {
        return steps;
    }

    public void setSteps(List<AnimType<T>> steps) {
        this.steps = steps;
    }

}