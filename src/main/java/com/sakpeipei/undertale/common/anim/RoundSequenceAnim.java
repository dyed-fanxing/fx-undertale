package com.sakpeipei.undertale.common.anim;

import java.util.List;

/**
 * @author Sakpeipei
 * @since 2025/11/21 15:27
 * 序列动画，由多个单动画组成
 * @param  round 回合数
 * @param  cd 冷却时间
 * @param steps 动画步骤列表
 */
public record RoundSequenceAnim<T>(int round, int cd, List<SingleAnim<T>> steps) {

    public RoundSequenceAnim(byte id, int hitTick, int length, int cd, T action) {
        this(1,cd, List.of(new SingleAnim<>(id, hitTick, length, 0, action)));
    }

    public RoundSequenceAnim(byte id, int[] hitTicks, int length, int cd, T action) {
        this(1,cd, List.of(new SingleAnim<>(id, hitTicks, length, 0, action)));
    }


    /**
     * 回合，单SingleAnim
     */
    public RoundSequenceAnim(int round, int cd, SingleAnim<T> step) {
        this(round,cd, List.of(step));
    }


}