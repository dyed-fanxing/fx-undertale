package com.fanxing.fx_undertale.entity.boss.sans;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.dialogue.DialogueMessageProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Sans 对话管理
 * 所有 Sans 相关的对话调用
 */
@EventBusSubscriber(modid = FxUndertale.MOD_ID)
public class SansDialogue {
    public static final String MERCY_COMMAND = "/" + FxUndertale.MOD_ID + ":mercy %s";

    // 显示仁慈阶段对话
    public static void mercy(ServerPlayer player, Sans sans) {
        String dialogueKey = DialogueMessageProvider.DIALOGUE_PRE + "entity.mercy";
        String mercyKey = DialogueMessageProvider.DIALOGUE_PRE + "mercy";
        if (!(Language.getInstance().has(dialogueKey) && Language.getInstance().has(mercyKey))) {
            return;
        }
        MutableComponent theForgiver = Component.literal("[").withStyle(ChatFormatting.BLUE)
                .append(DialogueMessageProvider.getSpeakerName(sans))
                .append(Component.literal("] "));
        Component mercy = Component.literal(" ").append(Component.translatable(mercyKey).withStyle(Style.EMPTY
                .withColor(ChatFormatting.YELLOW)
                .withUnderlined(true)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(MERCY_COMMAND, sans.getUUID())))
        )).append(" ");
        player.sendSystemMessage(Component.translatable(dialogueKey, theForgiver, mercy));
    }

    // 未来可以添加更多对话方法：
    // public static void showIntro(ServerPlayer player, Sans sans) { ... }
    // public static void showDeath(ServerPlayer player, Sans sans) { ... }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(FxUndertale.MOD_ID + ":mercy")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))  // 对所有客户端隐藏命令（包括 Tab 补全和命令提示）
                .then(Commands.argument("entity", net.minecraft.commands.arguments.EntityArgument.entity())
                        .executes(context -> {
                            Entity entity = net.minecraft.commands.arguments.EntityArgument.getEntity(context, "entity");
                            if (entity instanceof Sans sans) {
                                sans.setMercyTriggered(true);
                                return 1;
                            }
                            return 0;
                        })
                )
        );
    }
}