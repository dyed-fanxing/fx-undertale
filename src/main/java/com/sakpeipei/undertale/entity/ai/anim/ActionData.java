package com.sakpeipei.undertale.entity.ai.anim;

/**
 * @author Sakqiongzi
 * @since 2025-11-22 23:14
 * 用于执行方法的数据
 */
public record ActionData(int id, int... params) {
    public int getParam(int index) {
        return params.length > index ? params[index] : 0;
    }

    public boolean hasParam(int index) {
        return params.length > index;
    }
}