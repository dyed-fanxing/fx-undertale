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

    public SequenceAnim(byte id,int hitTick,int length, int cd,T action) {
        this.cd = cd;
        this.steps = List.of(new SingleAnim<>(id,hitTick,length,0,action));
    }
    public SequenceAnim(byte id,int[] hitTick,int length, int cd,T action) {
        this.cd = cd;
        this.steps = List.of(new SingleAnim<>(id,hitTick,length,0,action));
    }
    /**
     * @param cd 冷却时间
     * @param steps 步骤列表
     */
    public SequenceAnim( int cd,List<SingleAnim<T>> steps) {
        this.cd = cd;
        this.steps = new ArrayList<>(steps);
        int increment = 0;
        for (SingleAnim<T> step : steps) {
            step.applyOffset(increment);
            increment += step.length;
        }
    }

    /**
     * 通过指定的回合数，构造重复的序列
     * @param round 回合数 - 重复次数
     * @param cd 冷却时间
     * @param steps 要重复的步骤模板（每个步骤的hitTick是相对于该次重复的起始时间）
     */
    public SequenceAnim(int round,int cd,List<SingleAnim<T>> steps) {
        this.cd = cd;
        this.steps = new ArrayList<>(steps.size() * round);
        this.steps.addAll(steps);

        int increment = 0;
        for (int i = 1; i < round; i++) {
            for (SingleAnim<T> step : steps) {
                increment += step.length;
                this.steps.add(new SingleAnim<>(step.id,step.hitTicks,step.length,step.cd,step.action,increment));
            }
        }
    }

    /**
     * 回合，单SingleAnim
     */
    public SequenceAnim(int round,int cd,SingleAnim<T> step) {
        this.cd = cd;
        this.steps = new ArrayList<>(round);
        this.steps.add(step);

        int increment = 0;
        for (int i = 1; i < round; i++) {
            increment += step.length;
            this.steps.add(new SingleAnim<>(step.id,step.hitTicks,step.length,step.cd,step.action,increment));
        }
    }

    public void applyOffset(int step,int offset) {
        for (int i = step; i < steps.size(); i++) {
            steps.get(i).applyOffset(offset);
        }
    }


    public List<SingleAnim<T>> getSteps() {return steps;}
    public int getCd() {
        return cd;
    }

}