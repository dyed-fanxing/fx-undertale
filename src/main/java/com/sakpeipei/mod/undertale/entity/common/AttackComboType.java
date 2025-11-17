package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 14:04
 */
public class AttackComboType{

    private String animName;
    private int round;
    private AttackUnit[] steps;
    private int duration;

    public AttackComboType(String animName, int round, AttackUnit[] steps) {
        this.animName = animName;
        this.round = round;
        this.steps = steps;
        for (AttackUnit step : steps) {
            duration += step.getCd();
        }
    }

    public AttackComboType(String animName,AttackUnit[] steps){
        this(animName,1,steps);
    }
    public AttackComboType(String animName,int round) {
        this(animName,round,new AttackUnit[0]);
    }


    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getAnimName() {
        return animName;
    }

    public void setAnimName(String animName) {
        this.animName = animName;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public AttackUnit[] getSteps() {
        return steps;
    }

    public void setSteps(AttackUnit[] steps) {
        this.steps = steps;
    }
}
