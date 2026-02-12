package com.sakpeipei.undertale.mixin.roll;

import com.google.common.primitives.Floats;
import com.llamalad7.mixinextras.sugar.Local;
import com.sakpeipei.undertale.entity.IRollable;
import com.sakpeipei.undertale.net.IRollHolder;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 添加roll变量的相关判断和处理
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplRollMixin {
    @Shadow
    private static boolean containsInvalidValues(double p_143664_, double p_143665_, double p_143666_, float p_143667_, float p_143668_) {
        return false;
    }

    @Shadow public ServerPlayer player;

    @Redirect(method = "handleMovePlayer",at = @At(value = "INVOKE",target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;containsInvalidValues(DDDFF)Z"))
    private boolean redirectContainsInvalidValues(double x, double y, double z, float yRot, float xRot, @Local(ordinal = 0, argsOnly = true) ServerboundMovePlayerPacket packet) {

        return containsInvalidValues(x, y, z, yRot, xRot) || !Floats.isFinite(((IRollHolder)packet).undertale$getRoll(0F));
    }

    @Inject(method = "handleMovePlayer",at = @At(value = "INVOKE",target = "Lnet/minecraft/server/level/ServerPlayer;absMoveTo(DDDFF)V",shift = At.Shift.AFTER))
    private void redirectContainsInvalidValues(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        IRollable rollable = (IRollable) player;
        rollable.undertale$setRoll(Mth.wrapDegrees(((IRollHolder)packet).undertale$getRoll(rollable.undertale$getRoll())));
    }
//    @Inject(method = "handleMovePlayer",at = @At(value = "INVOKE",target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V",shift = At.Shift.AFTER))
//    private void redirectContainsInvalidValues(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
//        IRollable rollable = (IRollable) player;
//        rollable.undertale$setRoll(Mth.wrapDegrees(((IRollHolder)packet).undertale$getRoll(rollable.undertale$getRoll())));
//    }
}
