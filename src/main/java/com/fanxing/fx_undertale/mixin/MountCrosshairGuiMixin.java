package com.fanxing.fx_undertale.mixin;

import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public abstract class MountCrosshairGuiMixin {
    @Unique
    private static final ResourceLocation MOUNT_CROSSHAIR = ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "hud/mount_crosshair");

    @Shadow @Final private Minecraft minecraft;

    // 原版准星的 ResourceLocation（直接引用原字段）
    @Shadow @Final private static ResourceLocation CROSSHAIR_SPRITE;

    @ModifyArg(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 0 // 第一个 blitSprite 调用（即绘制准星的那一行）
            ),
            index = 0 // 修改第一个参数
    )
    private ResourceLocation replaceCrosshairSprite(ResourceLocation original) {
        Player player = minecraft.player;
        Entity target = minecraft.crosshairPickEntity;
        // 判断是否可骑乘（复用之前的 MountableHelper）
        if (undertale$isMountable(target, player)) {
            return MOUNT_CROSSHAIR;
        }
        return original; // 否则返回原版准星
    }

    /**
     * 判断实体是否可被玩家“坐上去”（用于显示自定义准星）
     */
    @Unique
    private static boolean undertale$isMountable(Entity target, Player player) {
        if (target == null || player == null) return false;
        return switch (target) {
            // 1. 船、矿车等载具：始终可坐
            case VehicleEntity ignored -> true;
            // 2. 马类（普通马、驴、骡）：排除骷髅马和僵尸马，且必须成年
            case AbstractHorse horse when !(horse instanceof SkeletonHorse) && !(horse instanceof ZombieHorse) -> !horse.isBaby();
            // 3. 猪、炽足兽：需要成年且装备鞍
            case Pig pig -> !pig.isBaby() && pig.isSaddled();
            case Strider strider -> !strider.isBaby() && strider.isSaddled();

//            case GasterBlaster gb -> gb.isRideable(); // 确保该数据已同步到客户端

            default -> false;
        };
    }

}
