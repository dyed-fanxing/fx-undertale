package com.sakpeipei.mod.undertale.entity.common;

import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个动画类型组成
 */
public class SequenceAnim<T> implements AnimType<T> {
    private List<AnimType<T>> steps;
    private int step = 0;

    public SequenceAnim(List<AnimType<T>> steps) {
        this.steps = steps;
    }

    public SequenceAnim(AnimType<T> ...steps) {
        this.steps = List.of(steps);
    }
    // IAnimType 接口实现 - 代理到当前步骤
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
    public boolean isCompeted() {
        step = step + 1;
        if(step == steps.size()) {
            step = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldHitAt(int currentTick) {
        return steps.get(step).shouldHitAt(currentTick);
    }

    @Override
    public boolean shouldPlaySoundAt(int currentTick) {
        return steps.get(step).shouldPlaySoundAt(currentTick);
    }

}