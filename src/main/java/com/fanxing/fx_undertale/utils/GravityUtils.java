package com.fanxing.fx_undertale.utils;

import com.fanxing.fx_undertale.common.phys.LocalDirection;
import com.fanxing.fx_undertale.entity.capability.OBBHolder;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.util.Mth.HALF_PI;

public class GravityUtils {
    private static final Logger log = LoggerFactory.getLogger(GravityUtils.class);
    public static double COLLIED_EPSILON = 1.0E-12;
    public static double FLOOR_EPSILON = 1.0E-6;

    public static final Quaterniond DOWN = new Quaterniond();
    public static final Quaterniond UP = new Quaterniond().rotationZ(Math.PI);
    public static final Quaterniond EAST = new Quaterniond().rotationY(Math.PI/2).rotateX(-Math.PI/2);
    public static final Quaterniond WEST = new Quaterniond().rotationY(-Math.PI/2).rotateX(-Math.PI/2);
    // KEY！！！ 由于双精度的rotationX方法的z w顺序与单精度的相反，以单精度的为准，这里直接显示声明，不知道这是JOML有意设计，还是bug，总之和单精度的不一致，详情可见源码
    public static final Quaterniond SOUTH = new Quaterniond(-0.7071067811865475, 0, 0, 0.7071067811865475);
    public static final Quaterniond NORTH = new Quaterniond().rotationY(Math.PI).rotateX(-Math.PI/2);

    public static final Quaterniond DOWN_INV = DOWN.conjugate(new Quaterniond()); // 对于单位四元数，共轭等于逆
    public static final Quaterniond UP_INV = UP.conjugate(new Quaterniond());
    public static final Quaterniond EAST_INV = EAST.conjugate(new Quaterniond());
    public static final Quaterniond WEST_INV = WEST.conjugate(new Quaterniond());
    public static final Quaterniond SOUTH_INV = SOUTH.conjugate(new Quaterniond());
    public static final Quaterniond NORTH_INV = NORTH.conjugate(new Quaterniond());


    public static final Quaternionf DOWN_F = new Quaternionf();
    public static final Quaternionf UP_F = new Quaternionf().rotationZ(Mth.PI);
    public static final Quaternionf EAST_F = new Quaternionf().rotationY(HALF_PI).rotateX(-HALF_PI);
    public static final Quaternionf WEST_F = new Quaternionf().rotationY(-Mth.HALF_PI).rotateX(-Mth.HALF_PI);
    public static final Quaternionf SOUTH_F = new Quaternionf().rotationX(-Mth.HALF_PI);
    public static final Quaternionf NORTH_F = new Quaternionf().rotationY(Mth.PI).rotateX(-Mth.HALF_PI);

    /**
     * 对目标实体应用攻击者的相对重力
     */
    public static Direction applyRelativeGravity(Entity attacker, Entity target, LocalDirection localGravity) {
        Direction forward = Direction.fromYRot(attacker.getYHeadRot());
        return applyGravity(target, switch (localGravity) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case FRONT -> forward;
            case BACK -> forward.getOpposite();
            case LEFT -> forward.getCounterClockWise();
            case RIGHT -> forward.getClockWise();
        });
    }

    /**
     * 对目标实体应用攻击者的相对重力
     */
    public static Direction applyGravity(Entity target, Direction gravity) {
        Direction oldGravity = target.getData(AttachmentTypes.GRAVITY);
        if (oldGravity == gravity) return oldGravity;
        target.setData(AttachmentTypes.GRAVITY, gravity);
        if (!(target instanceof OBBHolder)) {
            Vec3 center = target.getBoundingBox().getCenter();
            // 中心旋转公式
            Vec3 pos = center.subtract(GravityUtils.getVec3(gravity.getOpposite()).scale(target.getBbHeight() * 0.5f));
            target.setPos(pos);
            // 旋转后如果在方块内部，则调整在这个方块表面
            resolveCollision(target, gravity);
        }
        return gravity;
    }

    private static void resolveCollision(Entity entity, Direction gravity) {
        AABB aabb = entity.getBoundingBox();
        Level level = entity.level();
        Direction.Axis axis = gravity.getAxis();
        double minE = aabb.min(axis);
        double maxE = aabb.max(axis);
        double dy = 0;
        double maxFootS = 0;
        double minHeadS = Double.MAX_VALUE;
        for (VoxelShape shape : level.getBlockCollisions(null, aabb)) {
            if (shape.isEmpty()) continue;
            double minS = shape.min(axis);
            double maxS = shape.max(axis);
            if (minE < maxS && maxE > maxS) {
                maxFootS = Math.max(maxFootS, maxS);
                dy = maxFootS - minE;
            }
            if (maxE > minS && minE < minS){
                minHeadS = Math.min(minHeadS, minE);
                dy = minHeadS - maxE;
            }
        }
        if(dy != 0) entity.setPos(entity.position().add(GravityUtils.projectToAxis(dy,gravity)));
    }



    public static Vec3 findGround(Level level, Vec3 pos, Direction gravity) {
        Vec3i normal = gravity.getNormal();
        Vec3 g = new Vec3(normal.getX(), normal.getY(), normal.getZ());
        BlockPos searchPos = getContaining(gravity,pos);
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
                return compose(getSurfaceBlockHeight(searchPos,gravity).add(g.scale(collisionHeight)),pos);
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
    public static BlockPos getContaining(Direction gravity,Vec3 offset){
        Vec3i normal = gravity.getNormal();
        return BlockPos.containing(offset.x + (normal.getX()<0?-FLOOR_EPSILON:0), offset.y+(normal.getY()>0?-FLOOR_EPSILON:0), offset.z+(normal.getZ()>0?-FLOOR_EPSILON:0));
    }
    public static BlockPos getContaining(Direction gravity,double x, double y,double z){
        Vec3i normal = gravity.getNormal();
        return BlockPos.containing(x+(normal.getX()>0?-FLOOR_EPSILON:0), y+(normal.getY()>0?-FLOOR_EPSILON:0), z+(normal.getZ()>0?-FLOOR_EPSILON:0));
    }

    // 提取向量在重力轴上的分量（其他轴清零）
    public static Vec3 getAxialComponent(Direction gravity,Vec3 vec) {
        return switch (gravity.getAxis()) {
            case X -> new Vec3(vec.x, 0, 0);
            case Y -> new Vec3(0, vec.y, 0);
            case Z -> new Vec3(0, 0, vec.z);
        };
    }

    /**
     * 组合，取重力轴上的分量，和其他轴的向量
     * @param vec3 向量
     * @param gravity 重力方向上的分量
     */
    public static Vec3 compose(Vec3 gravity,Vec3 vec3) {
        return new Vec3(
                gravity.x == 0 ? vec3.x : gravity.x,
                gravity.y == 0 ? vec3.y : gravity.y,
                gravity.z == 0 ? vec3.z : gravity.z
        );
    }
    /**
     * 投影到gravity轴上
     * @param y 局部y坐标
     * @param gravity 重力方向上的分量
     */
    public static Vec3 projectToAxis(double y, Direction gravity) {
        Vec3 g = getVec3(gravity);
        return new Vec3(
                g.x == 0 ? 0 : y,
                g.y == 0 ? 0 : y,
                g.z == 0 ? 0 : y
        );
    }

    public static Vec3 roundTo(Vec3 vec, double precision) {
        return new Vec3(
                Math.round(vec.x / precision) * precision,
                Math.round(vec.y / precision) * precision,
                Math.round(vec.z / precision) * precision
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



    public static Quaterniond getLocalToWorld(Direction gravity){
        return switch (gravity){
            case DOWN -> DOWN;
            case UP -> UP;
            case EAST -> EAST;
            case WEST -> WEST;
            case SOUTH -> SOUTH;
            case NORTH -> NORTH;
        };
    }
    public static Quaterniond getWorldToLocal(Direction gravity){
        return switch (gravity){
            case DOWN -> DOWN_INV;
            case UP -> UP_INV;
            case EAST -> EAST_INV;
            case WEST -> WEST_INV;
            case SOUTH -> SOUTH_INV;
            case NORTH -> NORTH_INV;
        };
    }
    public static Quaternionf getLocalToWorldF(Direction gravity){
        return switch (gravity){
            case DOWN -> DOWN_F;
            case UP -> UP_F;
            case EAST -> EAST_F;
            case WEST -> WEST_F;
            case SOUTH -> SOUTH_F;
            case NORTH -> NORTH_F;
        };
    }


    public static Vec3 localToWorld(Entity entity, Vec3 offset) {
        return localToWorld(entity.getData(AttachmentTypes.GRAVITY),offset);
    }
    public static Vec3 localToWorld(Direction gravity,Vec3 vec) {
        Vector3d out = getLocalToWorld(gravity).transform(new Vector3d(vec.x, vec.y, vec.z), new Vector3d());
        return new Vec3(out.x, out.y, out.z);
    }

    public static Vec3 localToWorld(Direction gravity,double x, double y, double z) {
        Vector3d out = getLocalToWorld(gravity).transform(new Vector3d(x,y,z));
        return new Vec3(out.x, out.y, out.z);
    }


    public static Vector3f localToWorld(Direction gravity,Vector3f vector3f) {
        Vector3d out = getLocalToWorld(gravity).transform(new Vector3d(vector3f));
        return new Vector3f((float) out.x, (float) out.y, (float) out.z);
    }

    public static Vector3f localToWorld(Direction gravity,float x, float y, float z) {
        Vector3d out = getLocalToWorld(gravity).transform(new Vector3d(x,y,z));
        return new Vector3f((float) out.x, (float) out.y, (float) out.z);
    }


    public static Vec3 worldToLocal(Direction gravity,double x, double y, double z) {
        Vector3d out = getWorldToLocal(gravity).transform(new Vector3d(x,y,z));
        return new Vec3(out.x, out.y, out.z);
    }

    public static Vec3 worldToLocal(Direction gravity, Vec3 vec) {
        Vector3d out = getWorldToLocal(gravity).transform(new Vector3d(vec.x,vec.y,vec.z));
        return new Vec3(out.x, out.y, out.z);
    }

    /**
     * 获取局部坐标系的右方向在世界坐标系中的向量
     * 局部右方向 = (1,0,0)，转换到世界坐标系
     */
    public static Vector3f getRight(Direction gravity) {
        // 局部右方向 (1,0,0) 转换到世界坐标系
        return getLocalToWorldF(gravity).transform(new Vector3f(1, 0, 0));
    }
    /**
     * 获取局部坐标系的上方向在世界坐标系中的向量
     * 局部上方向 = (0,1,0)，转换到世界坐标系
     */
    public static Vector3f getUp(Direction gravity) {
        return getLocalToWorldF(gravity).transform(new Vector3f(0, 1, 0));
    }
    /**
     * 获取局部坐标系的前方向在世界坐标系中的向量
     * 局部前方向 = (0,0,1)，转换到世界坐标系
     */
    public static Vector3f getForward(Direction gravity) {
        return getLocalToWorldF(gravity).transform(new Vector3f(0, 0, 1));
    }
    /**
     * 获取局部坐标系的右方向在世界坐标系中的向量
     * 局部右方向 = (1,0,0)，转换到世界坐标系
     */
    public static Vector3f getRight(Entity entity) {
        // 局部右方向 (1,0,0) 转换到世界坐标系
        return getLocalToWorldF(entity.getData(AttachmentTypes.GRAVITY)).transform(new Vector3f(1, 0, 0));
    }
    /**
     * 获取局部坐标系的上方向在世界坐标系中的向量
     * 局部上方向 = (0,1,0)，转换到世界坐标系
     */
    public static Vector3f getUp(Entity entity) {
        return getLocalToWorldF(entity.getData(AttachmentTypes.GRAVITY)).transform(new Vector3f(0, 1, 0));
    }
    /**
     * 获取局部坐标系的前方向在世界坐标系中的向量
     * 局部前方向 = (0,0,1)，转换到世界坐标系
     */
    public static Vector3f getForward(Entity entity) {
        return getLocalToWorldF(entity.getData(AttachmentTypes.GRAVITY)).transform(new Vector3f(0, 0, 1));
    }
}
