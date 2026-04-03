package com.fanxing.fx_undertale.entity.boss.sans;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.dialogue.DialogueMessageProvider;
import com.fanxing.fx_undertale.entity.dialogue.EntityDialogue;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;

import javax.swing.text.html.parser.Entity;

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