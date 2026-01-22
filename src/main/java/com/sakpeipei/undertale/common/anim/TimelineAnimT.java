package com.sakpeipei.undertale.common.anim;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个单动画组成
 * @param anims 触发动画的 Tick点Map<Tick点，动画事件>
 * @param actions 触发动作/行为或进行判定的 Tick点Map<Tick点，动作/行为/判定>
 * @param cd 冷却时间
 * @param length 动画时长
 */
public record TimelineAnimT<T>(int length, int cd, Map<Integer, Byte> anims, Map<Integer, T> actions) {

    public TimelineAnimT(byte id, int hitTick, int length, int cd, T action) {
        this(cd,length,Map.of(0,id,length,(byte)-1),Map.of(hitTick,action));
    }
    public TimelineAnimT(int round, int cd, byte id, int hitTick, int interval, T action) {
        this(cd, round * interval, Map.of(0, id),IntStream.range(0, round).boxed().collect(Collectors.toMap(
                i -> hitTick + i * interval,
                i -> action
        )));
    }

    // 使用Entry列表
    public static <T> TimelineAnimT<T> create(int cd, int length, Map<Integer, Byte> anims, Map.Entry<int[], T>[] actionEntries) {
        anims.put(length,(byte) -1);
        Map<Integer, T> actions = new HashMap<>();
        for (Map.Entry<int[], T> entry : actionEntries) {
            for (int time : entry.getKey()) {
                actions.put(time, entry.getValue());
            }
        }
        return new TimelineAnimT<>(cd,length,anims, actions );
    }

    // 使用Map列表
    public static <T> TimelineAnimT<T> create(int cd, int length, Map<Integer, Byte> anims, Map<int[], T> actions) {
        Map<Integer, T> acts = new HashMap<>();
        actions.forEach((hitTicks,action) -> {
            for (int hitTick : hitTicks) {
                acts.put(hitTick, action);
            }
        });
        return new TimelineAnimT<>(cd,length,anims,acts);
    }
}