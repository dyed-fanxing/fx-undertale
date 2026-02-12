package com.sakpeipei.undertale.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.entity.boss.Sans;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * @author Sakqiongzi
 * @since 2026-01-08 22:13
 */
@EventBusSubscriber(modid = Undertale.MOD_ID)
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
                                            sans.setPersistentAngerTarget(player.getUUID());
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
    }
}
