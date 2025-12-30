package com.sakpeipei.undertale.entity.common.anim;

import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个动画类型组成
 */
public class SequenceAnim<T> implements AnimType<T> {

    private int duration;
    private final int cd;
    private final List<Step<T>> steps;
    private int step = 0;

    /**
     * @param duration 序列的duration
     * @param cd 冷却时间
     * @param steps 步骤列表
     */
    public SequenceAnim(int duration, int cd,List<Step<T>> steps) {
        this.duration = duration;
        this.cd = cd;
        this.steps = steps;
    }
    public SequenceAnim(int duration, int cd,Step<T> ...steps) {
        this.duration = duration;
        this.cd = cd;
        this.steps = List.of(steps);
    }

    /**
     * 通过指定的回合数，构造重复的序列
     * @param round 回合数 - 重复次数
     * @param interval 序列之间的间隔，即单次序列的duration
     * @param cd 冷却时间
     * @param steps 要重复的步骤模板（每个步骤的hitTick是相对于该次重复的起始时间）
     */
    public SequenceAnim(int round,int interval, int cd, Step<T> ...steps) {
        this.cd = cd;
        this.duration = interval * round;
        this.steps = new ArrayList<>(List.of(steps));
        for (int i = 1; i < round; i++) {
            for (Step<T> step : steps) {
                this.steps.add(new Step<>(step.getId(), step.getHitTick() + i * interval, step.getAction()));
            }
        }
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
    public int getDuration() {
        return duration;
    }

    @Override
    public int getCd() {
        return cd;
    }

    @Override
    public SoundEvent getSoundEvent() {
        return null;
    }



    @Override
    public boolean shouldHitAt(int animTick) {
        if(animTick == steps.get(step).hitTick){
            step = (step + 1) % steps.size();
            return true;
        }
        return false;
    }

    @Override
    public void addDuration(int increment) {
        this.duration += increment;
        for (int i = step; i < steps.size(); i++) {
            steps.get(i).addHitTick(increment);
        }
    }

    @Override
    public boolean shouldPlaySoundAt(int animTick) {
        return animTick == steps.get(step).hitTick - 4;
    }
}