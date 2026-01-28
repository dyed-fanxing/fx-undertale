package com.sakpeipei.undertale.common.anim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * @author Sakpeipei
 * @since 2025/11/21 15:27
 * 序列动画，由多个单动画组成
 * @param  cd 冷却时间
 * @param steps 动画步骤列表
 */
public record SequenceAnim<T>(int cd, List<SingleAnim<T>> steps) {

    public SequenceAnim(byte id, int hitTick, int length, int cd, T action) {
        this(cd, List.of(new SingleAnim<>(id, hitTick, length, 0, action)));
    }

    public SequenceAnim(byte id, int[] hitTicks, int length, int cd, T action) {
        this(cd, List.of(new SingleAnim<>(id, hitTicks, length, 0, action)));
    }

    /**
     * 通过指定的回合数，构造重复的序列
     *
     * @param round 回合数 - 重复次数
     * @param cd    冷却时间
     * @param steps 要重复的步骤模板（每个步骤的hitTick是相对于该次重复的起始时间）
     */
    public SequenceAnim(int round, int cd, List<SingleAnim<T>> steps) {
        this(cd, new ArrayList<>(steps.size() * round));
        for (int i = 0; i < round; i++) {
            this.steps.addAll(steps);
        }
    }

    /**
     * 回合，单SingleAnim
     */
    public SequenceAnim(int round, int cd, SingleAnim<T> step) {
        this(cd, new ArrayList<>(round));
        for (int i = 0; i < round; i++) {
            this.steps.add(step);
        }
    }

    /**
     * 创建只播放第一个动画步骤的回合序列
     */
    public static <T> SequenceAnim<T> onceAnim(int round, int cd, List<SingleAnim<T>> steps){
        List<SingleAnim<T>> all = new ArrayList<>(steps.size() * round);
        all.addAll(steps);
        List<SingleAnim<T>> noAnims = new ArrayList<>(steps.size());
        for (int i = 1; i < round; i++) {
            for (SingleAnim<T> step : steps) {
                noAnims.add(new SingleAnim<>((byte) -1,step.hitTicks(), step.length(), step.cd(), step.action()));
            }
            all.addAll(noAnims);
        }
        return new SequenceAnim<>(cd,all);
    }
    /**
     * 创建只播放第一个动画步骤的回合序列
     */
    public static <T> SequenceAnim<T> onceAnim(int round, int cd, SingleAnim<T> step){
        List<SingleAnim<T>> steps = new ArrayList<>(round);
        SingleAnim<T> t = new SingleAnim<>((byte) -1, step.hitTicks(), step.length(), step.cd(), step.action());
        steps.add(step);
        for (int i = 1; i < round; i++) {
            steps.add(t);
        }
        return new SequenceAnim<>(cd,steps);
    }
}