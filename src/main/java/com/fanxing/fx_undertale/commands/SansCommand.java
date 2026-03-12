package com.fanxing.fx_undertale.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.entity.boss.sans.Sans;
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
public class SansCommand {
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
                                                case 2 -> sans.shootArcSweepVolley();
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
                                                                Gravity.applyGravity(target, direction);
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
                                                                Gravity.applyGravity(target,Direction.DOWN);
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
                                                                Gravity.applyGravity(target,Direction.UP);
                                                            }
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }

}
