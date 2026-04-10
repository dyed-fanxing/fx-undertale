package com.fanxing.fx_undertale.worldgen.processor;

import com.fanxing.fx_undertale.registry.StructureProcessorTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

/**
 * 将结构中的实体AI激活
 */
public class WakeProcessor extends StructureProcessor {
    public WakeProcessor() {}
    public static final MapCodec<WakeProcessor> CODEC = MapCodec.unit(WakeProcessor::new);

    @Override
    protected @NotNull StructureProcessorType<?> getType() { return StructureProcessorTypes.WAKE_SPAWNER.get();}



    @Override
    public StructureTemplate.@NotNull StructureEntityInfo processEntity(@NotNull LevelReader world, @NotNull BlockPos seedPos,
                                                                        StructureTemplate.@NotNull StructureEntityInfo rawEntityInfo,
                                                                        StructureTemplate.StructureEntityInfo entityInfo,
                                                                        @NotNull StructurePlaceSettings settings,
                                                                        @NotNull StructureTemplate template) {
        CompoundTag nbt = entityInfo.nbt;
        nbt.putByte("NoAI", (byte) 0);
        return new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, nbt);
    }
}