package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 多次判定攻击
 */
public class MultiTimingAttack extends AttackUnit {
    private final int[] hitTicks;

    public MultiTimingAttack(int id, int cd, int[] hitTicks, int... params) {
        super(id, cd,params);
        this.hitTicks = hitTicks;
    }

    public int[] getHitTicks() {
        return hitTicks;
    }

    @Override
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            if(hitTick == currentTick){
                return true;
            }
        }
        return false;
    }
}