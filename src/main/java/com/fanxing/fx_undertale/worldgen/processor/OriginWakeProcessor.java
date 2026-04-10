package com.fanxing.fx_undertale.worldgen.processor;

import com.fanxing.fx_undertale.registry.StructureProcessorTypes;
import com.fanxing.fx_undertale.utils.NbtUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 将结构中的实体AI激活
 */
public class OriginWakeProcessor extends StructureProcessor {
    private static final Logger log = LoggerFactory.getLogger(OriginWakeProcessor.class);
    public OriginWakeProcessor() {}
    public static final MapCodec<OriginWakeProcessor> CODEC = MapCodec.unit(OriginWakeProcessor::new);

    @Override
    protected @NotNull StructureProcessorType<?> getType() { return StructureProcessorTypes.ORIGIN_WAKE_SPAWNER.get();}

    @Override
    public StructureTemplate.@NotNull StructureEntityInfo processEntity(@NotNull LevelReader world, @NotNull BlockPos seedPos,
                                                                        StructureTemplate.@NotNull StructureEntityInfo rawEntityInfo,
                                                                        StructureTemplate.StructureEntityInfo entityInfo,
                                                                        @NotNull StructurePlaceSettings settings,
                                                                        @NotNull StructureTemplate template) {
        CompoundTag nbt = entityInfo.nbt;
        nbt.putByte("NoAI", (byte) 0);
        nbt.put("originPos",NbtUtils.newDoubleList(entityInfo.pos));
        nbt.putFloat("structYaw",switch (settings.getRotation()) {
            case CLOCKWISE_90 -> 90.0F;
            case CLOCKWISE_180 -> 180.0F;
            case COUNTERCLOCKWISE_90 -> -90.0F; // 或 270.0F
            default -> 0.0F;
        });
        return entityInfo;
    }
}