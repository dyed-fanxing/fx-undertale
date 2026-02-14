package com.sakpeipei.undertale.utils;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class GravityUtils {
    public static BlockPos below(Entity entity, BlockPos pos) {
        GravityData gravityData = entity.getData(AttachmentTypes.GRAVITY.get());
        return pos.relative(gravityData.getGravity());
    }

    public static BlockPos above(Entity entity, BlockPos pos) {
        GravityData gravityData = entity.getData(AttachmentTypes.GRAVITY.get());
        return pos.relative(gravityData.getGravity().getOpposite());
    }

    public static Vec3 getEntitySurfaceBlockPos(Entity entity,BlockPos blockPos, Vec3i up) {
        return new Vec3(
                up.getX() == 0 ? entity.getX() : blockPos.getX(),
                up.getY() == 0 ? entity.getY() : blockPos.getY(),
                up.getZ() == 0 ? entity.getZ() : blockPos.getZ()
        );
    }

    public static BlockPos relative(Entity entity, BlockPos pos, Direction worldDirection) {
        GravityData gravityData = entity.getData(AttachmentTypes.GRAVITY.get());
        if (gravityData.getGravity() == Direction.DOWN) {
            return pos.relative(worldDirection);
        }
        // 将世界方向转换为重力感知的方向
        Vec3 worldDir = Vec3.atLowerCornerOf(worldDirection.getNormal());
        Vec3 localDir = gravityData.worldToLocal(worldDir);
        Direction localDirection = Direction.getNearest(localDir.x, localDir.y, localDir.z);

        return pos.relative(localDirection);
    }
}
