package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerGravityMinix {

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void onServerAiStepHead(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        GravityData data = self.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() != Direction.DOWN) {
            switch (data.getGravity()) {
                case UP -> self.zza = -self.input.forwardImpulse;
            }
        }
    }

}
