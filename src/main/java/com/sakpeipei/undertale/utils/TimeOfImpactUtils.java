package com.sakpeipei.undertale.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class TimeOfImpactUtils {

    // ============== 精确检测（返回详细碰撞信息） ==============

    /**
     * 精确检测：返回详细的BlockHitResult（包含位置、法线等）
     * 如果没有碰撞，返回miss结果（模仿原版clip方法）
     */
    @NotNull
    public static BlockHitResult getBlockHitResult(Entity entity, ClipContext.Block clipBlock) {
        return getBlockHitResult(entity.level(), entity.getBoundingBox(), entity.getDeltaMovement(), clipBlock,ClipContext.Fluid.NONE, CollisionContext.of(entity));
    }

    /**
     * 精确检测：返回详细的BlockHitResult
     */
    @NotNull
    public static BlockHitResult getBlockHitResult(Level level, AABB box, Vec3 velocity,ClipContext.Block clipBlock,ClipContext.Fluid clipFluid,CollisionContext context) {

        if (velocity.lengthSqr() == 0) return BlockHitResult.miss(null,null,null);

        // 计算搜索范围
        AABB searchBox = box.expandTowards(velocity);

        BlockHitResult earliestHit = BlockHitResult.miss(null,null,null);
        double earliestTime = Double.MAX_VALUE;

        // 遍历搜索范围内的所有方块
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(searchBox.minX, searchBox.minY, searchBox.minZ),
                BlockPos.containing(searchBox.maxX, searchBox.maxY, searchBox.maxZ)
        )) {
            BlockState blockState = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);
            // 获取方块和流体的碰撞形状

            VoxelShape blockShape = clipBlock.get(blockState, level, pos, context);
            VoxelShape fluidShape = clipFluid.canPick(fluidState) ? fluidState.getShape(level, pos) : Shapes.empty();
            // 检测方块碰撞
            BlockHitResult blockHit = testShapeForHit(box, blockShape, pos, velocity, level, blockState);
            // 检测流体碰撞
            BlockHitResult fluidHit = testShapeForHit(box, fluidShape, pos, velocity, level, blockState);
            // 选择更早的碰撞
            BlockHitResult hit = selectCloserHit(blockHit, fluidHit, box.getCenter());
            if (hit != null) {
                double collisionTime = getCollisionTime(box, velocity, hit.getLocation());
                if (collisionTime < earliestTime) {
                    earliestTime = collisionTime;
                    earliestHit = hit;
                }
            }
        }

        return earliestHit;
    }


    // ============== 快速检测（只返回是否碰撞） ==============
    public static boolean isBlockCollide(Entity entity, ClipContext.Block clipBlock) {
        return isBlockCollide(entity.level(), entity.getBoundingBox(), entity.getDeltaMovement(),clipBlock,ClipContext.Fluid.NONE,CollisionContext.of(entity));
    }
    /**
     * 快速检测：只检查是否会碰撞，不计算详细位置信息
     * 性能更好，适合只需要知道是否碰撞的场景
     */
    public static boolean isBlockCollide(Entity entity, ClipContext.Block clipBlock,ClipContext.Fluid clipFluid) {
        return isBlockCollide(entity.level(), entity.getBoundingBox(), entity.getDeltaMovement(),clipBlock,clipFluid,CollisionContext.of(entity));
    }

    /**
     * 快速检测：只检查是否会碰撞
     */
    public static boolean isBlockCollide(Level level, AABB box, Vec3 velocity, ClipContext.Block clipBlock,ClipContext.Fluid clipFluid,CollisionContext context) {
        if (velocity.lengthSqr() == 0) return false;

        AABB searchBox = box.expandTowards(velocity);
        // 遍历搜索范围内的所有方块
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(searchBox.minX, searchBox.minY, searchBox.minZ),
                BlockPos.containing(searchBox.maxX, searchBox.maxY, searchBox.maxZ)
        )) {
            BlockState blockState = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);

            // 只检查碰撞形状，不计算详细位置
            if (testShapeCollisionQuick(box, clipBlock.get(blockState, level, pos, context), pos, velocity) ||
                    testShapeCollisionQuick(box, clipFluid.canPick(fluidState) ? fluidState.getShape(level, pos) : Shapes.empty(), pos, velocity)) {
                return true; // 只要有一个碰撞就返回true
            }
        }

        return false;
    }

    /**
     * 快速形状碰撞检测（不计算位置，只判断是否碰撞）
     */
    private static boolean testShapeCollisionQuick(AABB movingBox, VoxelShape shape,
                                                   BlockPos pos, Vec3 velocity) { // 添加参数
        if (shape.isEmpty()) return false;

        for (AABB part : shape.toAabbs()) {
            AABB blockBox = part.move(pos);
            double collisionTime = calculateCollisionTime(movingBox, blockBox, velocity);

            // 只要在移动过程中有碰撞（0-1之间），就返回true
            if (collisionTime >= 0 && collisionTime < 1.0) {
                return true;
            }
        }
        return false;
    }

    // ============== 通用辅助方法 ==============

    /**
     * 创建miss结果
     */
    private static BlockHitResult createMissResult(Vec3 center, Vec3 velocity) {
        Vec3 endPoint = center.add(velocity);
        Vec3 directionVec = center.subtract(endPoint);
        Direction nearestDirection = Direction.getNearest(directionVec.x, directionVec.y, directionVec.z);
        return BlockHitResult.miss(endPoint, nearestDirection, BlockPos.containing(endPoint));
    }

    /**
     * 检测AABB与特定形状的碰撞
     */
    @Nullable
    private static BlockHitResult testShapeForHit(AABB movingBox, VoxelShape shape, BlockPos pos,Vec3 velocity, Level level, BlockState state) {
        if (shape.isEmpty()) return null;

        BlockHitResult bestHit = null;
        double bestTime = Double.MAX_VALUE;

        for (AABB part : shape.toAabbs()) {
            AABB blockBox = part.move(pos);

            double collisionTime = calculateCollisionTime(movingBox, blockBox, velocity);
            if (collisionTime >= 0 && collisionTime < 1.0 && collisionTime < bestTime) {
                bestTime = collisionTime;
                Direction normal = calculateCollisionNormal(movingBox, blockBox, velocity, collisionTime);
                Vec3 hitLocation = movingBox.getCenter().add(velocity.scale(collisionTime));

                bestHit = new BlockHitResult(hitLocation, normal, pos, false);
            }
        }

        // 检查交互形状
        if (bestHit != null) {
            VoxelShape interactionShape = state.getInteractionShape(level, pos);
            if (!interactionShape.isEmpty()) {
                BlockHitResult interactionHit = testShapeForHit(movingBox, interactionShape, pos, velocity, level, state);
                if (interactionHit != null) {
                    double interactionTime = getCollisionTime(movingBox, velocity, interactionHit.getLocation());
                    if (interactionTime < bestTime) {
                        bestHit = interactionHit;
                    }
                }
            }
        }

        return bestHit;
    }

    /**
     * 选择更近的碰撞
     */
    @Nullable
    private static BlockHitResult selectCloserHit(BlockHitResult blockHit, BlockHitResult fluidHit, Vec3 start) {
        if (blockHit == null) return fluidHit;
        if (fluidHit == null) return blockHit;
        double blockDist = blockHit.getLocation().distanceToSqr(start);
        double fluidDist = fluidHit.getLocation().distanceToSqr(start);
        return blockDist <= fluidDist ? blockHit : fluidHit;
    }

    /**
     * 计算碰撞时间（从位置反推）
     */
    private static double getCollisionTime(AABB box, Vec3 velocity, Vec3 hitPos) {
        Vec3 center = box.getCenter();

        if (Math.abs(velocity.x) > 1e-7) {
            return (hitPos.x - center.x) / velocity.x;
        } else if (Math.abs(velocity.y) > 1e-7) {
            return (hitPos.y - center.y) / velocity.y;
        } else if (Math.abs(velocity.z) > 1e-7) {
            return (hitPos.z - center.z) / velocity.z;
        }
        return 0;
    }

    /**
     * 计算两个AABB碰撞的时间参数
     */
    private static double calculateCollisionTime(AABB movingBox, AABB staticBox, Vec3 velocity) {
        double tEnter = 0.0;
        double tExit = 1.0;
        final double CONTACT_EPSILON = 1e-6; // 接触容差

        for (Direction.Axis axis : Direction.Axis.values()) {
            double movingMin = movingBox.min(axis);
            double movingMax = movingBox.max(axis);
            double staticMin = staticBox.min(axis);
            double staticMax = staticBox.max(axis);
            double vel = velocity.get(axis);

            if (Math.abs(vel) < 1e-7) {
                // 该轴没有速度，检查是否已经碰撞
                // 使用容差：如果只是接触（差距小于CONTACT_EPSILON），不算碰撞
                if (movingMax <= staticMin + CONTACT_EPSILON || movingMin >= staticMax - CONTACT_EPSILON) {
                    return Double.MAX_VALUE; // 不会碰撞或只是接触
                }
                // 有真正的重叠（间隙大于容差），继续检查
                continue;
            }

            // 计算该轴的碰撞时间区间
            double axisEnter, axisExit;

            if (vel > 0) {
                // 正向移动：从左侧接近
                // 加上容差：当 movingMax + CONTACT_EPSILON >= staticMin 才算碰撞
                axisEnter = (staticMin - movingMax - CONTACT_EPSILON) / vel;
                axisExit = (staticMax - movingMin + CONTACT_EPSILON) / vel;
            } else {
                // 负向移动：从右侧接近
                axisEnter = (staticMax - movingMin + CONTACT_EPSILON) / vel;
                axisExit = (staticMin - movingMax - CONTACT_EPSILON) / vel;
            }

            // 更新全局时间区间
            tEnter = Math.max(tEnter, axisEnter);
            tExit = Math.min(tExit, axisExit);

            if (tEnter > tExit) {
                return Double.MAX_VALUE;
            }

            if (tEnter > 1.0) {
                return Double.MAX_VALUE;
            }
        }

        // 检查是否只是接触
        if (tEnter < 0) {
            boolean isJustContact = true;
            for (Direction.Axis axis : Direction.Axis.values()) {
                double movingMin = movingBox.min(axis);
                double movingMax = movingBox.max(axis);
                double staticMin = staticBox.min(axis);
                double staticMax = staticBox.max(axis);

                // 计算重叠量
                double overlap = Math.min(movingMax, staticMax) - Math.max(movingMin, staticMin);
                if (overlap > CONTACT_EPSILON * 2) { // 有真正的重叠
                    isJustContact = false;
                    break;
                }
            }

            if (isJustContact) {
                return Double.MAX_VALUE; // 只是接触，不算碰撞
            }
        }

        return tEnter < 0 ? 0 : tEnter;
    }

    /**
     * 计算碰撞法线
     */
    private static Direction calculateCollisionNormal(AABB movingBox, AABB staticBox, Vec3 velocity, double collisionTime) {
        AABB movingAtTime = movingBox.move(velocity.scale(collisionTime));

        double east = staticBox.minX - movingAtTime.maxX;
        double west = movingAtTime.minX - staticBox.maxX;
        double up = staticBox.minY - movingAtTime.maxY;
        double down = movingAtTime.minY - staticBox.maxY;
        double south = staticBox.minZ - movingAtTime.maxZ;
        double north = movingAtTime.minZ - staticBox.maxZ;

        double minPenetration = Math.abs(east);
        Direction result = east < 0 ? Direction.WEST : Direction.EAST;

        if (Math.abs(west) < minPenetration) {
            minPenetration = Math.abs(west);
            result = west < 0 ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(up) < minPenetration) {
            minPenetration = Math.abs(up);
            result = up < 0 ? Direction.DOWN : Direction.UP;
        }
        if (Math.abs(down) < minPenetration) {
            minPenetration = Math.abs(down);
            result = down < 0 ? Direction.UP : Direction.DOWN;
        }
        if (Math.abs(south) < minPenetration) {
            minPenetration = Math.abs(south);
            result = south < 0 ? Direction.NORTH : Direction.SOUTH;
        }
        if (Math.abs(north) < minPenetration) {
            result = north < 0 ? Direction.SOUTH : Direction.NORTH;
        }

        return result;
    }
}