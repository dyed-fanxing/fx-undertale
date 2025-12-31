package com.sakpeipei.undertale.entity.common.anim;

/**
 * @author Sakqiongzi
 * @since 2025-12-14 17:53
 */
public class Step<T> {
    final byte id;   // 动画id
    int animTick;  // 触发动画Tick
    int hitTick;   // 判定Tick
    final T action;      // 执行什么动作

    public Step(byte id,int animTick,int hitTick, T action) {
        this.animTick = animTick;
        this.hitTick = hitTick;
        this.id = id;
        this.action = action;
    }
    public Step(byte id, int hitTick, T action) {
        this(id, -1, hitTick, action);
    }

    public byte getId() {
        return id;
    }

    public int getAnimTick() {
        return animTick;
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
