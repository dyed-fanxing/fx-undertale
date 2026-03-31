package com.fanxing.fx_undertale.entity.dialogue;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 对话消息提供者
 * 格式：dialogue.{实体类型}.{任意扩展键}
 */
public class DialogueMessageProvider {
    public static final String DIALOGUE_PRE = "dialogue." + FxUndertale.MOD_ID + ".";
    private static final Logger log = LoggerFactory.getLogger(DialogueMessageProvider.class);

    /**
     * 显示对话（带参数）
     *
     * @param player 玩家
     * @param entity 实体
     * @param key    扩展键（如 "mercy", "intro"）
     * @param args   参数（Component 对象，用于替换文本中的 %s 占位符）
     */
    public static void showDialogue(ServerPlayer player, Entity entity, String key, Component... args) {
        player.sendSystemMessage(Objects.requireNonNull(getBaseComponent(player, entity, key, args)));
    }

    /**
     * 获取基础消息组件，显示对话（带参数）
     *
     * @param player 玩家
     * @param entity 实体
     * @param key    扩展键（如 "mercy", "intro"）
     * @param args   参数（Component 对象，用于替换文本中的 %s 占位符）
     */
    public static Component getBaseComponent(ServerPlayer player, Entity entity, String key, Component... args) {

        String entityTypePath = EntityType.getKey(entity.getType()).getPath();
        String dialogueKey = DIALOGUE_PRE + entityTypePath + "." + key;
        log.info("EntityTypeDescriptonId: {}, EntityType.getKey：{},Path:{}", entity.getType().getDescriptionId(), EntityType.getKey(entity.getType()), entityTypePath);
        if (!Language.getInstance().has(dialogueKey)) {
            return null;
        }

        // 显示消息（带参数）
        Component speakerName = getSpeakerName(entity);
        var color = getSpeakerColor();

        return Component.literal("[").withStyle(color)
                .append(speakerName)
                .append(Component.literal("] "))
                .append(Component.translatable(dialogueKey, (Object[]) args));
    }

    /**
     * 获取说话者名字（自定义优先）
     */
    public static Component getSpeakerName(Entity entity) {
        if (entity.hasCustomName()) {
            return entity.getCustomName();
        } else {
            return Component.translatable(entity.getType().getDescriptionId());
        }
    }

    /**
     * 获取说话者颜色
     */
    public static net.minecraft.ChatFormatting getSpeakerColor() {
        return net.minecraft.ChatFormatting.WHITE;
    }

}