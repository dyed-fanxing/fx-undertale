package com.sakpeipei.mod.undertale.entity.common;

import net.minecraft.sounds.SoundEvent;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个动画类型组成
 */
public class SequenceAnim<T> implements IAnimType<T> {
    private int round;
    private AnimType<T>[] steps;
    private int step = 0;

    public SequenceAnim(int round, AnimType<T>[] steps) {
        this.round = round;
        this.steps = steps;
    }

    public SequenceAnim(AnimType<T>[] steps) {
        this(0, steps);
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public AnimType<T>[] getSteps() {
        return steps;
    }

    public void setSteps(AnimType<T>[] steps) {
        this.steps = steps;
    }

    // 序列管理方法
    public void nextStep() {
        step = (step + 1) % steps.length;
    }

    public boolean isSequenceFinished() {
        return step == steps.length - 1 && round <= 0;
    }

    public void resetSequence() {
        step = 0;
    }

    public AnimType<T> getCurrentStep() {
        return steps[step];
    }

    public int getCurrentStepIndex() {
        return step;
    }

    // IAnimType 接口实现 - 代理到当前步骤
    @Override
    public byte getId() {
        return steps[step].getId();
    }

    @Override
    public T getAction() {
        return steps[step].getAction();
    }

    @Override
    public SoundEvent getSoundEvent() {
        return steps[step].getSoundEvent();
    }

    @Override
    public int getDuration() {
        return steps[step].getDuration();
    }

    @Override
    public boolean isTriggerAnim() {
        return steps[step].isTriggerAnim();
    }

    @Override
    public int getCd() {
        return steps[step].getCd();
    }

    @Override
    public boolean shouldHitAt(int currentTick) {
        return steps[step].shouldHitAt(currentTick);
    }

    @Override
    public boolean shouldPlaySoundAt(int currentTick) {
        return steps[step].shouldPlaySoundAt(currentTick);
    }
}