package com.fanxing.fx_undertale.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;
import java.util.List;

public class GravityUtils {
    public static double COLLIED_EPSILON = 1.0E-12;
    public static double FLOOR_EPSILON = 1.0E-6;


    private static final Logger log = LoggerFactory.getLogger(GravityUtils.class);


    public static Vec3 findGround(Level level, Vec3 pos, Direction gravity) {
        Vec3i normal = gravity.getNormal();
        Vec3 g = new Vec3(normal.getX(), normal.getY(), normal.getZ());
        BlockPos searchPos = getContaining(pos,gravity);
        int step = gravity.getAxisDirection().getStep();
        int height = searchPos.get(gravity.getAxis())+step*256;
        double collisionHeight = 0.0;
        // 向重力方向搜索直到找到固体地面
        do {
            BlockPos posBelow = searchPos.relative(gravity);
            BlockState blockBelow = level.getBlockState(posBelow);
            // 检查下方方块的上表面是否坚固（可以站立）
            if (blockBelow.isFaceSturdy(level, posBelow, gravity.getOpposite())) {
                // 如果当前位置有方块，计算其碰撞箱高度
                if (!level.isEmptyBlock(searchPos)) {
                    BlockState blockAtPos = level.getBlockState(searchPos);
                    VoxelShape collisionShape = blockAtPos.getCollisionShape(level, searchPos);
                    if (!collisionShape.isEmpty()) {
                        collisionHeight = collisionShape.max(gravity.getAxis());
                    }
                }
                return combineTo(pos,getSurfaceBlockHeight(searchPos,gravity).add(g.scale(collisionHeight)));
            }
            // 继续向下搜索
            searchPos = searchPos.relative(gravity);
        } while (-step*searchPos.get(gravity.getAxis()) >= -step*height);
        return new Vec3(normal.getX(),normal.getY(),normal.getZ()).add(g.scale(collisionHeight));
    }


    public static Vec3 getEntitySurfaceBlockPos(Entity entity, BlockPos blockPos, Vec3i up) {
        return new Vec3(
                up.getX() == 0 ? entity.getX() : blockPos.getX(),
                up.getY() == 0 ? entity.getY() : blockPos.getY(),
                up.getZ() == 0 ? entity.getZ() : blockPos.getZ()
        );
    }

    public static Vec3 getSurfaceBlockPos(BlockPos blockPos, Direction gravity) {
        Vec3i down = gravity.getNormal();
        return new Vec3(
                blockPos.getX()+(down.getX()>0?1:0),
                blockPos.getY()+(down.getY()>0?1:0),
                blockPos.getZ()+(down.getZ()>0?1:0)
        );
    }
    public static Vec3 getSurfaceBlockHeight(BlockPos blockPos, Direction gravity) {
        Vec3i down = gravity.getNormal();
        return new Vec3(
                down.getX()==0?0:(blockPos.getX()+(down.getX()>0?1:0)),
                down.getY()==0?0:(blockPos.getY()+(down.getY()>0?1:0)),
                down.getZ()==0?0:(blockPos.getZ()+(down.getZ()>0?1:0))
        );
    }

    /**
     * 获取指定重力方向上的方块位置
     * @param offset 偏移
     */
    public static BlockPos getContaining(Vec3 offset,Direction gravity){
        Vec3i normal = gravity.getNormal();
        return BlockPos.containing(offset.x + (normal.getX()<0?-FLOOR_EPSILON:0), offset.y+(normal.getY()>0?-FLOOR_EPSILON:0), offset.z+(normal.getZ()>0?-FLOOR_EPSILON:0));
    }
    public static BlockPos getContaining(double x, double y,double z,Direction gravity){
        Vec3i normal = gravity.getNormal();
        return BlockPos.containing(x+(normal.getX()>0?-FLOOR_EPSILON:0), y+(normal.getY()>0?-FLOOR_EPSILON:0), z+(normal.getZ()>0?-FLOOR_EPSILON:0));
    }

    // 提取向量在重力轴上的分量（其他轴清零）
    public static Vec3 getAxialComponent(Vec3 vec, Direction gravity) {
        return switch (gravity.getAxis()) {
            case X -> new Vec3(vec.x, 0, 0);
            case Y -> new Vec3(0, vec.y, 0);
            case Z -> new Vec3(0, 0, vec.z);
        };
    }
    /**
     * 合并，取重力分量
     * @param vec3 向量
     * @param gravity 重力方向上的分量
     */
    public static Vec3 combineTo(Vec3 vec3, Vec3 gravity) {
        return new Vec3(
                gravity.x == 0 ? vec3.x : gravity.x,
                gravity.y == 0 ? vec3.y : gravity.y,
                gravity.z == 0 ? vec3.z : gravity.z
        );
    }


    public static Vec3 roundTo(Vec3 vec, int decimals) {
        double factor = Math.pow(10, decimals);
        return new Vec3(
                Math.round(vec.x * factor) / factor,
                Math.round(vec.y * factor) / factor,
                Math.round(vec.z * factor) / factor
        );
    }


    public static Vec3 getVec3(Direction direction){
        Vec3i normal = direction.getNormal();
        return new Vec3(normal.getX(),normal.getY(),normal.getZ());
    }

}
