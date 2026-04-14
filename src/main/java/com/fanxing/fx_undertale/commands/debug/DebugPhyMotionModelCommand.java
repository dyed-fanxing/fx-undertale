package com.fanxing.fx_undertale.commands.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class DebugPhyMotionModelCommand {
    @SubscribeEvent
    public static void onBone(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("magicbone")
                .then(Commands.literal("set")
                        .then(Commands.argument("scale", FloatArgumentType.floatArg())
                                .then(Commands.argument("growScale", FloatArgumentType.floatArg())
                                        .then(Commands.argument("lifetime", IntegerArgumentType.integer())
                                                .then(Commands.argument("angularVelocity", FloatArgumentType.floatArg(-180, 180))
                                                        // gravity 模型
                                                        .then(Commands.literal("gravity")
                                                                .then(Commands.argument("strength", FloatArgumentType.floatArg(0))
                                                                        .then(Commands.argument("softening", FloatArgumentType.floatArg(0))
                                                                                .then(Commands.argument("speed", FloatArgumentType.floatArg(0))
                                                                                        // 可选角度 - 直接添加，不嵌套
                                                                                        .then(Commands.argument("yaw", FloatArgumentType.floatArg(-90, 90))
                                                                                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                                                                                        .then(Commands.argument("roll", FloatArgumentType.floatArg(-180, 180))
                                                                                                                .executes(DebugPhyMotionModelCommand::setGravityParameters)
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                        .executes(DebugPhyMotionModelCommand::setGravityParameters)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        // spring 模型
                                                        .then(Commands.literal("spring")
                                                                .then(Commands.argument("frequency", FloatArgumentType.floatArg(0))
                                                                        .then(Commands.argument("damping", FloatArgumentType.floatArg(0))
                                                                                .then(Commands.argument("speed", FloatArgumentType.floatArg(0))
                                                                                        .then(Commands.argument("yaw", FloatArgumentType.floatArg(-90, 90))
                                                                                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                                                                                        .then(Commands.argument("roll", FloatArgumentType.floatArg(-180, 180))
                                                                                                                .executes(DebugPhyMotionModelCommand::setSpringParameters)
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                        .executes(DebugPhyMotionModelCommand::setSpringParameters)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        // proportional 模型
                                                        .then(Commands.literal("proportional")
                                                                .then(Commands.argument("turnRate", FloatArgumentType.floatArg(0, 1.0f))
                                                                        .then(Commands.argument("speed", FloatArgumentType.floatArg(0))
                                                                                .then(Commands.argument("angularVelocity", FloatArgumentType.floatArg(-180, 180))
                                                                                        .then(Commands.argument("yaw", FloatArgumentType.floatArg(-90, 90))
                                                                                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                                                                                        .then(Commands.argument("roll", FloatArgumentType.floatArg(-180, 180))
                                                                                                                .executes(DebugPhyMotionModelCommand::setProportionalParameters)
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                        .executes(DebugPhyMotionModelCommand::setProportionalParameters)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                        // oscillation 模型
                                                        .then(Commands.literal("oscillation")
                                                                .then(Commands.argument("speed", FloatArgumentType.floatArg(0))
                                                                        .then(Commands.argument("angle", FloatArgumentType.floatArg(-180, 180))
                                                                                .then(Commands.argument("yaw", FloatArgumentType.floatArg(-90, 90))
                                                                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                                                                                .then(Commands.argument("roll", FloatArgumentType.floatArg(-180, 180))
                                                                                                        .executes(DebugPhyMotionModelCommand::setOscillationParameters)
                                                                                                )
                                                                                        )
                                                                                )
                                                                                .executes(DebugPhyMotionModelCommand::setOscillationParameters)
                                                                        )
                                                                )
                                                        )
                                                )
                                        ))

                        )
                ));
    }

    private static int setGravityParameters(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Player player = source.getPlayer();
        if (player == null) return 0;

        float strength = FloatArgumentType.getFloat(ctx, "strength");
        float softening = FloatArgumentType.getFloat(ctx, "softening");
        float speed = FloatArgumentType.getFloat(ctx, "speed");

        CompoundTag data = player.getPersistentData();
        data.putString("model_type", "gravity");
        data.putFloat("strength", strength);
        data.putFloat("softening", softening);
        data.putFloat("speed", speed);

        source.sendSuccess(() -> Component.literal(
                String.format("[引力模型] 参数已设置: strength=%.2f, softening=%.2f, speed=%.2f", strength, softening, speed)), true);
        return 1;
    }

    private static int setSpringParameters(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Player player = source.getPlayer();
        if (player == null) return 0;

        float frequency = FloatArgumentType.getFloat(ctx, "frequency");
        float damping = FloatArgumentType.getFloat(ctx, "damping");
        float speed = FloatArgumentType.getFloat(ctx, "speed");
        float angularVelocity = FloatArgumentType.getFloat(ctx, "angularVelocity");
        float roll = FloatArgumentType.getFloat(ctx, "roll");
        float pitch = FloatArgumentType.getFloat(ctx, "pitch");
        float yaw = FloatArgumentType.getFloat(ctx, "yaw");
        float growScale = FloatArgumentType.getFloat(ctx, "growScale");
        float scale = FloatArgumentType.getFloat(ctx, "scale");
        int lifetime = IntegerArgumentType.getInteger(ctx, "lifetime");

        CompoundTag data = player.getPersistentData();
        data.putString("model_type", "spring");
        data.putFloat("frequency", frequency);
        data.putFloat("damping", damping);
        data.putFloat("speed", speed);
        data.putFloat("angularVelocity", angularVelocity);
        data.putFloat("roll", roll);
        data.putFloat("pitch", pitch);
        data.putFloat("yaw", yaw);
        data.putInt("lifetime", lifetime);
        data.putFloat("scale", scale);
        data.putFloat("growScale", growScale);

        source.sendSuccess(() -> Component.literal(
                String.format("[弹簧模型] 参数已设置: frequency=%.2f, damping=%.2f, speed=%.2f, angularVelocity=%.1f°",
                        frequency, damping, speed, angularVelocity)), true);
        return 1;
    }

    // 新增比例导引参数处理方法
    private static int setProportionalParameters(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Player player = source.getPlayer();
        if (player == null) return 0;

        float turnRate = FloatArgumentType.getFloat(ctx, "turnRate");
        float speed = FloatArgumentType.getFloat(ctx, "speed");
        float angle = FloatArgumentType.getFloat(ctx, "angle");

        CompoundTag data = player.getPersistentData();
        data.putString("model_type", "proportional");
        data.putFloat("turnRate", turnRate);
        data.putFloat("speed", speed);
        data.putFloat("angle", angle);

        source.sendSuccess(() -> Component.literal(
                String.format("[比例导引] 参数已设置: turnRate=%.3f, speed=%.2f, angle=%.1f°",
                        turnRate, speed, angle)), true);
        return 1;
    }

    private static int setOscillationParameters(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Player player = source.getPlayer();
        if (player == null) return 0;

        float speed = FloatArgumentType.getFloat(ctx, "speed");
        float angle = FloatArgumentType.getFloat(ctx, "angle");

        CompoundTag data = player.getPersistentData();
        data.putString("model_type", "oscillation");
        data.putFloat("speed", speed);
        data.putFloat("angle", angle);

        source.sendSuccess(() -> Component.literal(
                String.format("[振荡模型] 参数已设置: speed=%.2f, angle=%.1f°", speed, angle)), true);
        return 1;
    }
}
