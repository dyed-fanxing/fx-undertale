package com.sakpeipei.undertale.mixin.roll;

import com.sakpeipei.undertale.entity.IRollable;
import com.sakpeipei.undertale.net.IRollHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为玩家移动旋转数据添加roll，使其跟着原版的逻辑handleMovePlayer一起处理
 */
@Mixin(ServerboundMovePlayerPacket.class)
public abstract class ServerboundMovePlayerPacketRollMixin implements IRollHolder {
    @Shadow @Final protected boolean hasRot;
    @Unique
    protected float undertale$roll;
    @Override public float undertale$getRoll(float serverRoll) { return this.hasRot?undertale$roll:serverRoll; }
    @Override public void undertale$setRoll(float roll) { this.undertale$roll = roll; }

    @Mixin(ServerboundMovePlayerPacket.PosRot.class)
    public static class PosRot extends ServerboundMovePlayerPacketRollMixin {
        @Inject(method = "<init>", at = @At("RETURN"))
        private void init(double x, double y, double z, float yRot, float xRot, boolean onGround, CallbackInfo ci) {
            IRollable rollable = (IRollable) Minecraft.getInstance().player;
            if (rollable != null) {
                this.undertale$roll = rollable.undertale$getRoll();
            }
        }

        @Inject(method = "write", at = @At("RETURN"))
        private void write(@NotNull FriendlyByteBuf buf, CallbackInfo ci) {
            buf.writeFloat(this.undertale$roll);
        }
        @Inject(method = "read", at = @At("RETURN"), cancellable = true)
        private static void read(FriendlyByteBuf buf, CallbackInfoReturnable<ServerboundMovePlayerPacket.PosRot> cir) {
            ServerboundMovePlayerPacket.PosRot packet = cir.getReturnValue();
            ((IRollHolder)packet).undertale$setRoll(buf.readFloat());
            cir.setReturnValue(packet);
        }
    }

    @Mixin(ServerboundMovePlayerPacket.Rot.class)
    public static class Rot extends ServerboundMovePlayerPacketRollMixin {
        @Inject(method = "<init>", at = @At("RETURN"))
        private void init(float yRot, float xRot, boolean onGround, CallbackInfo ci) {
            IRollable rollable = (IRollable) Minecraft.getInstance().player;
            if (rollable != null) this.undertale$roll = rollable.undertale$getRoll();
        }
        @Inject(method = "write", at = @At("RETURN"))
        private void write(FriendlyByteBuf buf, CallbackInfo ci) {
            buf.writeFloat(this.undertale$roll);
        }
        @Inject(method = "read", at = @At("RETURN"), cancellable = true)
        private static void read(FriendlyByteBuf buf, CallbackInfoReturnable<ServerboundMovePlayerPacket.Rot> cir) {
            ServerboundMovePlayerPacket.Rot packet = cir.getReturnValue();
            ((IRollHolder)packet).undertale$setRoll(buf.readFloat());
            cir.setReturnValue(packet);
        }
    }
}
