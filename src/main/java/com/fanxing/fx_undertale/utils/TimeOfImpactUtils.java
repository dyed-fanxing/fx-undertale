package com.fanxing.fx_undertale.utils;

import com.fanxing.fx_undertale.common.phys.OBB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class TimeOfImpactUtils {
    private static final Logger log = LoggerFactory.getLogger(TimeOfImpactUtils.class);

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
        return getBlockHitResult(level, box,searchBox, velocity, clipBlock, clipFluid, context);
    }
    /**
     * 精确检测：返回详细的BlockHitResult
     */
    @NotNull
    public static BlockHitResult getBlockHitResult(Level level, AABB box,AABB searchBox,Vec3 velocity,ClipContext.Block clipBlock,ClipContext.Fluid clipFluid,CollisionContext context) {
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
     * 快速检测：只检查是否会碰撞
     */
    public static boolean isBlockCollide(Level level, AABB box,AABB searchArea,Vec3 velocity, ClipContext.Block clipBlock,ClipContext.Fluid clipFluid,CollisionContext context) {
        if (velocity.lengthSqr() == 0) return false;
        // 遍历搜索范围内的所有方块
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(searchArea.minX, searchArea.minY, searchArea.minZ),
                BlockPos.containing(searchArea.maxX, searchArea.maxY, searchArea.maxZ)
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
    private static boolean testShapeCollisionQuick(AABB movingBox, VoxelShape shape, BlockPos pos, Vec3 velocity) {
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







    // ============== OBB快速碰撞检测（boolean） ==============

    /**
     * OBB的精确碰撞检测（返回BlockHitResult）
     * 如果需要精确结果时使用
     */
    @NotNull
    public static BlockHitResult getOBBBlockHitResult(OBB obb, Vec3 velocity, Level level,
                                                      ClipContext.Block clipBlock, ClipContext.Fluid clipFluid,
                                                      CollisionContext context) {
        if (velocity.lengthSqr() == 0) {
            return BlockHitResult.miss(null, null, null);
        }

        // 先用快速检测是否有碰撞
        if (!isCollide(level, obb, velocity, clipBlock, clipFluid, context)) {
            return BlockHitResult.miss(null, null, null);
        }

        // 精确检测
        return getOBBExactHitResult(obb, velocity, level, clipBlock, clipFluid, context);
    }

    /**
     * 快速检测OBB是否会碰撞
     */
    public static boolean isCollide(Level level, OBB obb, Vec3 velocity, ClipContext.Block clipBlock, ClipContext.Fluid clipFluid, CollisionContext context) {
        // 获取扩展OBB的搜索范围
        OBB extendedOBB = obb.expandTowards(velocity);
        AABB searchArea = extendedOBB.getBoundingAABB();
        // 快速检查是否有碰撞的方块
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(searchArea.minX, searchArea.minY, searchArea.minZ),
                BlockPos.containing(searchArea.maxX, searchArea.maxY, searchArea.maxZ)
        )) {
            BlockState blockState = level.getBlockState(pos);
            VoxelShape blockShape = clipBlock.get(blockState, level, pos, context);
            if (!blockShape.isEmpty()) {
                // 检查扩展OBB是否与方块AABB相交
                for (AABB part : blockShape.toAabbs()) {
                    AABB worldBox = part.move(pos);
                    if (extendedOBB.intersects(worldBox)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * OBB精确碰撞检测（内部方法）
     */
    public static BlockHitResult getOBBExactHitResult(OBB obb, Vec3 velocity, Level level,
                                                       ClipContext.Block clipBlock, ClipContext.Fluid clipFluid,
                                                       CollisionContext context) {
        // 获取搜索范围
        OBB extendedOBB = obb.expandTowards(velocity);
        AABB searchArea = extendedOBB.getBoundingAABB().inflate(0.5);

        BlockHitResult earliestHit = BlockHitResult.miss(null, null, null);
        double earliestTime = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(searchArea.minX, searchArea.minY, searchArea.minZ),
                BlockPos.containing(searchArea.maxX, searchArea.maxY, searchArea.maxZ)
        )) {
            BlockState blockState = level.getBlockState(pos);
            VoxelShape blockShape = clipBlock.get(blockState, level, pos, context);

            if (!blockShape.isEmpty()) {
                // 检测碰撞并计算碰撞时间
                for (AABB part : blockShape.toAabbs()) {
                    AABB worldBox = part.move(pos);

                    // 计算OBB与AABB的碰撞时间
                    double time = calculateOBBCollisionTime(obb, worldBox, velocity);

                    if (time >= 0 && time < 1.0 && time < earliestTime) {
                        earliestTime = time;

                        // 计算碰撞点和法线
                        Vec3 hitPoint = obb.getCenter().add(velocity.scale(time));
                        Direction normal = calculateOBBNormal(obb, worldBox, velocity, time);

                        earliestHit = new BlockHitResult(hitPoint, normal, pos, false);
                    }
                }
            }
        }

        return earliestHit;
    }

    /**
     * 计算OBB与AABB的碰撞时间
     */
    public static double calculateOBBCollisionTime(OBB obb, AABB aabb, Vec3 velocity) {
        // 使用分离轴定理计算碰撞时间
        // 简化：采样OBB的关键点进行检测

        double earliestTime = Double.MAX_VALUE;
        boolean foundCollision = false;

        // 采样OBB的8个顶点和中心点
        Vec3[] samplePoints = getSamplePoints(obb);

        for (Vec3 point : samplePoints) {
            // 检测这个点沿速度方向是否会碰撞AABB
            double time = calculatePointCollisionTime(point, aabb, velocity);

            if (time >= 0 && time < 1.0 && time < earliestTime) {
                earliestTime = time;
                foundCollision = true;
            }
        }

        return foundCollision ? earliestTime : -1.0;
    }

    /**
     * 获取OBB的采样点（用于简化碰撞检测）
     */
    public static Vec3[] getSamplePoints(OBB obb) {
        Vec3[] vertices = obb.getVertices();
        Vec3 center = obb.getCenter();

        // 返回所有顶点和中心点
        Vec3[] points = new Vec3[vertices.length + 1];
        System.arraycopy(vertices, 0, points, 0, vertices.length);
        points[vertices.length] = center;

        return points;
    }

    /**
     * 计算点与AABB的碰撞时间
     */
    public static double calculatePointCollisionTime(Vec3 point, AABB aabb, Vec3 velocity) {
        // 将点视为一个微小AABB
        double epsilon = 0.001;
        AABB pointBox = new AABB(
                point.x - epsilon, point.y - epsilon, point.z - epsilon,
                point.x + epsilon, point.y + epsilon, point.z + epsilon
        );

        // 使用AABB碰撞检测
        return calculateAABBCollisionTime(pointBox, aabb, velocity);
    }

    /**
     * 计算AABB与AABB的碰撞时间（连续碰撞检测）
     */
    public static double calculateAABBCollisionTime(AABB movingBox, AABB fixedBox, Vec3 velocity) {
        double tEnter = 0.0;
        double tExit = 1.0;

        // 检查X轴
        if (velocity.x != 0) {
            double t1 = (fixedBox.minX - movingBox.maxX) / velocity.x;
            double t2 = (fixedBox.maxX - movingBox.minX) / velocity.x;
            double tMin = Math.min(t1, t2);
            double tMax = Math.max(t1, t2);

            tEnter = Math.max(tEnter, tMin);
            tExit = Math.min(tExit, tMax);
        }

        // 检查Y轴
        if (velocity.y != 0) {
            double t1 = (fixedBox.minY - movingBox.maxY) / velocity.y;
            double t2 = (fixedBox.maxY - movingBox.minY) / velocity.y;
            double tMin = Math.min(t1, t2);
            double tMax = Math.max(t1, t2);

            tEnter = Math.max(tEnter, tMin);
            tExit = Math.min(tExit, tMax);
        }

        // 检查Z轴
        if (velocity.z != 0) {
            double t1 = (fixedBox.minZ - movingBox.maxZ) / velocity.z;
            double t2 = (fixedBox.maxZ - movingBox.minZ) / velocity.z;
            double tMin = Math.min(t1, t2);
            double tMax = Math.max(t1, t2);

            tEnter = Math.max(tEnter, tMin);
            tExit = Math.min(tExit, tMax);
        }

        if (tEnter <= tExit && tEnter >= 0 && tEnter <= 1.0) {
            return tEnter;
        }

        return -1.0;
    }

    /**
     * 计算OBB碰撞法线
     */
    public static Direction calculateOBBNormal(OBB obb, AABB aabb, Vec3 velocity, double collisionTime) {
        // 计算OBB在碰撞时刻的位置
        Vec3 moveAtTime = velocity.scale(collisionTime);
        OBB obbAtTime = obb.move(moveAtTime);

        // 获取OBB的AABB包围盒
        AABB obbAABB = obbAtTime.getBoundingAABB();

        // 计算从OBB中心到AABB中心的向量
        Vec3 obbCenter = obbAtTime.getCenter();
        Vec3 aabbCenter = new Vec3(
                (aabb.minX + aabb.maxX) * 0.5,
                (aabb.minY + aabb.maxY) * 0.5,
                (aabb.minZ + aabb.maxZ) * 0.5
        );
        Vec3 centerToCenter = aabbCenter.subtract(obbCenter);

        // 找到重叠最少的方向作为法线
        double minOverlap = Double.MAX_VALUE;
        Direction closestNormal = Direction.UP;

        // 检查每个轴方向的穿透深度
        Direction[] axes = Direction.values();
        for (Direction axis : axes) {
            Vec3 axisVec = new Vec3(axis.getStepX(), axis.getStepY(), axis.getStepZ());

            // 计算在这个轴上的投影重叠
            double overlap = calculateOverlapOnAxis(obbAABB, aabb, axisVec);

            if (overlap >= 0 && overlap < minOverlap) {
                minOverlap = overlap;
                closestNormal = axis;
            }
        }

        // 确保法线指向正确方向
        Vec3i normalVecI = closestNormal.getNormal();
        double dot = centerToCenter.x * normalVecI.getX() +
                centerToCenter.y * normalVecI.getY() +
                centerToCenter.z * normalVecI.getZ();

        if (dot > 0) {
            closestNormal = closestNormal.getOpposite();
        }

        return closestNormal;
    }

    /**
     * 计算两个AABB在指定轴上的重叠
     */
    public static double calculateOverlapOnAxis(AABB box1, AABB box2, Vec3 axis) {
        // 计算box1在轴上的投影范围
        double[] range1 = getProjectionRange(box1, axis);
        double[] range2 = getProjectionRange(box2, axis);
        // 计算重叠
        return Math.min(range1[1], range2[1]) - Math.max(range1[0], range2[0]);
    }

    /**
     * 获取AABB在指定轴上的投影范围
     */
    public static double[] getProjectionRange(AABB box, Vec3 axis) {
        // 获取AABB的8个顶点
        double[] verticesX = {box.minX, box.maxX, box.minX, box.maxX, box.minX, box.maxX, box.minX, box.maxX};
        double[] verticesY = {box.minY, box.minY, box.maxY, box.maxY, box.minY, box.minY, box.maxY, box.maxY};
        double[] verticesZ = {box.minZ, box.minZ, box.minZ, box.minZ, box.maxZ, box.maxZ, box.maxZ, box.maxZ};

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            double proj = verticesX[i] * axis.x + verticesY[i] * axis.y + verticesZ[i] * axis.z;
            min = Math.min(min, proj);
            max = Math.max(max, proj);
        }

        return new double[]{min, max};
    }

    /**
     * 计算OBB碰撞时间（从碰撞点反推）
     */
    public static double getOBBCollisionTime(OBB obb, Vec3 velocity, Vec3 hitPoint) {
        // 使用中心点到碰撞点的向量
        Vec3 toHit = hitPoint.subtract(obb.getCenter());

        // 找到速度最大的轴
        double velX = Math.abs(velocity.x);
        double velY = Math.abs(velocity.y);
        double velZ = Math.abs(velocity.z);

        if (velX > velY && velX > velZ) {
            return toHit.x / velocity.x;
        } else if (velY > velZ) {
            return toHit.y / velocity.y;
        } else {
            return toHit.z / velocity.z;
        }
    }
}