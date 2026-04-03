package com.fanxing.fx_undertale.mixin;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.boss.sans.SansDialogue;
import com.fanxing.fx_undertale.entity.dialogue.EntityDialogue;
import com.fanxing.fx_undertale.net.packet.MercyTriggerPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenClickCommandMixin {
    private static final Logger log = LoggerFactory.getLogger(ScreenClickCommandMixin.class);

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void onHandleComponentClicked(Style style, CallbackInfoReturnable<Boolean> cir) {
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
            String value = clickEvent.getValue();
            // 检查是否以我们的特殊前缀开头
            if(value.startsWith(EntityDialogue.CLICK_COMMAND_PACKET_PRE)){
                String action = value.substring(EntityDialogue.CLICK_COMMAND_PACKET_PRE.length());
                if(action.startsWith(SansDialogue.MERCY)){
                    log.debug("action: {}", action);
                    PacketDistributor.sendToServer(new MercyTriggerPacket(Integer.parseInt(action.substring(SansDialogue.MERCY.length()+1))));
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }
}
