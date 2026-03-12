package com.fanxing.fx_undertale.entity.ai.anim;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * @author Sakpeipei
 * @since 2025/11/21 15:27
 * 序列动画，由多个单动画组成
 * @param  cooldown 冷却时间
 * @param steps 动画步骤列表
 */
public record SequenceAnim(int cooldown, List<AnimStep> steps) {

    public SequenceAnim(int hitTick, ToIntFunction<LivingEntity> action, int duration, int cooldown) {
        this(cooldown, List.of(new AnimStep(null, hitTick,action,duration)));
    }
    public SequenceAnim(Byte id, int hitTick, ToIntFunction<LivingEntity> action, int duration, int cooldown) {
        this(cooldown, List.of(new AnimStep(id, hitTick,action,duration)));
    }
    public SequenceAnim(int[] hitTicks, int duration, int cooldown, ToIntFunction<LivingEntity> action) {
        this(cooldown, List.of(new AnimStep(null, hitTicks,action,duration)));
    }
    public SequenceAnim(Byte id, int[] hitTicks, int duration, int cooldown, ToIntFunction<LivingEntity> action) {
        this(cooldown, List.of(new AnimStep(id, hitTicks,action,duration)));
    }

    /**
     * 通过指定的回合数，构造重复的序列
     *
     * @param round 回合数 - 重复次数
     * @param cooldown    冷却时间
     * @param steps 要重复的步骤模板（每个步骤的hitTick是相对于该次重复的起始时间）
     */
    public SequenceAnim(int round, int cooldown, List<AnimStep> steps) {
        this(cooldown, new ArrayList<>(steps.size() * round));
        for (int i = 0; i < round; i++) {
            this.steps.addAll(steps);
        }
    }

    /**
     * 回合，单SingleAnim
     */
    public SequenceAnim(int round, int cooldown, AnimStep step) {
        this(cooldown, new ArrayList<>(round));
        for (int i = 0; i < round; i++) {
            this.steps.add(step);
        }
    }

    /**
     * 创建只播放第一个动画步骤的回合序列
     */
    public static SequenceAnim create(int round,int interval,int cooldown, byte id, int hitTick, ToIntFunction<LivingEntity> action){
        AnimStep animStep = new AnimStep(id, hitTick, action, interval + hitTick);
        AnimStep noAnimAction = new AnimStep( hitTick, action, interval + hitTick);
        List<AnimStep> steps = new ArrayList<>(round);
        steps.add(animStep);
        for (int i = 1; i < round; i++) {
            steps.add(noAnimAction);
        }
        return new SequenceAnim(cooldown,steps);
    }
    /**
     * 创建只播放第一个动画步骤的回合序列
     */
    public static SequenceAnim onceAnim(int round, int cooldown, List<AnimStep> steps){
        List<AnimStep> all = new ArrayList<>(steps.size() * round);
        all.addAll(steps);
        List<AnimStep> noAnims = new ArrayList<>(steps.size());
        for (int i = 1; i < round; i++) {
            for (AnimStep step : steps) {
                noAnims.add(new AnimStep(step.hitTicks(), step.action(), step.duration()));
            }
            all.addAll(noAnims);
        }
        return new SequenceAnim(cooldown,all);
    }
}