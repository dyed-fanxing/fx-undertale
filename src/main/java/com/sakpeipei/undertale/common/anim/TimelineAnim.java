package com.sakpeipei.undertale.common.anim;

import com.ibm.icu.impl.Pair;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个单动画组成
 * @param anims 触发动画的 Tick点Map<Tick点，动画事件>
 * @param actions 触发动作/行为或进行判定的 Tick点Map<Tick点，动作/行为/判定>
 * @param length 动画时长
 * @param cd 冷却时间
 */
public record TimelineAnim(int length, int cd, Map<Integer, Byte> anims, Map<Integer, ToIntFunction<LivingEntity>> actions) {

    // 单动画，单动作，单判定
    public TimelineAnim(byte id, int length, int cd,int hitTick, ToIntFunction<LivingEntity> action) {
        this(length,cd,Map.of(0,id),Map.of(hitTick,action));
    }
    // 单动画，多动作，单判定
    public TimelineAnim(byte id, int length, int cd, Map<Integer,ToIntFunction<LivingEntity>> actions) {
        this(length,cd,Map.of(0,id),actions);
    }

    /**
     * 多回合，单动画，单动作，单判定
     * 动画总长度 = 最后一次判定的位置 + 最后执行行动后的长度
     * 最后执行行动后的长度 = 间隔 - 动作的判定Tick = interval - hitTick
     * @param round 回合数
     * @param interval 间隔，不是指两个判定点之间的Tick，而是指每个判定点到下一次判定开始前的那个Tick，而不是到判定点的Tick，即单个判定长度：hiTick + interval = length
     *                 例如：4+16=20，24+16=40，就是假设每个判定都是一次单独的动画，那么动画开始Tick点是0，判定点是4，长度是20，那interval就是16，依次类推
     */
    public static TimelineAnim create(int round,int interval,int cd, byte id, int hitTick, ToIntFunction<LivingEntity> action) {
        Map<Integer, ToIntFunction<LivingEntity>> acts = new HashMap<>(round);
        int offsetHitTick = 0;
        for (int i = 0; i < round; i++) {
            offsetHitTick += hitTick;
            acts.put(offsetHitTick, action);
            offsetHitTick+=interval;
        }
        return new TimelineAnim( offsetHitTick,cd,Map.of(0,id), Map.copyOf(acts) );
    }
    /**
     * 多回合，单动画，多动作，单判定
     * 动画总长度 = 最后一次判定的位置 + 最后执行行动后的长度
     * 最后执行行动后的长度 = 间隔 - 回合内的第一个行动的判定Tick = interval - actions的第一个的key
     * @param round 回合数
     * @param interval 间隔
     */
    public static TimelineAnim create(int round,int interval,int cd, byte id, List<Pair<Integer,ToIntFunction<LivingEntity>>> actions) {
        Map<Integer, ToIntFunction<LivingEntity>> acts = new HashMap<>(round * actions.size());
        int offsetHitTick = 0;
        for (int i = 0; i < round; i++) {
            int hitTick = 0;
            for (Pair<Integer, ToIntFunction<LivingEntity>> action : actions) {
                hitTick = offsetHitTick + action.first;
                acts.put(hitTick, action.second);
            }
            offsetHitTick = hitTick + interval;
        }
        return new TimelineAnim(offsetHitTick + actions.getFirst().first,cd,Map.of(0,id), Map.copyOf(acts) );
    }

    // 多动画，多动作，多判定
    public static TimelineAnim create(int length,int cd,  Map<Integer, Byte> anims, Map<int[], ToIntFunction<LivingEntity>> actions) {
        Map<Integer, ToIntFunction<LivingEntity>> acts = new HashMap<>();
        for (Map.Entry<int[], ToIntFunction<LivingEntity>> entry : actions.entrySet()) {
            for (int hitTick : entry.getKey()) {
                acts.put(hitTick, entry.getValue());
            }
        }
        return new TimelineAnim(length,cd,anims, Map.copyOf(acts));
    }
}