package com.fanxing.fx_undertale.mixin;
import com.fanxing.fx_undertale.registry.ItemTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @ModifyArg(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"),
            index = 1   // 修改第二个参数（垂直旋转）
    )
    private double disableVerticalTurn(double originalYRot) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        // 你的条件：使用特定物品且按下冲刺键
        if (player != null && player.isUsingItem() && player.getUseItem().getItem() == ItemTypes.GASTER_BLASTER.get() && mc.options.keySprint.isDown()) {
            return 0.0;
        }
        return originalYRot;
    }
}