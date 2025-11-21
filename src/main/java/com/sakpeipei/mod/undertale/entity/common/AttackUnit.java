package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author yujinbao
 * @since 2025/11/21 15:11
 */
public abstract class AttackUnit implements AttackTiming{
    private final int id;
    private final int cd;
    private int[] params;

    protected AttackUnit(int id, int cd,int ...params) {
        this.id = id;
        this.cd = cd;
        this.params = params;
    }


    public int getId() {
        return id;
    }

    public int getCd() {
        return cd;
    }

    public int[] getParams() {
        return params;
    }

    public void setParams(int ...param) {
        this.params = param;
    }

}
