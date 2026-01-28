package com.sakpeipei.undertale.common.anim;

import java.util.List;
import java.util.function.IntFunction;

/**
 * @author Sakpeipei
 * @since 2025/11/21 15:27
 * 序列动画，由多个单动画组成
 * @param  cd 冷却时间
 * @param rounder 回合计算器
 * @param steps 动画步骤列表
 */
public record RoundSequenceGAnim<T>(int cd, IntFunction<Integer> rounder, List<SingleAnim<T>> steps) {

    static final IntFunction<Integer> SINGLE_ROUND = diff -> 1;

    public RoundSequenceGAnim(byte id, int hitTick, int length, int cd, T action) {
        this(cd,SINGLE_ROUND, List.of(new SingleAnim<>(id, hitTick, length, 0, action)));
    }

    public RoundSequenceGAnim(byte id, int[] hitTicks, int length, int cd, T action) {
        this(cd,SINGLE_ROUND, List.of(new SingleAnim<>(id, hitTicks, length, 0, action)));
    }

    /**
     * 回合，单SingleAnim
     */
    public RoundSequenceGAnim(int cd, IntFunction<Integer> rounder, SingleAnim<T> step) {
        this(cd,rounder,List.of(step));
    }
}