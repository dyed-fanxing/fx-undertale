package com.sakpeipei.undertale.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sakpeipei.undertale.network.TimeJumpTeleportPacket;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Sakpeipei
 * @since 2025/12/16 15:19
 */
@Mixin(Gui.class)
public class TimeJumpTeleportMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderCameraOverlays", at=@At("TAIL"))
    private void renderCameraOverlays(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Entity entity = this.minecraft.cameraEntity;
        if (entity != null) {
            CompoundTag persistentData = entity.getPersistentData();
            if (persistentData.contains(TimeJumpTeleportPacket.END_TICK)) {
                int endTick = persistentData.getInt(TimeJumpTeleportPacket.END_TICK);
                if(entity.tickCount < endTick){
                    // 3. 直接渲染纯黑屏
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    //纯黑色，不透明
                    guiGraphics.fill(RenderType.guiOverlay(),0, 0,guiGraphics.guiWidth(), guiGraphics.guiHeight(),-200, 0xFF000000 );
                    RenderSystem.disableBlend();
                    RenderSystem.depthMask(true);
                    RenderSystem.enableDepthTest();
                }
            }
        }
    }
}
