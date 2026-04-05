package com.fanxing.fx_undertale.level.pathfinder;

import com.fanxing.fx_undertale.registry.AttachmentTypes;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

public class GravityAwareNodeEvaluator extends NodeEvaluator {

    private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();
    private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap<>();
    private final Node[] reusableNeighbors = new Node[4]; // 用于水平方向邻居缓存

    // 重力数据
    private final Direction gravityDir;      // 重力方向（向下）
    private final Vec3i down;                // 重力方向向量
    private final Vec3i up;                  // 反重力方向（向上）
    private final Direction.Axis gravityAxis; // 重力轴

    private PathNavigationRegion region;
    private Mob mob;

    public GravityAwareNodeEvaluator(Mob mob) {
        Direction gravity = mob.getData(AttachmentTypes.GRAVITY.get());
        this.gravityDir = gravity;
        this.down = gravityDir.getNormal();
        this.up = gravityDir.getOpposite().getNormal();
        this.gravityAxis = gravityDir.getAxis();
    }

    @Override
    public void prepare(PathNavigationRegion region, Mob mob) {
        super.prepare(region, mob);
        this.region = region;
        this.mob = mob;
        this.pathTypesByPosCache.clear();
        this.collisionCache.clear();
        this.entityWidth = Mth.floor(mob.getBbWidth() + 1.0F);
        this.entityHeight = Mth.floor(mob.getBbHeight() + 1.0F);
        this.entityDepth = Mth.floor(mob.getBbWidth() + 1.0F);
    }

    @Override
    public void done() {
        super.done();
        this.region = null;
        this.mob = null;
    }

    // 返回沿“上”方向的高度坐标（投影）
    private double getUpCoord(BlockPos pos) {
        return pos.getX() * up.getX() + pos.getY() * up.getY() + pos.getZ() * up.getZ();
    }

    private double getUpCoord(Node node) {
        return node.x * up.getX() + node.y * up.getY() + node.z * up.getZ();
    }

    // 获取某位置的地面高度（返回沿“上”方向的高度坐标，即投影）
    private double getFloorLevel(BlockPos pos) {
        BlockPos groundPos = findGroundBelow(pos);
        // 站在该方块的上表面（沿 up 方向半个方块）
        // 投影坐标加上0.5
        return getUpCoord(groundPos) + 0.5;
    }

    // 向重力方向找到第一个非空气方块（返回世界坐标）
    private BlockPos findGroundBelow(BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();
        while (pos.getY() > region.getMinBuildHeight()) {
            if (!region.getBlockState(pos).isAir()) {
                return pos.immutable();
            }
            pos.move(down.getX(), down.getY(), down.getZ());
        }
        return start;
    }

    // 向重力方向找到第一个非空气方块，但用于节点搜索（可能需返回节点）
    private BlockPos findNonAirBelow(BlockPos start) {
        return findGroundBelow(start); // 复用
    }

    @Override
    public Node getStart() {
        BlockPos pos = mob.blockPosition();
        // 调整起始点到地面
        BlockPos groundPos = findGroundBelow(pos);
        Node node = getNode(groundPos);
        node.type = getCachedPathType(groundPos.getX(), groundPos.getY(), groundPos.getZ());
        node.costMalus = mob.getPathfindingMalus(node.type);
        return node;
    }

    @Override
    public Target getTarget(double x, double y, double z) {
        BlockPos targetPos = BlockPos.containing(x, y, z);
        BlockPos groundPos = findGroundBelow(targetPos);
        return getTargetNodeAt(groundPos.getX(), groundPos.getY(), groundPos.getZ());
    }

    @Override
    public int getNeighbors(Node[] output, Node node) {
        int count = 0;
        int maxStep = 0;
        PathType typeAbove = getCachedPathType(node.x + up.getX(), node.y + up.getY(), node.z + up.getZ());
        PathType typeCurrent = getCachedPathType(node.x, node.y, node.z);
        if (mob.getPathfindingMalus(typeAbove) >= 0.0F && typeCurrent != PathType.STICKY_HONEY) {
            maxStep = Mth.floor(Math.max(1.0F, mob.maxUpStep()));
        }

        double currentFloor = getFloorLevel(new BlockPos(node.x, node.y, node.z));

        // 水平方向（垂直于重力轴的平面）
        Direction[] horizontals = getHorizontalDirections();
        for (int i = 0; i < horizontals.length; i++) {
            Direction dir = horizontals[i];
            Node neighbor = findAcceptedNode(
                    node.x + dir.getStepX(), node.y + dir.getStepY(), node.z + dir.getStepZ(),
                    maxStep, currentFloor, dir, typeCurrent
            );
            reusableNeighbors[i] = neighbor;
            if (isNeighborValid(neighbor, node)) {
                output[count++] = neighbor;
            }
        }

        // 对角线方向（基于上述邻居）
        for (int i = 0; i < horizontals.length; i++) {
            Direction dir1 = horizontals[i];
            Direction dir2 = horizontals[(i + 1) % horizontals.length]; // 顺时针下一个
            if (isDiagonalValid(node, reusableNeighbors[i], reusableNeighbors[(i + 1) % horizontals.length])) {
                Node diag = findAcceptedNode(
                        node.x + dir1.getStepX() + dir2.getStepX(),
                        node.y + dir1.getStepY() + dir2.getStepY(),
                        node.z + dir1.getStepZ() + dir2.getStepZ(),
                        maxStep, currentFloor, dir1, typeCurrent
                );
                if (isDiagonalValid(diag)) {
                    output[count++] = diag;
                }
            }
        }

        return count;
    }

    // 获取垂直于重力轴的水平方向
    private Direction[] getHorizontalDirections() {
        return switch (gravityAxis) {
            case Y -> new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            case X -> new Direction[]{Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH}; // 重力东西时，水平面是 YZ 平面
            case Z -> new Direction[]{Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST}; // 重力南北时，水平面是 XY 平面
        };
    }

    @Nullable
    private Node findAcceptedNode(int x, int y, int z, int maxStep, double currentFloor, Direction dir, PathType currentNodeType) {
        // 检查目标方块类型
        PathType type = getCachedPathType(x, y, z);
        float malus = mob.getPathfindingMalus(type);
        if (malus < 0.0F) return null;

        // 目标位置的地面高度
        double targetFloor = getFloorLevel(new BlockPos(x, y, z));
        double heightDiff = targetFloor - currentFloor;

        // 检查是否超过最大步高（向上）
        if (heightDiff > maxStep) return null;

        // 如果是向下移动，检查是否超过最大下落距离
        if (dir == gravityDir && heightDiff < -mob.getMaxFallDistance()) return null;

        // 创建节点
        Node node = getNode(x, y, z);
        node.type = type;
        node.costMalus = malus;

        // 检查是否有部分碰撞（如栅栏、关闭的门）且无法通过
        if (doesBlockHavePartialCollision(currentNodeType) && node.costMalus >= 0.0F && !canReachWithoutCollision(node)) {
            return null;
        }

        // 特殊处理非可行走类型
        if (type != PathType.WALKABLE && (!isAmphibious() || type != PathType.WATER)) {
            if ((node.costMalus < 0.0F) && maxStep > 0 && (type != PathType.FENCE || canWalkOverFences())
                    && type != PathType.UNPASSABLE_RAIL && type != PathType.TRAPDOOR && type != PathType.POWDER_SNOW) {
                node = tryJumpOn(x, y, z, maxStep, currentFloor, dir, currentNodeType);
            } else if (!isAmphibious() && type == PathType.WATER && !canFloat()) {
                node = tryFindFirstNonWaterBelow(x, y, z, node);
            } else if (type == PathType.OPEN) {
                node = tryFindFirstGroundNodeBelow(x, y, z);
            }
        }

        return node;
    }

    @Nullable
    private Node tryJumpOn(int x, int y, int z, int maxStep, double currentFloor, Direction dir, PathType currentNodeType) {
        // 尝试向上跳一格（沿 up 方向）
        int upX = x + up.getX();
        int upY = y + up.getY();
        int upZ = z + up.getZ();
        Node node = findAcceptedNode(upX, upY, upZ, maxStep - 1, currentFloor, dir, currentNodeType);
        if (node == null) return null;
        if (mob.getBbWidth() >= 1.0F) return node;
        if (node.type != PathType.OPEN && node.type != PathType.WALKABLE) return node;

        // 碰撞箱检查
        double d0 = (double) (x - dir.getStepX()) + 0.5;
        double d1 = (double) (z - dir.getStepZ()) + 0.5;
        double w = mob.getBbWidth() / 2.0;
        // 注意：这里的 y 是原位置，但我们用 up 方向调整？
        // 原版用 y+1，我们这里用 up 方向
        BlockPos groundPos = new BlockPos(x, y, z);
        BlockPos upGroundPos = new BlockPos(upX, upY, upZ);
        AABB aabb = new AABB(
                d0 - w,
                getFloorLevel(groundPos) + 0.001,
                d1 - w,
                d0 + w,
                mob.getBbHeight() + getFloorLevel(upGroundPos) - 0.002,
                d1 + w
        );
        if (hasCollisions(aabb)) return null;
        return node;
    }

    @Nullable
    private Node tryFindFirstNonWaterBelow(int x, int y, int z, @Nullable Node node) {
        // 向重力方向找非水方块
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (pos.getY() > region.getMinBuildHeight()) {
            pos.move(down.getX(), down.getY(), down.getZ());
            int nx = pos.getX(), ny = pos.getY(), nz = pos.getZ();
            PathType type = getCachedPathType(nx, ny, nz);
            if (type != PathType.WATER) {
                return node;
            }
            node = getNodeAndUpdateCostToMax(nx, ny, nz, type, mob.getPathfindingMalus(type));
        }
        return node;
    }

    private Node tryFindFirstGroundNodeBelow(int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        int startY = pos.getY();
        while (pos.getY() > region.getMinBuildHeight()) {
            pos.move(down.getX(), down.getY(), down.getZ());
            int nx = pos.getX(), ny = pos.getY(), nz = pos.getZ();
            if (startY - ny > mob.getMaxFallDistance()) {
                return getBlockedNode(nx, ny, nz);
            }
            PathType type = getCachedPathType(nx, ny, nz);
            float malus = mob.getPathfindingMalus(type);
            if (type != PathType.OPEN) {
                if (malus >= 0.0F) {
                    return getNodeAndUpdateCostToMax(nx, ny, nz, type, malus);
                } else {
                    return getBlockedNode(nx, ny, nz);
                }
            }
        }
        return getBlockedNode(x, y, z);
    }

    private Node getNodeAndUpdateCostToMax(int x, int y, int z, PathType type, float malus) {
        Node node = getNode(x, y, z);
        node.type = type;
        node.costMalus = Math.max(node.costMalus, malus);
        return node;
    }

    private Node getBlockedNode(int x, int y, int z) {
        Node node = getNode(x, y, z);
        node.type = PathType.BLOCKED;
        node.costMalus = -1.0F;
        return node;
    }

    private Node getClosedNode(int x, int y, int z, PathType type) {
        Node node = getNode(x, y, z);
        node.closed = true;
        node.type = type;
        node.costMalus = type.getMalus();
        return node;
    }

    protected boolean isNeighborValid(@Nullable Node neighbor, Node current) {
        return neighbor != null && !neighbor.closed && (neighbor.costMalus >= 0.0F || current.costMalus < 0.0F);
    }

    protected boolean isDiagonalValid(Node center, @Nullable Node neighbor1, @Nullable Node neighbor2) {
        if (neighbor1 == null || neighbor2 == null) return false;
        if (neighbor1.type == PathType.WALKABLE_DOOR || neighbor2.type == PathType.WALKABLE_DOOR) return false;
        double upCenter = getUpCoord(center);
        double up1 = getUpCoord(neighbor1);
        double up2 = getUpCoord(neighbor2);
        if (up1 <= upCenter && up2 <= upCenter) {
            boolean flag = neighbor1.type == PathType.FENCE && neighbor2.type == PathType.FENCE && mob.getBbWidth() < 0.5F;
            return (up1 < upCenter || neighbor1.costMalus >= 0.0F || flag) &&
                    (up2 < upCenter || neighbor2.costMalus >= 0.0F || flag);
        }
        return false;
    }

    protected boolean isDiagonalValid(@Nullable Node node) {
        if (node == null || node.closed) return false;
        if (node.type == PathType.WALKABLE_DOOR) return false;
        return node.costMalus >= 0.0F;
    }

    private boolean doesBlockHavePartialCollision(PathType type) {
        return type == PathType.FENCE || type == PathType.DOOR_WOOD_CLOSED || type == PathType.DOOR_IRON_CLOSED;
    }

    private boolean canReachWithoutCollision(Node target) {
        AABB aabb = mob.getBoundingBox();
        Vec3 delta = new Vec3(
                target.x - mob.getX() + aabb.getXsize() / 2.0,
                target.y - mob.getY() + aabb.getYsize() / 2.0,
                target.z - mob.getZ() + aabb.getZsize() / 2.0
        );
        int steps = Mth.ceil(delta.length() / aabb.getSize());
        delta = delta.scale(1.0 / steps);
        for (int i = 1; i <= steps; i++) {
            aabb = aabb.move(delta);
            if (hasCollisions(aabb)) return false;
        }
        return true;
    }

    private boolean hasCollisions(AABB aabb) {
        return collisionCache.computeIfAbsent(aabb, (p) -> !region.noCollision(mob, aabb));
    }

    protected PathType getCachedPathType(int x, int y, int z) {
        return pathTypesByPosCache.computeIfAbsent(BlockPos.asLong(x, y, z),
                k -> getPathTypeOfMob(currentContext, x, y, z, mob));
    }

    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
        Set<PathType> set = getPathTypeWithinMobBB(context, x, y, z);
        if (set.contains(PathType.FENCE)) return PathType.FENCE;
        if (set.contains(PathType.UNPASSABLE_RAIL)) return PathType.UNPASSABLE_RAIL;
        PathType result = PathType.BLOCKED;
        for (PathType type : set) {
            if (mob.getPathfindingMalus(type) < 0.0F) return type;
            if (mob.getPathfindingMalus(type) >= mob.getPathfindingMalus(result)) result = type;
        }
        if (entityWidth <= 1 && result != PathType.OPEN && mob.getPathfindingMalus(result) == 0.0F &&
                getPathType(context, x, y, z) == PathType.OPEN) {
            return PathType.OPEN;
        }
        return result;
    }

    public Set<PathType> getPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
        EnumSet<PathType> set = EnumSet.noneOf(PathType.class);
        for (int dx = 0; dx < entityWidth; dx++) {
            for (int dy = 0; dy < entityHeight; dy++) {
                for (int dz = 0; dz < entityDepth; dz++) {
                    int ix = x + dx;
                    int iy = y + dy;
                    int iz = z + dz;
                    PathType type = getPathType(context, ix, iy, iz);
                    // 处理门、铁轨等（简化，可根据需要添加原版逻辑）
                    if (type == PathType.DOOR_WOOD_CLOSED && canOpenDoors() && canPassDoors()) {
                        type = PathType.WALKABLE_DOOR;
                    }
                    if (type == PathType.DOOR_OPEN && !canPassDoors()) {
                        type = PathType.BLOCKED;
                    }
                    // 铁轨处理略
                    set.add(type);
                }
            }
        }
        return set;
    }

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        // 获取当前方块类型
        PathType type = context.getPathTypeFromState(x, y, z);

        // 如果当前方块是空气且不在世界最底部，检查重力方向下方的方块
        if (type == PathType.OPEN && y > context.level().getMinBuildHeight()) {
            int downX = x + down.getX();
            int downY = y + down.getY();
            int downZ = z + down.getZ();
            PathType downType = context.getPathTypeFromState(downX, downY, downZ);

            // 根据下方方块类型决定返回类型
            switch (downType) {
                case OPEN:
                case WATER:
                case LAVA:
                case WALKABLE:
                    return PathType.OPEN;
                case DAMAGE_FIRE:
                    return PathType.DAMAGE_FIRE;
                case DAMAGE_OTHER:
                    return PathType.DAMAGE_OTHER;
                case STICKY_HONEY:
                    return PathType.STICKY_HONEY;
                case POWDER_SNOW:
                    return PathType.DANGER_POWDER_SNOW;
                case DAMAGE_CAUTIOUS:
                    return PathType.DAMAGE_CAUTIOUS;
                case TRAPDOOR:
                    return PathType.DANGER_TRAPDOOR;
                default:
                    // 检查周围邻居是否有危险
                    return checkNeighbourBlocks(context, x, y, z, PathType.WALKABLE);
            }
        }
        return type;
    }

    private PathType checkNeighbourBlocks(PathfindingContext context, int x, int y, int z, PathType fallback) {
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    // 跳过正上方/正下方（i=0, k=0）
                    if (i == 0 && k == 0) continue;
                    PathType neighborType = context.getPathTypeFromState(x + i, y + j, z + k);
                    if (neighborType == PathType.DAMAGE_OTHER) {
                        return PathType.DANGER_OTHER;
                    }
                    if (neighborType == PathType.DAMAGE_FIRE || neighborType == PathType.LAVA) {
                        return PathType.DANGER_FIRE;
                    }
                    if (neighborType == PathType.WATER) {
                        return PathType.WATER_BORDER;
                    }
                    if (neighborType == PathType.DAMAGE_CAUTIOUS) {
                        return PathType.DAMAGE_CAUTIOUS;
                    }
                }
            }
        }
        return fallback;
    }


    protected boolean isAmphibious() {
        return false; // 根据需求可修改
    }

    // 以下方法可以保留原样，它们不依赖Y轴
    @Override
    public void setCanPassDoors(boolean canPassDoors) {
        super.setCanPassDoors(canPassDoors);
    }

    @Override
    public void setCanOpenDoors(boolean canOpenDoors) {
        super.setCanOpenDoors(canOpenDoors);
    }

    @Override
    public void setCanFloat(boolean canFloat) {
        super.setCanFloat(canFloat);
    }

    @Override
    public void setCanWalkOverFences(boolean canWalkOverFences) {
        super.setCanWalkOverFences(canWalkOverFences);
    }
}