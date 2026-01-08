package com.sakpeipei.undertale.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.common.mechanism.ColorAttack;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sakqiongzi
 * @since 2026-01-08 22:13
 */
@EventBusSubscriber(modid = Undertale.MODID)
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
                                        Sans sans = player.level().getNearestEntity(Sans.class, TargetingConditions.forNonCombat(), player, player.getX(), player.getY(), player.getZ(), new AABB(-viewDis,-viewDis,-viewDis,viewDis,viewDis,viewDis));
                                        if(sans != null) {
                                            sans.setPersistentAngerTarget(player.getUUID());
                                            switch (IntegerArgumentType.getInteger(cc, "function")) {
                                                case 1 -> sans.shootBoneRingVolley(player);
                                                case 2 -> sans.shootArcSweepVolley();
                                                case 3 -> sans.shootAimedBarrage(player);
                                                case 4 -> sans.shootForwardBarrage(player);
                                                case 5 -> sans.summonGroundBoneSpineAtSelf();
                                                case 6 -> sans.summonGroundBoneSpineWaveAroundSelf(player, 30f, ColorAttack.WHITE);
                                                case 7 -> sans.summonGroundBoneSpineWaveAroundSelf(player, ColorAttack.WHITE);
                                            }
                                        }
                                    }
                                    return 1;
                                })
                )
        );
    }
}
