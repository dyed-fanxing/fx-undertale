package com.sakpeipei.undertale.common.anim;

import net.minecraft.world.entity.LivingEntity;

import java.util.function.ToIntFunction;

/**
 * 服务端行动/执行/行为事件
 * @param hitTick 触发Tick
 * @param action 行动/执行/行为
 * @param duration 时长
 */
public record ActionEvent(int hitTick, ToIntFunction<LivingEntity> action, int duration) {
}
