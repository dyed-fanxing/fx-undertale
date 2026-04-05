package com.fanxing.fx_undertale.commands;

import com.fanxing.fx_undertale.net.packet.GravityPacket;
import com.fanxing.fx_undertale.utils.GravityUtils;
import com.mojang.brigadier.arguments.*;
import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
                        Commands.argument("function", IntegerArgumentType.integer(1, 10))
                                .executes(cc -> {
                                    ServerPlayer player = cc.getSource().getPlayer();
                                    if (player != null) {
                                        int viewDis = player.requestedViewDistance() * 16;
                                        Vec3 pos = player.position();
                                        Sans sans = player.level().getNearestEntity(Sans.class, TargetingConditions.forNonCombat(), player, pos.x, pos.y, pos.z,
                                                new AABB(pos.x-viewDis,pos.y-viewDis,pos.z-viewDis,pos.x+viewDis,pos.y+viewDis,pos.z+viewDis));
                                        if(sans != null) {
//                                            sans.setPersistentAngerTarget(player.getUUID());
                                            int function = IntegerArgumentType.getInteger(cc, "function");
                                            switch (function) {
                                                case 1 -> sans.shootBoneRingVolley(player);
                                                case 2 -> sans.shootArcSweepVolley(player);
                                                case 3 -> sans.shootAimedBarrage(player);
                                                case 4 -> sans.shootForwardBarrage(player);
                                                case 5 -> sans.summonGroundBoneSpineAtSelf();
                                                case 6 -> sans.summonGroundBoneSpineWaveAroundSelf(player);
                                            }
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
                                                                GravityUtils.applyGravity(target,Direction.DOWN);
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
                                                                GravityUtils.applyGravity(target,Direction.UP);
                                                            }
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }




    @SubscribeEvent
    public static void onRegisterCommandsG(RegisterCommandsEvent event) {
        // 定义 direction 参数（可复用）
        ArgumentType<String> directionArg = StringArgumentType.word();
        // 定义速度参数
        ArgumentType<Double> dxArg = DoubleArgumentType.doubleArg();
        ArgumentType<Double> dyArg = DoubleArgumentType.doubleArg();
        ArgumentType<Double> dzArg = DoubleArgumentType.doubleArg();

        event.getDispatcher().register(
                Commands.literal("gravity")
                        .requires(css -> css.hasPermission(2))
                        // 主命令：/gravity <targets> <direction> [velocity <dx> <dy> <dz>]
                        .then(
                                Commands.argument("targets", EntityArgument.entities())
                                        .then(
                                                Commands.argument("direction", directionArg)
                                                        .suggests((ctx, builder) -> {
                                                            for (Direction dir : Direction.values()) {
                                                                builder.suggest(dir.name());
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        // 分支1：不带速度，只切换重力
                                                        .executes(ctx -> {
                                                            Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                                            String directionStr = StringArgumentType.getString(ctx, "direction");
                                                            Direction direction = Direction.valueOf(directionStr.toUpperCase());
                                                            for (Entity target : targets) {
                                                                GravityUtils.applyGravity(target, direction);
                                                                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,new GravityPacket(target.getId(),direction));
                                                            }
                                                            ctx.getSource().sendSuccess(() ->
                                                                            Component.literal("应用重力方向 " + directionStr + " 到 " + targets.size() + " 个实体"),
                                                                    true
                                                            );
                                                            return 1;
                                                        })
                                                        // 分支2：带速度，切换重力后设置速度
                                                        .then(
                                                                Commands.literal("velocity")
                                                                        .then(
                                                                                Commands.argument("dx", dxArg)
                                                                                        .then(
                                                                                                Commands.argument("dy", dyArg)
                                                                                                        .then(
                                                                                                                Commands.argument("dz", dzArg)
                                                                                                                        .executes(ctx -> {
                                                                                                                            Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                                                                                                            String directionStr = StringArgumentType.getString(ctx, "direction");
                                                                                                                            Direction direction = Direction.valueOf(directionStr.toUpperCase());
                                                                                                                            double dx = DoubleArgumentType.getDouble(ctx, "dx");
                                                                                                                            double dy = DoubleArgumentType.getDouble(ctx, "dy");
                                                                                                                            double dz = DoubleArgumentType.getDouble(ctx, "dz");
                                                                                                                            for (Entity target : targets) {
                                                                                                                                GravityUtils.applyGravity(target, direction);
                                                                                                                                target.setDeltaMovement(dx, dy, dz);
                                                                                                                                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,new GravityPacket(target.getId(),direction, (float) dy));
                                                                                                                                // 2. 设置速度
                                                                                                                                if (target instanceof ServerPlayer player) {
                                                                                                                                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                                                                                                                                }
                                                                                                                            }
                                                                                                                            ctx.getSource().sendSuccess(() ->
                                                                                                                                            Component.literal("应用重力方向 " + directionStr + " 到 " + targets.size() + " 个实体，并设置速度为 (" + dx + ", " + dy + ", " + dz + ")"),
                                                                                                                                    true
                                                                                                                            );
                                                                                                                            return 1;
                                                                                                                        })
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        // 原有的 /gravity down <targets> 快捷方式（可选保留）
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
                        // 原有的 /gravity velocity 子命令（独立设置速度，不改变重力）
                        .then(
                                Commands.literal("velocity")
                                        .then(
                                                Commands.argument("targets", EntityArgument.entities())
                                                        .then(
                                                                Commands.argument("dx", DoubleArgumentType.doubleArg())
                                                                        .then(
                                                                                Commands.argument("dy", DoubleArgumentType.doubleArg())
                                                                                        .then(
                                                                                                Commands.argument("dz", DoubleArgumentType.doubleArg())
                                                                                                        .executes(ctx -> {
                                                                                                            Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                                                                                            double dx = DoubleArgumentType.getDouble(ctx, "dx");
                                                                                                            double dy = DoubleArgumentType.getDouble(ctx, "dy");
                                                                                                            double dz = DoubleArgumentType.getDouble(ctx, "dz");
                                                                                                            for (Entity target : targets) {
                                                                                                                target.setDeltaMovement(dx, dy, dz);
                                                                                                                target.hasImpulse = true;
                                                                                                                if (target instanceof ServerPlayer player) {
                                                                                                                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                                                                                                                }
                                                                                                            }
                                                                                                            ctx.getSource().sendSuccess(() ->
                                                                                                                            Component.literal("设置 " + targets.size() + " 个实体的速度为 (" + dx + ", " + dy + ", " + dz + ")"),
                                                                                                                    true
                                                                                                            );
                                                                                                            return 1;
                                                                                                        })
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }
}
