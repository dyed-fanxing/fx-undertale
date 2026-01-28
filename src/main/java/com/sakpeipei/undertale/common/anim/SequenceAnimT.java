package com.sakpeipei.undertale.common.anim;

import com.sakpeipei.undertale.common.function.FloatSupplier;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sakpeipei
 * @since 2025/11/21 15:27
 * 序列动画，由多个动画组成
 */
public class SequenceAnimT<T>{

    private int length;
    private final int cd;
    private final List<AnimStepT<T>> steps;
    private final FloatSupplier speedModifier;

    public SequenceAnimT(byte id, int animTick, int hitTick, int length,FloatSupplier speedModifier, int cd, T action) {
        this.length = length;
        this.cd = cd;
        this.steps = List.of(new AnimStepT<>(id,animTick,hitTick,speedModifier,action));
        this.speedModifier = speedModifier;
    }
    public SequenceAnimT(byte id, int animTick, int[] hitTick, int length,FloatSupplier speedModifier, int cd, T action) {
        this.length = length;
        this.cd = cd;
        this.steps = List.of(new AnimStepT<>(id,animTick,hitTick,speedModifier,action));
        this.speedModifier = speedModifier;
    }
    /**
     * @param length 序列的length
     * @param cd 冷却时间
     * @param steps 步骤列表
     */
    public SequenceAnimT(int length, int cd, List<AnimStepT<T>> steps, FloatSupplier speedModifier) {
        this.length = length;
        this.cd = cd;
        this.steps = steps;
        this.speedModifier = speedModifier;
    }

    /**
     * 通过指定的回合数，构造重复的序列
     * @param round 回合数 - 重复次数
     * @param interval 序列之间的间隔，即单次序列的length
     * @param cd 冷却时间
     * @param steps 要重复的步骤模板（每个步骤的hitTick是相对于该次重复的起始时间）
     */
    public SequenceAnimT(int round, int interval, int cd, List<AnimStepT<T>> steps, FloatSupplier speedModifier) {
        this.speedModifier = speedModifier;
        this.length = interval * round;
        this.cd = cd;
        this.steps = new ArrayList<>(steps.size() * round);
        this.steps.addAll(steps);
        for (int i = 1; i < round; i++) {
            for (AnimStepT<T> step : steps) {
                this.steps.add(new AnimStepT<>(step.id,step.animTick,step.hitTicks,speedModifier,step.action,i * interval));
            }
        }
    }

    /**
     * 回合，单AnimStep，单判定时机
     */
    public SequenceAnimT(int round, int interval, int cd, byte id, int animTick, int hitTick, T action, FloatSupplier speedModifier) {
        this.speedModifier = speedModifier;
        this.length = interval * round;
        this.cd = cd;
        this.steps = new ArrayList<>();
        for (int i = 0; i < round; i++) {
            this.steps.add(new AnimStepT<>(id,i * interval + animTick,i * interval + hitTick,speedModifier,action));
        }
    }

    /**
     * 回合，单AnimStep，多判定时机
     */
    public SequenceAnimT(int round, int interval, int cd, byte id, int animTick, int[] hitTick, T action, FloatSupplier speedModifier) {
        this.speedModifier = speedModifier;
        this.length = interval * round;
        this.cd = cd;
        this.steps = new ArrayList<>();
        for (int i = 0; i < round; i++) {
            this.steps.add(new AnimStepT<>(id,animTick,hitTick,speedModifier,action,i * interval));
        }
    }
    public void addLength(int step,int increment) {
        this.length += increment;
        for (int i = step; i < steps.size(); i++) {
            steps.get(i).applyOffset(increment);
        }
    }

    public List<AnimStepT<T>> getSteps() {return steps;}
    public int getLength() {
        return (int) (speedModifier.get()*length);
    }
    public int getCd() {
        return cd;
    }

}