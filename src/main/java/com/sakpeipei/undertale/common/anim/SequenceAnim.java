package com.sakpeipei.undertale.common.anim;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 * 序列动画，由多个动画组成
 */
public class SequenceAnim<T>{

    private final int cd;
    private final List<SingleAnim<T>> steps;

    public SequenceAnim(byte id, int animTick,int hitTick,  int cd,T action) {
        this.cd = cd;
        this.steps = List.of(new SingleAnim<T>(id,animTick,hitTick,action));
    }
    public SequenceAnim(byte id, int animTick,int[] hitTick, int cd,T action) {
        this.cd = cd;
        this.steps = List.of(new SingleAnim<T>(id,animTick,hitTick,action));
    }
    /**
     * @param cd 冷却时间
     * @param steps 步骤列表
     */
    public SequenceAnim( int cd,List<SingleAnim<T>> steps) {
        this.cd = cd;
        this.steps = steps;
    }

    /**
     * 通过指定的回合数，构造重复的序列
     * @param round 回合数 - 重复次数
     * @param interval 序列之间的间隔，即单次序列的length
     * @param cd 冷却时间
     * @param steps 要重复的步骤模板（每个步骤的hitTick是相对于该次重复的起始时间）
     */
    public SequenceAnim(int round, int interval, int cd, List<SingleAnim<T>> steps) {
        this.cd = cd;
        this.steps = new ArrayList<>(steps.size() * round);
        this.steps.addAll(steps);
        for (int i = 1; i < round; i++) {
            for (SingleAnim<T> step : steps) {
                this.steps.add(new SingleAnim<>(step.id,step.hitTicks,step.action,i * interval));
            }
        }
    }

    /**
     * 回合，单SingleAnim，单判定时机
     */
    public SequenceAnim(int round, int interval, int cd,byte id, int animTick,int hitTick,T action) {
        this.cd = cd;
        this.steps = new ArrayList<>();
        for (int i = 0; i < round; i++) {
            this.steps.add(new SingleAnim<>(id,i * interval + animTick,i * interval + hitTick,action));
        }
    }

    /**
     * 回合，单SingleAnim，多判定时机
     */
    public SequenceAnim(int round, int interval, int cd,byte id, int animTick,int[] hitTick,T action) {
        this.cd = cd;
        this.steps = new ArrayList<>();
        for (int i = 0; i < round; i++) {
            this.steps.add(new SingleAnim<>(id,animTick,hitTick,action,i * interval));
        }
    }
    public void addLength(int step,int increment) {
        for (int i = step; i < steps.size(); i++) {
            steps.get(i).applyOffset(increment);
        }
    }

    public List<SingleAnim<T>> getSteps() {return steps;}
    public int getCd() {
        return cd;
    }

}