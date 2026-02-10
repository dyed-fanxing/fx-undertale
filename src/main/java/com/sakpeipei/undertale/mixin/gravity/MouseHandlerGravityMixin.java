package com.sakpeipei.undertale.mixin.gravity;


import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerGravityMixin {
    @Shadow @Final private Minecraft minecraft;
    /**
     * 根据重力方向修正鼠标输入的yawDelta和pitchDelta
     */
    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void turnPlayer(Args args) {
        if (this.minecraft.player != null) {
            Direction gravity = this.minecraft.player.getData(AttachmentTypeRegistry.GRAVITY).getGravity();
            double yawDelta = args.get(0),pitchDelta = args.get(1);
            switch (gravity) {
                case UP -> {
                    args.set(0,-yawDelta);
                    args.set(1,-pitchDelta);
                }
            }
        }
    }
}
