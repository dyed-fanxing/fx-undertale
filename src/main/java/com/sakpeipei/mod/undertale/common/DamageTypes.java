package com.sakpeipei.mod.undertale.common;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

import static com.sakpeipei.mod.undertale.Undertale.MODID;

public interface DamageTypes {

    ResourceKey<DamageType> GASTER_BLASTER_BEAM = create( "gaster_blaster_beam");
    ResourceKey<DamageType> FRAME = create("frame");
    ResourceKey<DamageType> KARMA = create("karma");

    private static ResourceKey<DamageType> create(String name){
        return  ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(MODID, name));
    }
}
