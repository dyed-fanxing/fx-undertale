package com.fanxing.fx_undertale.commands.debug;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

/**
 * @author FanXing
 * @since 2026-01-08 22:13
 */
@EventBusSubscriber(modid = FxUndertale.MOD_ID)
public class DebugCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 直接注册命令
        event.getDispatcher().register(
                Commands.literal("sans").requires(css -> css.hasPermission(3)).then(
                        Commands.argument("attackId", IntegerArgumentType.integer(1, 50))
                                .executes(cc -> {
                                    ServerPlayer player = cc.getSource().getPlayer();
                                    if (player != null) {
                                        int viewDis = player.requestedViewDistance() * 16;
                                        Vec3 pos = player.position();
                                        Sans sans = player.level().getNearestEntity(Sans.class, TargetingConditions.forNonCombat(), player, pos.x, pos.y, pos.z, new AABB(pos.x - viewDis, pos.y - viewDis, pos.z - viewDis, pos.x + viewDis, pos.y + viewDis, pos.z + viewDis));
                                        if (sans != null) {
                                            sans.testAttackId = IntegerArgumentType.getInteger(cc, "attackId");
                                        }
                                    }
                                    return 1;
                                })
                )
        );

        event.getDispatcher().register(
                Commands.literal("gravity")
                        .requires(css -> css.hasPermission(3))
                        .then(
                                Commands.argument("targets", EntityArgument.entities())
                                        .then(
                                                Commands.argument("direction", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            builder.suggest("UP");
                                                            builder.suggest("DOWN");
                                                            builder.suggest("NORTH");
                                                            builder.suggest("SOUTH");
                                                            builder.suggest("EAST");
                                                            builder.suggest("WEST");
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ctx -> {
                                                            Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                                            String directionStr = StringArgumentType.getString(ctx, "direction");

                                                            Direction direction = Direction.valueOf(directionStr.toUpperCase());
                                                            for (Entity target : targets) {
                                                                GravityUtils.applyGravity(target, direction);
                                                            }
                                                            ctx.getSource().sendSuccess(() ->
                                                                            Component.literal("应用重力方向 " + directionStr + " 到 " + targets.size() + " 个实体"),
                                                                    true
                                                            );
                                                            return 1;
                                                        })
                                        )
                        )
                        // 快捷命令：gravity @e down
                        .then(
                                Commands.literal("down")
                                        .then(
                                                Commands.argument("targets", EntityArgument.entities())
                                                        .executes(ctx -> {
                                                            Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                                            for (Entity target : targets) {
                                                                GravityUtils.applyGravity(target, Direction.DOWN);
                                                            }
                                                            return 1;
                                                        })
                                        )
                        )
                        // 快捷命令：gravity @e up
                        .then(
                                Commands.literal("up")
                                        .then(
                                                Commands.argument("targets", EntityArgument.entities())
                                                        .executes(ctx -> {
                                                            Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                                            for (Entity target : targets) {
                                                                GravityUtils.applyGravity(target, Direction.UP);
                                                            }
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }
}
