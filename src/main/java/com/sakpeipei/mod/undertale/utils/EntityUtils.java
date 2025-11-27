package com.sakpeipei.mod.undertale.utils;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * @author Sakqiongzi
 * @since 2025-11-18 23:01
 */
public class EntityUtils {
    /**
     * 精确查找地面Y坐标（与原版Evoker尖牙相同的逻辑）
     * @param level 世界
     * @param targetX 目标X坐标
     * @param targetZ 目标Z坐标
     * @param minY 最小搜索Y坐标
     * @param maxY 最大搜索Y坐标
     * @return 精确的地面Y坐标，如果没找到返回世界最小坐标
     */
    public static double findGroundY(Level level, double targetX, double targetZ, double minY, double maxY) {
        BlockPos searchPos = BlockPos.containing(targetX, maxY, targetZ);
        double collisionHeight = 0.0;

        // 向下搜索直到找到固体地面
        do {
            BlockPos posBelow = searchPos.below();
            BlockState blockBelow = level.getBlockState(posBelow);
            // 检查下方方块的上表面是否坚固（可以站立）
            if (blockBelow.isFaceSturdy(level, posBelow, Direction.UP)) {
                // 如果当前位置有方块，计算其碰撞箱高度
                if (!level.isEmptyBlock(searchPos)) {
                    BlockState blockAtPos = level.getBlockState(searchPos);
                    VoxelShape collisionShape = blockAtPos.getCollisionShape(level, searchPos);
                    if (!collisionShape.isEmpty()) {
                        collisionHeight = collisionShape.max(Direction.Axis.Y);
                    }
                }
                // 找到有效地面，返回精确高度
                return searchPos.getY() + collisionHeight;
            }
            // 继续向下搜索
            searchPos = searchPos.below();
        } while (searchPos.getY() >= Mth.floor(minY) - 1);


        // 没找到地面，返回世界最小Y坐标
        return level.getMinBuildHeight();
    }

}