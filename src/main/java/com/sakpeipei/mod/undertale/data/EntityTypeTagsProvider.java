package com.sakpeipei.mod.undertale.data;

import com.sakpeipei.mod.undertale.registry.EntityTypeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EntityTypeTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class EntityTypeTagsProvider extends net.minecraft.data.tags.EntityTypeTagsProvider {

    public EntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, provider, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
                .add(EntityTypeRegistry.GASTER_BLASTER_FIXED.get(),EntityTypeRegistry.GASTER_BLASTER_PRO.get());
    }
}
