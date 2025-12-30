package com.sakpeipei.undertale.entity.common.anim;

/**
 * @author Sakqiongzi
 * @since 2025-12-14 17:53
 */
public class Step<T> {
    final byte id;   // 动画id
    int hitTick;  // 判定tick
    final T action;      // 执行什么动作

    public Step(byte id, int hitTick, T action) {
        this.hitTick = hitTick;
        this.id = id;
        this.action = action;
    }

    public byte getId() {
        return id;
    }

    public int getHitTick() {
        return hitTick;
    }

    public T getAction() {
        return action;
    }

    public void addHitTick(int increment) {
        this.hitTick += increment;
    }
}
