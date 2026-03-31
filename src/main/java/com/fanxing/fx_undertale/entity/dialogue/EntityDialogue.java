package com.fanxing.fx_undertale.entity.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 通用对话系统
 * 支持自定义名字（Component）和翻译键
 */
public class EntityDialogue {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, DialogueData> DIALOGUES = new HashMap<>();

    // 注册对话（由实体类调用）
    public static void registerDialogue(String id, DialogueData dialogue) {
        DIALOGUES.put(id, dialogue);
    }

    // 注册对话（通用方法，自动生成ID）
    public static void registerDialogue(net.minecraft.world.entity.EntityType<?> entityType, String phase, DialogueData dialogue) {
        String dialogueId = entityType.getDescriptionId() + ".dialogue." + phase;
        dialogue.id = dialogueId;
        
        // 自动设置节点ID为对话ID（如果只有一个节点）
        if (dialogue.nodes.size() == 1 && dialogue.nodes.containsKey(null)) {
            DialogueNode node = dialogue.nodes.remove(null);
            dialogue.nodes.put(dialogueId, node);
        }
        
        DIALOGUES.put(dialogueId, dialogue);
    }

    // 显示对话（使用实体翻译键 + .dialogue.阶段）
    public static void showDialogue(ServerPlayer player, Entity entity, String phase) {
        // 格式：实体翻译键.dialogue.阶段
        // 例如：entity.fx_undertale.sans.dialogue.mercy
        String dialogueId = entity.getType().getDescriptionId() + ".dialogue." + phase;
        show(player, dialogueId, entity);
    }

    // 显示对话（指定对话ID）
    public static void show(ServerPlayer player, String dialogueId, Entity entity) {
        DialogueData dialogue = DIALOGUES.get(dialogueId);
        if (dialogue == null) return;

        // 直接用对话ID作为节点ID
        showNode(player, dialogueId, entity);
    }

    // 显示特定节点
    public static void showNode(ServerPlayer player, String nodeId, Entity entity) {
        DialogueNode node = findNode(nodeId);
        if (node == null) return;

        // 显示消息
        Component message = formatMessage(node, entity);
        player.sendSystemMessage(message);

        // 显示选项
        node.options.forEach(option -> showOption(player, option, entity));

        // 继续下一个节点
        if (node.next != null) {
            showNode(player, node.next, entity);
        }
    }

    private static DialogueNode findNode(String nodeId) {
        for (DialogueData dialogue : DIALOGUES.values()) {
            if (dialogue.nodes.containsKey(nodeId)) {
                return dialogue.nodes.get(nodeId);
            }
        }
        return null;
    }

    private static Component formatMessage(DialogueNode node, Entity entity) {
        return switch (node.speaker) {
            case "entity" -> {
                Component name = entity.hasCustomName() ? entity.getCustomName() : Component.literal("实体");
                yield Component.literal("[").withStyle(ChatFormatting.WHITE)
                    .append(name)
                    .append(Component.literal("] "))
                    .append(Component.literal(node.text));
            }
            case "player" -> Component.literal("[你] ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(node.text));
            case "system" -> Component.literal("[系统] ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(node.text));
            default -> Component.literal("[").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(node.speaker))
                .append(Component.literal("] "))
                .append(Component.literal(node.text));
        };
    }

    private static void showOption(ServerPlayer player, DialogueOption option, Entity entity) {
        Component optionText = Component.literal(option.text)
            .withStyle(Style.EMPTY
                .withColor(option.color)
                .withUnderlined(option.underlined)
                .withBold(option.bold)
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal(option.hoverText)
                ))
            );

        if (option.clickAction != null) {
            String command = option.clickAction.command;
            if (command.contains("{entity_id}")) {
                command = command.replace("{entity_id}", String.valueOf(entity.getId()));
            }
            optionText.getStyle().withClickEvent(new ClickEvent(
                option.clickAction.action,
                command
            ));
        }

        player.sendSystemMessage(optionText);
    }

    // ========== 数据结构 - 易于JSON化 ==========

    // 对话数据
    public static class DialogueData {
        public String id;
        public Map<String, DialogueNode> nodes;

        public DialogueData(String id, Map<String, DialogueNode> nodes) {
            this.id = id;
            this.nodes = nodes;
        }
    }

    // 对话节点
    public static class DialogueNode {
        public String speaker;          // 说话者："entity", "player", "system", 或其他
        public String text;             // 文本内容
        public List<DialogueOption> options;  // 选项列表
        public String next;             // 下一个节点ID
        public DialogueCondition condition;    // 触发条件

        public DialogueNode(String speaker, String text, List<DialogueOption> options,
                           String next, DialogueCondition condition) {
            this.speaker = speaker;
            this.text = text;
            this.options = options;
            this.next = next;
            this.condition = condition;
        }
    }

    // 对话选项
    public static class DialogueOption {
        public String text;                     // 选项文本
        public ChatFormatting color;            // 文字颜色
        public boolean underlined;              // 下划线
        public boolean bold;                    // 粗体
        public ClickAction clickAction;         // 点击动作
        public String hoverText;                // 悬停提示
        public String nextNode;                 // 下一个节点ID

        public DialogueOption(String text, ChatFormatting color, boolean underlined, boolean bold,
                            ClickAction clickAction, String hoverText, String nextNode) {
            this.text = text;
            this.color = color;
            this.underlined = underlined;
            this.bold = bold;
            this.clickAction = clickAction;
            this.hoverText = hoverText;
            this.nextNode = nextNode;
        }
    }

    // 点击动作
    public static class ClickAction {
        public ClickEvent.Action action;
        public String command;

        public ClickAction(ClickEvent.Action action, String command) {
            this.action = action;
            this.command = command;
        }
    }

    // 对话条件
    public static class DialogueCondition {
        public String type;          // "hp_below", "phase_equals", "item_in_hand"
        public Object value;         // 条件值
        public boolean required;     // 是否必须满足

        public DialogueCondition(String type, Object value, boolean required) {
            this.type = type;
            this.value = value;
            this.required = required;
        }
    }

    // ========== JSON工具 ==========

    // 导出为JSON（未来用于生成JSON文件）
    public static String toJson(String dialogueId) {
        return GSON.toJson(DIALOGUES.get(dialogueId));
    }

    // 从JSON加载（未来实现）
    public static void loadFromJson(String json) {
        Type type = new TypeToken<Map<String, DialogueData>>(){}.getType();
        Map<String, DialogueData> loaded = GSON.fromJson(json, type);
        DIALOGUES.putAll(loaded);
    }
}