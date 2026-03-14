package com.fanxing.fx_undertale.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

@EventBusSubscriber(modid = FxUndertale.MOD_ID, value = Dist.CLIENT)
public class Shaders {
    private static ShaderInstance whiteEntityShader;
    private static ShaderInstance flyBasicShader;
    private static ShaderInstance topFadeShader;

    private static ShaderInstance entityTranslucentEmissiveAdjustableShader;

    public static ShaderInstance getWhiteEntityShader() {
        return whiteEntityShader;
    }
    public static ShaderInstance getFlyBasicShader() {
        return flyBasicShader;
    }
    public static ShaderInstance getTopFadeShader() {
        return topFadeShader;
    }
    public static ShaderInstance getEntityTranslucentEmissiveAdjustableShader() { return entityTranslucentEmissiveAdjustableShader;}

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "white_entity"),
                        DefaultVertexFormat.NEW_ENTITY
                ),
                shader -> whiteEntityShader = shader
        );
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "fly_basic"),
                        DefaultVertexFormat.NEW_ENTITY
                ),
                shader -> flyBasicShader = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "top_fade"),
                        DefaultVertexFormat.NEW_ENTITY
                ),
                shader -> topFadeShader = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "entity_translucent_emissive_adjustable"),
                        DefaultVertexFormat.NEW_ENTITY
                ),
                shader -> entityTranslucentEmissiveAdjustableShader = shader);
    }

}