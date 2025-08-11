package com.sakpeipei.mod.undertale.tags.damagetype;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

import static com.sakpeipei.mod.undertale.Undertale.MODID;

public class DamageTypes {

    public static final ResourceKey<DamageType> GASTER_BLASTER_BEAM =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(MODID, "gaster_blaster_beam"));

}
