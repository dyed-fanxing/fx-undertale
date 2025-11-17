package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 */
public class AttackUnit {
    private final int id;
    private final int cd;
    private final int[] hitTick;
    private final boolean isTriggerAnim;
    private int[] params;

    public AttackUnit(int id, int cd, int[] hitTick, boolean isTriggerAnim, int... params) {
        this.id = id;
        this.cd = cd;
        this.hitTick = hitTick;
        this.isTriggerAnim = isTriggerAnim;
        this.params = params;
    }

    public AttackUnit(int id, int cd, int[] hitTick, int... params) {
        this(id, cd, hitTick, true, params);
    }

    public int getId() {
        return id;
    }

    public int getCd() {
        return cd;
    }

    public int[] getHitTick() {
        return hitTick;
    }

    public int[] getParams() {
        return params;
    }

    public boolean isTriggerAnim() {
        return isTriggerAnim;
    }

    public void setParams(int ...param) {
        this.params = param;
    }
}