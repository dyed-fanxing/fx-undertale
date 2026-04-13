package com.fanxing.fx_undertale.worldgen.processor;

import com.fanxing.fx_undertale.registry.StructureProcessorTypes;
import com.fanxing.fx_undertale.utils.NbtUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
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
        if (world instanceof ServerLevelAccessor accessor){
            CompoundTag nbt = entityInfo.nbt;
            ServerLevel level = accessor.getLevel();
            // 获取实体类型ID（从原始NBT中读取）
            ResourceLocation entityId = ResourceLocation.tryParse(nbt.getString("id"));
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            Entity entity = entityType.create(level);
            if (entity != null) {
                CompoundTag newNbt = NbtUtils.merge( nbt,entity.saveWithoutId(new CompoundTag()));
                newNbt.putByte("NoAI",(byte) 0);
                return new StructureTemplate.StructureEntityInfo(entityInfo.pos,entityInfo.blockPos,newNbt);
            }
        }
        return entityInfo;
    }
}