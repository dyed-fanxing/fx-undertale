package com.sakpeipei.undertale.entity.ai.anim;

import com.sakpeipei.undertale.common.function.FloatSupplier;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 单次动画
 */
public class SingleAnimT<T>{
    final byte id;   // 动画id
    int animTick;  // 触发动画Tick
    int[] hitTicks;   // 判定Ticks
    protected int length; // 动画时长 施法/动画/动作
    protected FloatSupplier speedModifier; // 速度修改器
    protected final int cd; // 攻击后摇CD
    final List<T> actions;      // 执行什么动作

    public SingleAnimT(byte id, int hitTick, int length, int cd, T action) {
        this(id,new int[]{hitTick},length,null,cd,List.of(action));
    }
    public SingleAnimT(byte id, int hitTick, int length, FloatSupplier speedModifier, int cd, T action) {
        this(id,new int[]{hitTick},length,speedModifier,cd,List.of(action));
    }

    public SingleAnimT(byte id, int[] hitTicks, int length, FloatSupplier speedModifier, int cd, List<T> actions) {
        this.id = id;
        this.hitTicks = hitTicks;
        this.length = length;
        this.cd = cd;
        this.actions = actions;
        this.speedModifier = speedModifier;
    }

    // 在该tick是否判定
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            float speed = speedModifier.get();
            hitTick = (int) (hitTick * speed);
            if(hitTick == currentTick){
                return true;
            }
        }
        return false;
    }

    // 在该tick是否播放音效
    public boolean shouldPlaySoundAt(int animTick) {
        return false;
    }

    public void addHitTick(int increment) {
        for (int i = 0; i < hitTicks.length; i++) {
            hitTicks[i] += increment;
        }
    }

    public void addLength(int increment) {
        this.length += increment;
    }

    public byte getId() {
        return id;
    }
    public int[] getHitTicks() {
        return hitTicks;
    }
    public int getLength() {
        return (int) (speedModifier.get()*length);
    }
    public int getCd() {
        return cd;
    }
    public float getSpeed() {
        return speedModifier.get();
    }

    public List<T> getActions() {
        return actions;
    }
    public SoundEvent getSoundEvent() {
        return null;
    }

}