package com.sakpeipei.undertale.common.anim;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.List;
import java.util.function.*;

/**
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 单次动画
 */
public class SingleAnim<T>{
    final byte id;   // 动画id
    int[] hitTicks;   // 判定Ticks
    protected int length; // 动画时长 施法/动画/动作
    protected float speed = 1.0f; // 播放速度
    protected final int cd; // 攻击后摇CD
    final T action;      // 执行什么动作


    public SingleAnim(byte id, int hitTick, int length, int cd,T action) {
        this(id,hitTick,length,1.0f,cd,action);
    }
    /**
     * 单判定
     */
    public SingleAnim(byte id, int hitTick, int length,float speed, int cd, T action) {
        this.id = id;
        this.hitTicks = new int[]{(int) (hitTick * speed)};
        this.length = (int) Math.ceil(length * speed);
        this.cd = cd;
        this.action = action;
        this.speed = speed;
    }
    /**
     * 带判定，带偏移的
     */
    public SingleAnim(byte id, int hitTick, int length,float speed, int cd, T action,int offset) {
        this.id = id;
        this.hitTicks = new int[]{(int) ((hitTick + offset) * speed)};
        this.length = (int) Math.ceil((length+offset) * speed);
        this.cd = cd + offset;
        this.action = action;
        this.speed = speed;
    }


    public SingleAnim(byte id, int[] hitTicks, int length, int cd,T actions) {
        this(id,hitTicks,length,1.0f,cd,actions);
    }
    /**
     * 多判定
     */
    public SingleAnim(byte id,int[] hitTicks, int length,float speed, int cd, T action) {
        this.id = id;
        for (int i = 0; i < hitTicks.length; i++) {
            hitTicks[i] = (int) (hitTicks[i] * speed);
        }
        this.hitTicks = hitTicks;
        this.length = (int) Math.ceil(length * speed);
        this.cd = cd;
        this.action = action;
        this.speed = speed;
    }
    public SingleAnim(byte id, int[] hitTicks, int length, int cd, T action, int offset) {
        this(id,hitTicks,length,1.0f,cd,action,offset);
    }
    /**
     * 多判定，带有偏移的
     */
    public SingleAnim(byte id,int[] hitTicks, int length,float speed, int cd, T action,int offset) {
        this.id = id;
        for (int i = 0; i < hitTicks.length; i++) {
            hitTicks[i] = (int) ((hitTicks[i]+offset) * speed);
        }
        this.hitTicks = hitTicks;
        this.length = (int) Math.ceil((length+offset) * speed);
        this.cd = cd;
        this.action = action;
        this.speed = speed;
    }

    // 在该tick是否判定
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
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

    public void applyOffset(int offset) {
        for (int i = 0; i < hitTicks.length; i++) {
            hitTicks[i] += offset;
        }
        this.length += offset;
    }

    public void offsetLength(int offset) {
        this.length += offset;
    }

    public byte getId() {
        return id;
    }
    public int[] getHitTicks() {
        return hitTicks;
    }
    public int getLength() {
        return length;
    }
    public int getCd() {
        return cd;
    }
    public float getSpeed() {
        return speed;
    }

    public T getAction() {
        return action;
    }
    public SoundEvent getSoundEvent() {
        return null;
    }


    @Override
    public String toString() {
        return "SingleAnim{" +
                "id=" + id +
                ", hitTicks=" + Arrays.toString(hitTicks) +
                ", length=" + length +
                ", speed=" + speed +
                ", cd=" + cd +
                ", action=" + action +
                '}';
    }
}