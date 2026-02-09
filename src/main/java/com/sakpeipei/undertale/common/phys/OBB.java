package com.sakpeipei.undertale.common.phys;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/**
 * 完整的OBB（定向包围盒）实现
 * 支持3D旋转
 * 优化：直接存储三个正交轴，不存角度
 */
public class OBB {
    // 核心属性
    public final Vec3 center;        // 中心点
    public final float xHalfSize,yHalfSize,zHalfSize; // 半径
    // 三个正交轴（已归一化）
    public final Vec3 forward;       // 前方向
    public final Vec3 up;            // 上方向
    public final Vec3 right;         // 右方向
    // 缓存
    private Vec3[] cachedVertices;
    private AABB cachedAABB;

    // ============== 构造方法 ==============


    /**
     * 从中心、尺寸和前方向创建OBB（自动计算up和right）
     */
    public OBB(Vec3 center,float xHalfSize, float yHalfSize, float zHalfSize,  Vec3 forwardDirection) {
        this(center, xHalfSize,yHalfSize,zHalfSize, forwardDirection, calculateUpFromForward(forwardDirection));
    }
    /**
     * 从yaw和pitch角度创建OBB
     */
    public OBB(Vec3 center,float xHalfSize, float yHalfSize, float zHalfSize, float yaw, float pitch) {
        this(center, xHalfSize,yHalfSize,zHalfSize, calculateForwardFromYawPitch(yaw, pitch));
    }
    /**
     * 从中心、尺寸和三个正交轴创建OBB
     */
    public OBB(Vec3 center, float xHalfSize, float yHalfSize, float zHalfSize,  Vec3 forward, Vec3 up) {
        this.center = center;
        this.xHalfSize = xHalfSize;
        this.yHalfSize = yHalfSize;
        this.zHalfSize = zHalfSize;
        // 1. 前方向归一化
        Vec3 f = forward.normalize();
        // 2. 对up进行施密特正交化
        // 从up中减去在forward方向上的投影
        double proj = up.dot(f);
        Vec3 u = up.subtract(f.scale(proj)).normalize();
        // 3. 计算右方向
        Vec3 r = f.cross(u).normalize();
        // 4. 重新计算上方向（确保完全正交）
        u = r.cross(f).normalize();
        this.forward = f;
        this.up = u;
        this.right = r;
    }
    // ============== 辅助方法 ==============

    /**
     * 从前方向计算上方向
     */
    private static Vec3 calculateUpFromForward(Vec3 forward) {
        // 如果前方向接近垂直，使用特殊的上方向
        if (Math.abs(forward.y) > 0.99) {
            // 接近垂直，使用世界X轴作为上方向的参考
            return new Vec3(1, 0, 0);
        }

        // 正常情况下，上方向是前方向和世界Y轴的叉积
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(worldUp).normalize();
        return right.cross(forward).normalize();
    }

    /**
     * 从yaw和pitch计算前方向
     */
    private static Vec3 calculateForwardFromYawPitch(float yaw, float pitch) {
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * cosPitch,-Math.sin(pitchRad),Math.cos(yawRad) * cosPitch).normalize();
    }

    // ============== 基本属性 ==============

    public Vec3 getCenter() {
        return center;
    }

    /**
     * 需要时计算yaw（偏航角）
     */
    public float getYaw() {
        // 从前方向向量计算yaw
        double yawRad = Math.atan2(-forward.x, forward.z);
        return (float) Math.toDegrees(yawRad);
    }

    /**
     * 需要时计算pitch（俯仰角）
     */
    public float getPitch() {
        // 从前方向向量计算pitch
        double forwardLength = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
        double pitchRad = Math.atan2(-forward.y, forwardLength);
        return (float) Math.toDegrees(pitchRad);
    }

    public float getWidth() {
        return xHalfSize * 2;
    }

    public float getHeight() {
        return yHalfSize * 2;
    }

    public float getLength() {
        return zHalfSize * 2;
    }

    // ============== 轴相关方法 ==============

    public Vec3[] getAxes() {
        return new Vec3[] { right, up, forward }; // 注意顺序：X, Y, Z
    }

    // ============== 几何计算 ==============

    public Vec3[] getVertices() {
        if (cachedVertices == null) {
            cachedVertices = calculateVertices();
        }
        return cachedVertices;
    }

    private Vec3[] calculateVertices() {
        Vec3[] vertices = new Vec3[8];
        // 三个方向的偏移量
        Vec3 rightOffset = right.scale(xHalfSize);      // X轴
        Vec3 upOffset = up.scale(yHalfSize);            // Y轴
        Vec3 forwardOffset = forward.scale(zHalfSize);  // Z轴
        int i = 0;
        for (int sx = -1; sx <= 1; sx += 2) {      // 宽度方向（右/左）
            for (int sy = -1; sy <= 1; sy += 2) {  // 高度方向（上/下）
                for (int sz = -1; sz <= 1; sz += 2) { // 长度方向（前/后）
                    Vec3 offset = rightOffset.scale(sx)
                            .add(upOffset.scale(sy))
                            .add(forwardOffset.scale(sz));
                    vertices[i++] = center.add(offset);
                }
            }
        }

        return vertices;
    }

    public AABB getBoundingAABB() {
        if (cachedAABB == null) {
            cachedAABB = calculateBoundingAABB();
        }
        return cachedAABB;
    }

    private AABB calculateBoundingAABB() {
        Vec3[] vertices = getVertices();

        double minX = vertices[0].x;
        double minY = vertices[0].y;
        double minZ = vertices[0].z;
        double maxX = minX;
        double maxY = minY;
        double maxZ = minZ;

        for (int i = 1; i < 8; i++) {
            Vec3 v = vertices[i];
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            minZ = Math.min(minZ, v.z);
            maxX = Math.max(maxX, v.x);
            maxY = Math.max(maxY, v.y);
            maxZ = Math.max(maxZ, v.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    /**
     * 从实体的视线方向创建OBB
     */
    public static OBB fromEntityView(Entity entity, float xHalfSize, float yHalfSize, float zHalfSize) {
        return new OBB(entity.getBoundingBox().getCenter(),xHalfSize, yHalfSize, zHalfSize,entity.getViewVector(1.0f), entity.getUpVector(1.0f));
    }
    // ============== 变换操作 ==============

    /**
     * 移动OBB（返回新对象）
     */
    public OBB move(Vec3 delta) {
        return new OBB(center.add(delta), xHalfSize, yHalfSize, zHalfSize, forward, up);
    }
    public OBB move(float x, float y, float z) {
        return new OBB(center.add(x,y,z), xHalfSize, yHalfSize, zHalfSize, forward, up);
    }
    public OBB move(double x, double y, double z) {
        return new OBB(center.add(x,y,z), xHalfSize, yHalfSize, zHalfSize, forward, up);
    }
    /**
     * 旋转OBB到指定方向（返回新对象）
     */
    public OBB rotateTo(Vec3 newForward, Vec3 newUp) {
        return new OBB(center, xHalfSize, yHalfSize, zHalfSize, newForward, newUp);
    }

    /**
     * 缩放OBB（返回新对象）
     */
    public OBB scale(float scale) {
        return new OBB(center, xHalfSize, yHalfSize, zHalfSize, forward, up);
    }
    /**
     * 设置新的中心点（返回新对象）
     */
    public OBB withCenter(Vec3 newCenter) {
        return new OBB(newCenter, xHalfSize, yHalfSize, zHalfSize, forward, up);
    }
    public OBB withXHalfSize(float newXHalfSize) {
        return new OBB(center, newXHalfSize, yHalfSize, zHalfSize, forward, up);
    }

    public OBB withYHalfSize(float newYHalfSize) {
        return new OBB(center, xHalfSize, newYHalfSize, zHalfSize, forward, up);
    }

    public OBB withZHalfSize(float newZHalfSize) {
        return new OBB(center, xHalfSize, yHalfSize, newZHalfSize, forward, up);
    }

    // ============== 碰撞检测 ==============

    public boolean intersects(AABB aabb) {
        // 快速拒绝：先检查AABB包围盒
        if (!getBoundingAABB().intersects(aabb)) {
            return false;
        }

        // 分离轴定理
        return checkSATIntersection(aabb);
    }

    private boolean checkSATIntersection(AABB aabb) {
        Vec3 aabbCenter = new Vec3(
                (aabb.minX + aabb.maxX) * 0.5,
                (aabb.minY + aabb.maxY) * 0.5,
                (aabb.minZ + aabb.maxZ) * 0.5
        );
        Vec3 aabbHalfSize = new Vec3(
                (aabb.maxX - aabb.minX) * 0.5,
                (aabb.maxY - aabb.minY) * 0.5,
                (aabb.maxZ - aabb.minZ) * 0.5
        );
        Vec3[] axes = getAxes();
        Vec3 separation = center.subtract(aabbCenter);

        // 检查OBB的3个轴
        for (int i = 0; i < 3; i++) {
            Vec3 axis = axes[i];
            // OBB在这个轴上的半尺寸（现在直接获取）
            double r1;
            if (i == 0) r1 = xHalfSize;      // X轴
            else if (i == 1) r1 = yHalfSize; // Y轴
            else r1 = zHalfSize;             // Z轴

            // AABB在这个轴上的投影半径
            double r2 = Math.abs(aabbHalfSize.x * axis.x) +
                    Math.abs(aabbHalfSize.y * axis.y) +
                    Math.abs(aabbHalfSize.z * axis.z);

            double distance = Math.abs(separation.dot(axis));
            if (distance > r1 + r2) {
                return false;
            }
        }

        // 检查世界坐标轴
        for (Direction.Axis worldAxis : Direction.Axis.values()) {
            double r1 = 0;
            // OBB在世界轴上的投影半径
            double[] halfSizes = {xHalfSize, yHalfSize, zHalfSize};
            Vec3[] obbAxes = {right, up, forward};

            for (int j = 0; j < 3; j++) {
                Vec3 axis = obbAxes[j];
                double axisComponent;

                if (worldAxis == Direction.Axis.X) {
                    axisComponent = axis.x;
                } else if (worldAxis == Direction.Axis.Y) {
                    axisComponent = axis.y;
                } else {
                    axisComponent = axis.z;
                }

                r1 += Math.abs(halfSizes[j] * axisComponent);
            }

            double r2 = worldAxis.choose(aabbHalfSize.x, aabbHalfSize.y, aabbHalfSize.z);
            double distance = Math.abs(worldAxis.choose(separation.x, separation.y, separation.z));

            if (distance > r1 + r2) {
                return false;
            }
        }

        return true;
    }


    /**
     * 沿着任意向量扩展OBB
     * 原理：将向量分解到OBB的三个轴上，分别扩展对应轴的尺寸
     */
    public OBB expandTowards(Vec3 extension) {
        if (extension.lengthSqr() < 1e-7) {
            return this;
        }
        // 1. 将扩展向量分解到OBB的三个轴上
        double extRight = extension.dot(right);     // X轴分量
        double extUp = extension.dot(up);           // Y轴分量
        double extForward = extension.dot(forward); // Z轴分量
        // 2. 计算新的半尺寸
        float newXHalfSize = xHalfSize + (float)Math.abs(extRight);
        float newYHalfSize = yHalfSize + (float)Math.abs(extUp);
        float newZHalfSize = zHalfSize + (float)Math.abs(extForward);
        // 3. 计算新的中心点
        Vec3 newCenter = center.add(extension.scale(0.5));
        // 4. 返回新OBB
        return new OBB(newCenter, newXHalfSize, newYHalfSize, newZHalfSize, forward, up);
    }

    /**
     * 沿着指定轴和距离扩展OBB
     * @param axis 扩展轴（0:右, 1:上, 2:前）
     * @param distance 扩展距离（正负表示方向）
     */
    public OBB expandAlongAxis(int axis, double distance) {
        if (Math.abs(distance) < 1e-7) {
            return this;
        }
        // 根据轴选择扩展方向
        Vec3 extension;
        switch (axis) {
            case 0 -> extension = right.scale(distance);     // 右方向
            case 1 -> extension = up.scale(distance);        // 上方向
            case 2 -> extension = forward.scale(distance);   // 前方向
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        }

        return expandTowards(extension);
    }

    /**
     * 向各个方向均匀扩展OBB（类似inflate）
     */
    public OBB inflate(double inflate) {
        if (Math.abs(inflate) < 1e-7) {
            return this;
        }
        // 创建扩展向量：在各个轴上均匀扩展，将扩展向量转换到世界坐标系
        return expandTowards(localToWorld(inflate,inflate,inflate));
    }

    /**
     * 分别向各个轴扩展不同距离
     */
    public OBB inflate(double x, double y, double z) {
        // 创建局部扩展向量， 转换到世界坐标系
        return expandTowards(localToWorld(x,y,z));
    }

    // ============== 坐标变换 ==============

    public Vec3 worldToLocal(Vec3 worldPoint) {
        Vec3 relative = worldPoint.subtract(center);
        return new Vec3(
                relative.dot(right),      // 局部X（右）
                relative.dot(up),         // 局部Y（上）
                relative.dot(forward)     // 局部Z（前）
        );
    }

    public Vec3 localToWorld(Vec3 local) {
        return center.add(right.scale(local.x)).add(up.scale(local.y)).add(forward.scale(local.z));   // Z（前）
    }
    public Vec3 localToWorld(double x,double y,double z) {
        return center.add(right.scale(x)).add(up.scale(y)).add(forward.scale(z));   // Z（前）
    }

    @Override
    public String toString() {
        return String.format("OBB{center=%s, size=(%.2f, %.2f, %.2f)}",
                center, getLength(), getHeight(), getWidth());
    }


}