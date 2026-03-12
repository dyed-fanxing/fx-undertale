package com.fanxing.fx_undertale.common.tags.entitytype;

import com.fanxing.fx_undertale.FxUndertale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * @author FanXing
 * @since 2025-10-13 23:31
 */
public interface EntityTypeTags {
    // 闪避弹射物，用于类似末影人免疫弹射物，但弹射物会继续飞行
    TagKey<EntityType<?>> DODGE_PROJECTILE = create("dodge_projectile");

    private static TagKey<EntityType<?>> create(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID,name));
    }
}
