package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 单次判定攻击
 */
public class OnceTimingAttack extends AttackUnit{
    private final int hitTick;

    public OnceTimingAttack(int id, int cd, int hitTick,int... params) {
        super(id, cd, params);
        this.hitTick = hitTick;
    }

    public int getHitTick() {
        return hitTick;
    }

    @Override
    public boolean shouldHitAt(int currentTick) {
        return this.hitTick == currentTick;
    }
}