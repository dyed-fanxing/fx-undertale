package com.sakpeipei.undertale.common;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * @author yujinbao
 * @since 2026/1/5 16:40
 * 局部向量类：输入为局部坐标，存储为转换后的全局坐标
 */
public class LocalVec3 extends Vec3 {

    // 全局坐标系基准方向
    public static final Vec3 GLOBAL_DOWN = new Vec3(0, -1, 0);   // 全局重力向下
    public static final Vec3 GLOBAL_NORTH = new Vec3(0, 0, -1);  // 全局正北
    public static final Vec3 GLOBAL_SOUTH = new Vec3(0, 0, 1);   // 全局正南
    public static final Vec3 GLOBAL_EAST = new Vec3(1, 0, 0);    // 全局正东
    public static final Vec3 GLOBAL_WEST = new Vec3(-1, 0, 0);   // 全局正西

    // 局部坐标系定义
    private final LocalDirection localDirection;

    /**
     * 构造函数：传入局部坐标，根据局部重力方向转换为全局坐标存储
     * @param localDirection 局部重力方向（定义局部坐标系）
     * @param localCoords 局部坐标系中的坐标
     */
    public LocalVec3(LocalDirection localDirection, Vec3 localCoords) {
        // 将局部坐标转换为全局坐标，然后调用父类构造函数
        super(convertLocalToGlobal(localDirection, localCoords).toVector3f());
        this.localDirection = localDirection;
    }

    /**
     * 将局部坐标转换为全局坐标
     */
    private static Vec3 convertLocalToGlobal(LocalDirection localDir, Vec3 localVec) {
        // 获取局部坐标系的旋转矩阵（从局部到全局）
        Quaternionf rotation = getLocalToGlobalRotation(localDir);

        // 旋转局部坐标到全局坐标系
        Vector3f global = new Vector3f(
                (float)localVec.x,
                (float)localVec.y,
                (float)localVec.z
        );
        rotation.transform(global);

        return new Vec3(global.x, global.y, global.z);
    }

    /**
     * 获取从局部坐标系到全局坐标系的旋转
     */
    private static Quaternionf getLocalToGlobalRotation(LocalDirection localDir) {
        // 如果局部重力与全局重力一致，不需要旋转
        if (localDir.gravityVector.equals(GLOBAL_DOWN)) {
            return new Quaternionf(); // 单位四元数，不旋转
        }

        // 计算旋转轴：全局重力与局部重力的叉积
        Vec3 globalGravity = GLOBAL_DOWN;
        Vec3 localGravity = localDir.gravityVector;
        Vec3 axis = globalGravity.cross(localGravity);

        // 如果方向完全相反，需要特殊处理
        if (axis.lengthSqr() < 1e-9) {
            // 重力方向完全相反（上/下），绕Z轴旋转180度
            if (localGravity.equals(new Vec3(0, 1, 0))) {
                // 局部重力向上，全局重力向下
                // 局部Y轴与全局Y轴相反，需要旋转180度
                // 选择绕全局北方向旋转180度
                return new Quaternionf().rotationY((float)Math.PI);
            }
            return new Quaternionf(); // 不应该到达这里
        }

        // 计算旋转角度
        double cosAngle = globalGravity.dot(localGravity);
        float angle = (float)Math.acos(cosAngle);

        // 创建旋转四元数
        return new Quaternionf().fromAxisAngleRad(
                new Vector3f((float)axis.x, (float)axis.y, (float)axis.z),
                angle
        );
    }

    /**
     * 将存储的全局坐标转换回局部坐标
     */
    public Vec3 toLocalCoordinates() {
        return convertGlobalToLocal(this.localDirection, this);
    }

    /**
     * 静态方法：将全局坐标转换为局部坐标
     */
    public static Vec3 convertGlobalToLocal(LocalDirection localDir, Vec3 globalVec) {
        // 获取从全局到局部的旋转（局部到全局的逆旋转）
        Quaternionf rotation = getLocalToGlobalRotation(localDir);
        Quaternionf inverse = new Quaternionf(rotation).conjugate();

        // 旋转全局坐标到局部坐标系
        Vector3f local = new Vector3f(
                (float)globalVec.x,
                (float)globalVec.y,
                (float)globalVec.z
        );
        inverse.transform(local);

        return new Vec3(local.x, local.y, local.z);
    }

    /**
     * 获取局部重力方向（在全局坐标系中）
     */
    public Vec3 getLocalGravityDirection() {
        return this.localDirection.gravityVector;
    }

    /**
     * 获取局部坐标系的"上"方向（在局部坐标系中总是(0,1,0)，但这里返回全局表示）
     */
    public Vec3 getLocalUpDirection() {
        // 局部坐标系的上方向在全局中的表示
        return convertLocalToGlobal(this.localDirection, new Vec3(0, 1, 0));
    }

    /**
     * 获取局部坐标系的"北"方向（在局部坐标系中总是(0,0,-1)，但这里返回全局表示）
     */
    public Vec3 getLocalNorthDirection() {
        return convertLocalToGlobal(this.localDirection, new Vec3(0, 0, -1));
    }

    /**
     * 获取局部坐标系的"东"方向（在局部坐标系中总是(1,0,0)，但这里返回全局表示）
     */
    public Vec3 getLocalEastDirection() {
        return convertLocalToGlobal(this.localDirection, new Vec3(1, 0, 0));
    }

    /**
     * 局部方向枚举
     */
    public enum LocalDirection {
        // 重力方向定义（在全局坐标系中）
        DOWN(GLOBAL_DOWN),      // 重力向下（默认）
        UP(new Vec3(0, 1, 0)),  // 重力向上
        NORTH(GLOBAL_NORTH),    // 重力向北
        SOUTH(GLOBAL_SOUTH),    // 重力向南
        EAST(GLOBAL_EAST),      // 重力向东
        WEST(GLOBAL_WEST);      // 重力向西

        public final Vec3 gravityVector;

        LocalDirection(Vec3 gravity) {
            this.gravityVector = gravity;
        }

        public static LocalDirection fromMinecraftDirection(Direction dir) {
            return switch (dir) {
                case DOWN -> DOWN;
                case UP -> UP;
                case NORTH -> NORTH;
                case SOUTH -> SOUTH;
                case EAST -> EAST;
                case WEST -> WEST;
            };
        }
    }

    /**
     * 便捷构造方法：使用double参数
     */
    public LocalVec3(LocalDirection localDirection, double localX, double localY, double localZ) {
        this(localDirection, new Vec3(localX, localY, localZ));
    }
}