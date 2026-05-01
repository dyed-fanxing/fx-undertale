package com.fanxing.fx_undertale.data;

import com.fanxing.fx_undertale.data.damagesource.DamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author FanXing
 * @since 2025-09-08 21:00
 */
public class DamageTypeTagsProvider extends net.minecraft.data.tags.DamageTypeTagsProvider {
    public DamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, provider, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(DamageTypeTags.BYPASSES_COOLDOWN).add(DamageTypes.FRAME,DamageTypes.GASTER_BLASTER_BEAM);
        this.tag(DamageTypeTags.BYPASSES_ARMOR).add(DamageTypes.FRAME,DamageTypes.GASTER_BLASTER_BEAM);
        this.tag(DamageTypeTags.NO_KNOCKBACK).add(DamageTypes.FRAME,DamageTypes.GASTER_BLASTER_BEAM);
    }
}
