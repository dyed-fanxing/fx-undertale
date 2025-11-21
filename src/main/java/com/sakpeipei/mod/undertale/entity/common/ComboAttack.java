package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author yujinbao
 * @since 2025/11/21 15:27
 */
public class ComboAttack{
    private int round;
    private AttackUnit[] steps;


    public ComboAttack(int round,AttackUnit[] steps) {
        this.round = round;
        this.steps = steps;
    }
    public ComboAttack(AttackUnit[] steps) {
        this(0,steps);
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
