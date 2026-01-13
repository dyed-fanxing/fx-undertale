package com.sakpeipei.undertale.common.anim;

import com.sakpeipei.undertale.common.function.FloatSupplier;
import net.minecraft.sounds.SoundEvent;

/**
 * @author Sakqiongzi
 * @since 2025-12-14 17:53
 * 判定步骤
 */
public class AnimStepT<T>{
    final byte id;   // 动画id
    int animTick;  // 触发动画Tick
    int[] hitTicks;   // 判定Ticks
    private final FloatSupplier speedModifier;
    final T action;      // 执行什么动作

    public AnimStepT(byte id, int animTick, int[] hitTicks, FloatSupplier speedModifier, T action) {
        this.animTick = animTick;
        this.hitTicks = hitTicks;
        this.id = id;
        this.speedModifier = speedModifier;
        this.action = action;
    }
    public AnimStepT(byte id, int animTick, int hitTick, FloatSupplier speedModifier, T action) {
        this(id, animTick, new int[]{hitTick}, speedModifier, action);
    }
    public AnimStepT(byte id, int[] hitTicks, FloatSupplier speedModifier, T action) {
        this(id, -1, hitTicks, speedModifier, action);
    }

    /**
     * 创建偏移所有时间点的
     * @param offset 偏移量（增加或减少）Tick
     */
    public AnimStepT(byte id, int animTick, int[] hitTicks, FloatSupplier speedModifier, T action, int offset) {
        this.speedModifier = speedModifier;
        this.animTick = animTick + offset;
        for (int i = 0; i < hitTicks.length; i++) {
            hitTicks[i] = hitTicks[i] + offset;
        }
        this.hitTicks = hitTicks;
        this.id = id;
        this.action = action;
    }

    public boolean shouldTriggerAnim(int animTick) {
        return (int)(this.animTick * speedModifier.get()) == animTick;
    }
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            hitTick = (int) (hitTick * speedModifier.get());
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
    public float getSpeed() {
        return speedModifier.get();
    }
    public T getAction() {
        return action;
    }
    public SoundEvent getSoundEvent() {
        return null;
    }

}
