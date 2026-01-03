package com.sakpeipei.undertale.common.anim;

import net.minecraft.sounds.SoundEvent;

/**
 * @author Sakqiongzi
 * @since 2025-12-14 17:53
 * 判定步骤
 */
public class AnimStep<T>{
    final byte id;   // 动画id
    int animTick;  // 触发动画Tick
    int[] hitTicks;   // 判定Ticks
    final T action;      // 执行什么动作

    public AnimStep(byte id, int animTick, int[] hitTicks, T action) {
        this.animTick = animTick;
        this.hitTicks = hitTicks;
        this.id = id;
        this.action = action;
    }
    public AnimStep(byte id,int animTick, int hitTick, T action) {
        this(id, animTick, new int[]{hitTick}, action);
    }
    public AnimStep(byte id, int[] hitTicks, T action) {
        this(id, -1, hitTicks, action);
    }

    /**
     * 创建偏移所有时间点的
     * @param offset 偏移量（增加或减少）Tick
     */
    public AnimStep(byte id,int animTick, int[] hitTicks, T action,int offset) {
        this.animTick = animTick + offset;
        for (int i = 0; i < hitTicks.length; i++) {
            hitTicks[i] = hitTicks[i] + offset;
        }
        this.hitTicks = hitTicks;
        this.id = id;
        this.action = action;
    }

    public boolean shouldTriggerAnim(int animTick) {
        return this.animTick == animTick;
    }
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            if(hitTick == currentTick){
                return true;
            }
        }
        return false;
    }
    /**
     * 应用偏移
     * @param offset 偏移量
     */
    public void applyOffset(int offset) {
        for (int i = 0; i < hitTicks.length; i++) {
            this.hitTicks[i] += offset;
        }
        this.animTick += offset;
    }

    public byte getId() {
        return id;
    }

    public int[] getHitTicks() {
        return hitTicks;
    }

    public SoundEvent getSoundEvent() {
        return null;
    }

    public T getAction() {
        return action;
    }

}
