package com.sakpeipei.mod.undertale.entity.common;

import net.minecraft.sounds.SoundEvent;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个动画类型组成
 */
public class SequenceAnim<T> implements AnimType<T> {
    private AnimType<T>[] steps;
    private int step = 0;

    public SequenceAnim(AnimType<T>[] steps) {
        this.steps = steps;
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
        return step == steps.length - 1;
    }

    public void resetSequence() {
        step = 0;
    }

    public AnimType<T> getCurrentAnim() {
        return steps[step];
    }

    public int getIndex() {
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
        int cd = steps[step].getCd();
        step = step + 1;
        return cd;
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