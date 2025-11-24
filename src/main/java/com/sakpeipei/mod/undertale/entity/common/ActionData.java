package com.sakpeipei.mod.undertale.entity.common;

/**
 * @author Sakqiongzi
 * @since 2025-11-22 23:14
 * 用于执行方法的数据
 */
public class ActionData {
    private final int id;
    private final int[] params;

    public ActionData(int id, int ...params) {
        this.id = id;
        this.params = params;
    }
    public int getId() {
        return id;
    }
    public int[] getParams() {
        return params;
    }
    public int getParam(int index) {
        return params.length > index ? params[index] : 0;
    }
    public boolean hasParam(int index) {
        return params.length > index;
    }
}