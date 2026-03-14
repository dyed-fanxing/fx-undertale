package com.fanxing.fx_undertale.entity;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 攻击者自定义死亡消息接口
 * 实现此接口的攻击者可以自定义死亡消息中的名词和动词
 */
public interface CustomDeathMessage {

    /**
     * 自定义名词（攻击物或招式名称）
     * @param attackObject 攻击物实体
     * @return 自定义名词，返回 null 则使用默认（攻击物名称）
     */
    @Nullable
    default String getCustomNoun(Entity attackObject) {
        return null;
    }

    /**
     * 自定义动词（攻击动作）
     * @param attackObject 攻击物实体
     * @return 自定义动词，返回 null 则使用默认动词
     */
    @Nullable
    default String getCustomVerb(Entity attackObject) {
        return null;
    }
}