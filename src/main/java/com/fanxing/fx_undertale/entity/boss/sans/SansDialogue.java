package com.fanxing.fx_undertale.entity.boss.sans;

import com.fanxing.lib.entity.dialogue.DialogueMessageProvider;
import com.fanxing.lib.entity.dialogue.EntityDialogue;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sans 对话管理
 * 所有 Sans 相关的对话调用
 */
public class SansDialogue {
    public static final String MERCY = "mercy";
    public static final String MERCY_COMMAND = EntityDialogue.CLICK_COMMAND_PACKET_PRE + MERCY+" %d";

    // 显示仁慈阶段对话
    public static void mercy(ServerPlayer player, Sans sans) {
        String dialogueKey = DialogueMessageProvider.DIALOGUE_PRE + "entity."+MERCY;
        String mercyKey = DialogueMessageProvider.DIALOGUE_PRE +MERCY;
        if (!(Language.getInstance().has(dialogueKey) && Language.getInstance().has(mercyKey))) {
            return;
        }
        MutableComponent theForgiver = Component.literal("[").withStyle(ChatFormatting.AQUA)
                .append(DialogueMessageProvider.getSpeakerName(sans))
                .append(Component.literal("] "));
        Component mercy = Component.literal(" ").append(Component.translatable(mercyKey).withStyle(Style.EMPTY
                .withColor(ChatFormatting.YELLOW)
                .withUnderlined(true)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(MERCY_COMMAND,sans.getId())))
        )).append(" ");
        player.sendSystemMessage(Component.translatable(dialogueKey, theForgiver, mercy));
    }
}