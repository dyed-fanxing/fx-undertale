package com.sakpeipei.undertale.entity.ai.anim;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.ToIntFunction;

/**
 * @param id       动画id
 * @param hitTicks 判定Ticks
 * @param length   动画时长 施法/动画/动作
 * @param cd       攻击后摇CD
 * @param action   执行什么动作
 * @author Sakqiongzi
 * @since 2025-11-15 15:39
 * 单次动画
 */
public record SingleAnim(byte id, int[] hitTicks, int length, int cd, ToIntFunction<LivingEntity> action) {
    /**
     * 单判定
     */
    public SingleAnim(byte id, int hitTick, int length, int cd, ToIntFunction<LivingEntity> action) {
        this(id, new int[]{hitTick}, length, cd, action);
    }

    // 在该tick是否判定
    public boolean shouldHitAt(int currentTick) {
        for (int hitTick : hitTicks) {
            if (hitTick == currentTick) {
                return true;
            }
        }
        return false;
    }

    // 在该tick是否播放音效
    public boolean shouldPlaySoundAt(int animTick) {
        return false;
    }


    public SoundEvent getSoundEvent() {
        return null;
    }

    @Override
    public @NotNull String toString() {
        return "SingleAnim{" +
                "id=" + id +
                ", hitTicks=" + Arrays.toString(hitTicks) +
                ", length=" + length +
                ", cd=" + cd +
                ", action=" + action +
                '}';
    }
}