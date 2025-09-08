package com.sakpeipei.mod.undertale.utils;

import com.sakpeipei.mod.undertale.Undertale;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class Tags {

    public static final TagKey<DamageType> KARMAR_BUFF = create(Registries.DAMAGE_TYPE, "karmar_buff");

    private static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> registry, String name){
        return TagKey.create(registry,ResourceLocation.fromNamespaceAndPath(Undertale.MODID, name));
    }
}
