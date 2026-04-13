package com.fanxing.fx_undertale.common;

import com.fanxing.fx_undertale.FxUndertale;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.fanxing.fx_undertale.client.Shaders;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.client.renderer.RenderStateShard.*;

/**
 * @author Sakpeipei
 * @since 2026/1/6 13:50
 */
public interface RenderTypes {
    /**
     * 半透明实体 + 可调节自发光强度 + 可控制排序
     * 对应原版 ENTITY_TRANSLUCENT_EMISSIVE，添加 strength 和 sortOnUpload 参数
     *
     * @param texture 纹理
     * @param sortOnUpload 是否启用排序（半透明需要 true，加法混合可用 false）
     * @param strength 自发光强度 (0.0 - 1.0)
     */
    // ========== 核心创建方法 ==========
    private static RenderType createEntityTranslucentEmissiveAdjustable(ResourceLocation texture, boolean sortOnUpload, float strength) {
        return RenderType.create(
                "entity_translucent_emissive_adjustable", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, sortOnUpload,
                RenderType.CompositeState.builder()
                        .setShaderState(new ShaderStateShard(() -> {
                            ShaderInstance shader = Shaders.getEntityTranslucentEmissiveAdjustableShader();
                            shader.safeGetUniform("uEmissiveStrength").set(strength);
                            return shader;
                        }))
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOverlayState(OVERLAY)
                        .setLightmapState(LIGHTMAP)
                        .createCompositeState(true)
        );
    }
    BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_DEPTH = Util.memoize((texture, isAffectsOutline) ->
            RenderType.create("entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
                    RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(isAffectsOutline))
    );

    BiFunction<ResourceLocation, Float, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_ADJUSTABLE_SORT =
            Util.memoize((texture, strength) -> createEntityTranslucentEmissiveAdjustable(texture, true, strength));
    BiFunction<ResourceLocation, Float, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_ADJUSTABLE_UNSORT =
            Util.memoize((texture, strength) -> createEntityTranslucentEmissiveAdjustable(texture, false, strength));

    /**
     * 与原版beam的区别为 NO_CULL，因为这个GB炮是跟着实体渲染的，而原版的信标光束是跟着方块渲染的，顺序不一样
     * 如果使用原版的beam，会导致光束不会覆盖穿过的实体，而是会在光束中看到实体，且光束的两端会被GB炮覆盖导致透明
     */
    BiFunction<ResourceLocation, Boolean, RenderType> BEAM_NO_CULL = Util.memoize((texture, translucent) -> RenderType.create(
            "beam_no_cull", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, false, translucent,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setWriteMaskState(translucent ? COLOR_WRITE : COLOR_DEPTH_WRITE) // 透明只写颜色，不透明写颜色+深度
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    ));
    BiFunction<ResourceLocation, Boolean, RenderType> BEAM_NO_CULL_TRIANGLE_STRIP = Util.memoize((texture, translucent) -> RenderType.create(
            "beam_no_cull_triangle_strip", DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLE_STRIP, 1536, false, translucent,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setWriteMaskState(translucent ? COLOR_WRITE : COLOR_DEPTH_WRITE) // 透明只写颜色，不透明写颜色+深度
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    ));
    RenderType ENTITY_TRANSLUCENT_EMISSIVE_WHITE = ENTITY_TRANSLUCENT_EMISSIVE_DEPTH.apply(ResourceLocations.WHITE_TEXTURE, true);
    /**
     * ENERGY_BEAM_FLOW - 高亮能量流动效果（带偏移）
     * 使用信标着色器 + 加法混合 + 无光照，颜色鲜艳且不受光影影响。
     */
    Function<ResourceLocation, RenderType> ENERGY_BEAM = Util.memoize((texture) -> RenderType.create("energy_beam",DefaultVertexFormat.BLOCK,VertexFormat.Mode.QUADS,1536,false,true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)          // 不写深度
                        .createCompositeState(false)
    ));
    /**
     * ENERGY_BEAM_FLOW_TRIANGLE_STRIP - 高亮能量流动效果（带偏移，条带模式）
     * 使用信标着色器 + 加法混合 + 无光照，颜色鲜艳且不受光影影响。
     */
    Function<ResourceLocation, RenderType> ENERGY_BEAM_TRIANGLE_STRIP = Util.memoize((texture) -> RenderType.create("energy_beam_triangle_strip",DefaultVertexFormat.BLOCK,VertexFormat.Mode.TRIANGLE_STRIP,1536,false,true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)          // 不写深度
                        .createCompositeState(false)
    ));


    // ========== ENTITY_TRANSLUCENT_EMISSIVE 渲染类型 ==========
    /**
     * 适合渲染：圆，任意三角形网格等
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLES = Util.memoize((texture, sortOnUpload) -> RenderType.create(
            "entity_translucent_emissive_triangles", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, sortOnUpload,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ));
    RenderType ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_WHITE = ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLES.apply(ResourceLocations.WHITE_TEXTURE, true);
    /**
     * 适合渲染：圆柱侧面、胶囊体圆柱部分侧面等条带结构
     * 顶点数最少：segments * 2 个顶点
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP = Util.memoize((texture, sortOnUpload) -> RenderType.create(
            "entity_translucent_emissive_triangle_strip", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_STRIP, 1536, false, sortOnUpload,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ));
    RenderType ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP_WHITE = ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_STRIP.apply(ResourceLocations.WHITE_TEXTURE, true);
    /**
     * 适合渲染：圆，扇形
     * 但是注意！！！！不能共用同一个buffer缓冲池，每个渲染类型会共有缓冲池，所有提交的顶点都会在这个缓冲池里，最后批量一起提交给GPU，GPU会将所有顶点按照FAN模式渲染，会导致第二个提交的独立顶点和第一个产生关系
     * 顶点数最少：segments + 2 个顶点
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_FAN = Util.memoize((texture, sortOnUpload) -> RenderType.create(
            "entity_translucent_emissive_triangle_fan", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_FAN, 1536, false, sortOnUpload,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    ));
    RenderType ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_FAN_WHITE = ENTITY_TRANSLUCENT_EMISSIVE_TRIANGLE_FAN.apply(ResourceLocations.WHITE_TEXTURE, true);
    // ========== ENERGY 能量渲染效果 ==========

    /**
     * ENERGY_TRIANGLE_FAN
     * 能量效果 - 扇形模式
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENERGY_TRIANGLE_FAN = Util.memoize((texture, sortOnUpload) -> RenderType.create(
            "energy_triangle_fan", DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLE_FAN, 1536, false, sortOnUpload,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    ));

    /**
     * ENERGY_TRIANGLES
     * 能量效果 - 三角形模式
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENERGY_TRIANGLES = Util.memoize((texture, sortOnUpload) -> RenderType.create(
            "energy_triangles", DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLES, 1536, false, sortOnUpload,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    ));

    /**
     * ENERGY_TRIANGLE_STRIP
     * 能量效果 - 条带模式
     */
    BiFunction<ResourceLocation, Boolean, RenderType> ENERGY_TRIANGLE_STRIP = Util.memoize((texture, sortOnUpload) -> RenderType.create(
            "energy_triangle_strip", DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLE_STRIP, 1536, false, sortOnUpload,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)
    ));
    RenderType ENERGY_TRIANGLE_STRIP_WHITE = ENERGY_TRIANGLE_STRIP.apply(ResourceLocations.WHITE_TEXTURE, true);
    RenderType ENERGY_TRIANGLE_FAN_WHITE = ENERGY_TRIANGLE_FAN.apply(ResourceLocations.WHITE_TEXTURE, true);


    // ========== ENERGY_SWIRL 能量漩涡静态方法（带偏移） ==========
    static RenderType energySwirl(ResourceLocation resourceLocation, float uOffset, float vOffset) {
        return RenderType.create("energy_swirl",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new TextureStateShard(resourceLocation, false, false))
                        .setTexturingState(new OffsetTexturingStateShard(uOffset, vOffset))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
        );
    }
    /**
     * ENERGY_SWIRL_TRIANGLES - 能量漩涡效果（带偏移，三角形模式）
     */
    static RenderType energySwirlTriangles(ResourceLocation resourceLocation, float uOffset, float vOffset) {
        return RenderType.create("energy_swirl_triangles", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new TextureStateShard(resourceLocation, false, false))
                        .setTexturingState(new OffsetTexturingStateShard(uOffset, vOffset))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
        );
    }

    /**
     * ENERGY_SWIRL_TRIANGLE_FAN - 能量漩涡效果（带偏移，扇形模式）
     */
    static RenderType energySwirlTriangleFan(ResourceLocation resourceLocation, float uOffset, float vOffset) {
        return RenderType.create("energy_swirl_triangle_fan", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_FAN, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new TextureStateShard(resourceLocation, false, false))
                        .setTexturingState(new OffsetTexturingStateShard(uOffset, vOffset))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
        );
    }

    /**
     * ENERGY_SWIRL_TRIANGLE_STRIP - 能量漩涡效果（带偏移，条带模式）
     */
    static RenderType energySwirlTriangleStrip(ResourceLocation resourceLocation, float uOffset, float vOffset) {
        return RenderType.create("energy_swirl_triangle_strip", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_STRIP, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new TextureStateShard(resourceLocation, false, false))
                        .setTexturingState(new OffsetTexturingStateShard(uOffset, vOffset))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
        );
    }





    /**
     * 渲染可透明白色实体类型：将有色部分替换为白色，透明部分不动
     */
    BiFunction<ResourceLocation, Boolean, RenderType> WHITE_ENTITY_TRANSLUCENT = Util.memoize((texture, sortOnUpload) -> RenderType.create(
            "white_entity_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, sortOnUpload,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(Shaders::getWhiteEntityShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setCullState(NO_CULL)
                    .createCompositeState(true)
    ));
    /**
     * 模型顶底偏移消散
     */
    BiFunction<ResourceLocation, Boolean, RenderType> FLY_BASIC = Util.memoize((texture, translucent) -> RenderType.create(
            "fly_basic", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(Shaders::getFlyBasicShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setCullState(NO_CULL)
                    .createCompositeState(true)
    ));

    /**
     * 从顶部开始向下，淡出（消失）
     * 使用了自定义的getTopFadeShader着色器，详细参数看着色器
     */
    BiFunction<ResourceLocation, Boolean, RenderType> TOP_FADE = Util.memoize((texture, translucent) -> RenderType.create(
            "top_fade", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(Shaders::getTopFadeShader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setCullState(NO_CULL)
                    .createCompositeState(true)
    ));
}
