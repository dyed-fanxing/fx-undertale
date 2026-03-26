package com.fanxing.fx_undertale.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.fanxing.fx_undertale.entity.capability.Mountable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    /**
     * 双击shift脱离可乘坐实体
     */
    @ModifyVariable(method = "handleSetEntityPassengersPacket", at =@At(value = "STORE", ordinal = 0))
    private Component modifyMountOnboardMessage(Component value, @Local(ordinal = 0) Entity vehicle) {
        if (vehicle instanceof Mountable mountable && mountable.shouldDismountOnDoubleKey()) {
            Minecraft minecraft = Minecraft.getInstance();
            value = Component.translatable("mount.onboard.double.key.dismount",minecraft.options.keyShift.getTranslatedKeyMessage());
        }
        return value;
    }
}