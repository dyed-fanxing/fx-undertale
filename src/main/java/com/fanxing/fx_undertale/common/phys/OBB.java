package com.fanxing.fx_undertale.common.phys;

import com.fanxing.fx_undertale.entity.capability.Rollable;
import com.fanxing.fx_undertale.utils.RotUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 标准的 OBB（定向包围盒）实现，绕几何中心旋转
 * 支持 3D 旋转、平移、缩放
 */
public class OBB {
    private static final Logger log = LoggerFactory.getLogger(OBB.class);
    // 核心属性
    public final Vec3 center;        // 中心点
    public final float xHalfSize;    // 右/左半宽 (对应 right 轴)
    public final float yHalfSize;    // 上/下半高 (对应 up 轴)
    public final float zHalfSize;    // 前/后半长 (对应 forward 轴)

    // 三个正交轴（已归一化）
    public final Vec3 forward;       // 前方向 (Local Z)
    public final Vec3 up;            // 上方向 (Local Y)
    public final Vec3 right;         // 右方向 (Local X)

    // 缓存 (变换后必须重置为 null)
    private Vec3[] cachedVertices;
    private AABB cachedAABB;

    // ============== 构造方法 ==============

    /**
     * 从中心、尺寸和前方向创建 OBB（自动计算 up 和 right）
     */
    public OBB(Vec3 center, float width, float height, Entity entity) {
        this(center, width * 0.5f, height * 0.5f, width * 0.5f, entity.getViewVector(1.0f), entity.getUpVector(1.0f));
    }

    /**
     * 从中心、尺寸和前方向创建 OBB（自动计算 up 和 right）
     */
    public OBB(Vec3 center, float xHalfSize, float yHalfSize, float zHalfSize, Vec3 forward) {
        this(center, xHalfSize, yHalfSize, zHalfSize, forward, calculateUpFromForward(forward));
    }

    /**
     * @param yaw   航偏
     * @param pitch 仰附
     */
    public OBB(Vec3 center, float xHalfSize, float yHalfSize, float zHalfSize, float yaw, float pitch) {
        this(center, xHalfSize, yHalfSize, zHalfSize, calculateForwardFromYawPitch(yaw, pitch));
    }

    /**
     * @param center    几何中心点
     * @param xHalfSize 局部 x 轴 (宽度) 半径
     * @param yHalfSize 局部 y 轴 (高度) 半径
     * @param zHalfSize 局部 z 轴 (长度) 半径
     * @param forward   正交的前方向世界向量
     * @param up        正交的上方向世界向量
     */
    public OBB(Vec3 center, float xHalfSize, float yHalfSize, float zHalfSize, Vec3 forward, Vec3 up) {
        this.center = center;
        this.xHalfSize = xHalfSize;
        this.yHalfSize = yHalfSize;
        this.zHalfSize = zHalfSize;

        // 直接使用传入的 forward 和 up，只做归一化
        this.forward = forward.normalize();
        this.up = up.normalize();
        this.right = this.forward.cross(this.up).normalize();

        // 重置缓存
        this.cachedVertices = null;
        this.cachedAABB = null;
    }

    /**
     * @param center    几何中心点
     * @param xHalfSize 局部x轴(宽度)半径
     * @param yHalfSize 局部y轴(高度)半径
     * @param zHalfSize 局部z轴(长度)半径
     * @param forward   必须为单位向量
     * @param up        必须为单位向量，且垂直于 forward
     * @param right     必须为单位向量，且垂直于 forward 和 up (构成右手系)
     */
    public OBB(Vec3 center, float xHalfSize, float yHalfSize, float zHalfSize, Vec3 forward, Vec3 up, Vec3 right) {
        this.center = center;
        this.xHalfSize = xHalfSize;
        this.yHalfSize = yHalfSize;
        this.zHalfSize = zHalfSize;
        this.forward = forward;
        this.up = up;
        this.right = right;
        // 重置缓存
        this.cachedVertices = null;
        this.cachedAABB = null;
    }

    // ============== 辅助静态方法 ==============
    private static Vec3 calculateUpFromForward(Vec3 forward) {
        if (Math.abs(forward.y) > 0.99) {
            return new Vec3(1, 0, 0);
        }
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(worldUp).normalize();
        return right.cross(forward).normalize();
    }

    private static Vec3 calculateForwardFromYawPitch(float yaw, float pitch) {
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch).normalize();
    }

    /**
     * key 构建以实体几何中心为旋转中心的OBB，最终的OBB的几何中心就是position
     */
    public static OBB fromCenter(Entity entity, float partialTick) {
        float halfWidth = entity.getBbWidth() * 0.5f;
        float halfHeight = entity.getBbHeight() * 0.5f;
        return new OBB(entity.position(), halfWidth, halfHeight, halfWidth,
                entity.getViewVector(partialTick), entity.getUpVector(partialTick));
    }
    public static OBB fromCenter(Entity entity) {
        return fromCenter(entity, 1.0f);
    }

    /**
     * key 构建以实体脚底(position)为旋转中心的OBB，最终的OBB的几何中心是绕position旋转得到的
     *  !!! 在做旋转碰撞检测时，需要传入position作为旋转点pivot，而不是OBB自身的center
     */
    public static OBB fromFoot(Entity entity, float partialTick) {
        float halfWidth = entity.getBbWidth() * 0.5f;
        float halfHeight = entity.getBbHeight() * 0.5f;
        Vec3 up = entity.getUpVector(partialTick);
        return new OBB(entity.position().add(up.scale(halfHeight)), halfWidth, halfHeight, halfWidth,
                entity.getViewVector(partialTick), up);
    }
    public static OBB fromFoot(Entity entity) {
        return fromFoot(entity,1.0f);
    }


    // ============== 基本属性 ==============
    public Vec3 getCenter() {
        return center;
    }

    public float getYaw() {
        double yawRad = Math.atan2(-forward.x, forward.z);
        return (float) Math.toDegrees(yawRad);
    }

    public float getPitch() {
        double forwardLength = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
        double pitchRad = Math.atan2(-forward.y, forwardLength);
        return (float) Math.toDegrees(pitchRad);
    }

    public float getWidth() {
        return xHalfSize * 2.0f;
    }

    public float getHeight() {
        return yHalfSize * 2.0f;
    }

    public float getLength() {
        return zHalfSize * 2.0f;
    }

    public Vec3[] getAxes() {
        return new Vec3[]{right, up, forward};
    }

    // ============== 几何计算 (带缓存) ==============
    public Vec3[] getVertices() {
        if (cachedVertices == null) {
            cachedVertices = calculateVertices();
        }
        return cachedVertices;
    }

    public AABB getBoundingAABB() {
        if (cachedAABB == null) {
            cachedAABB = calculateBoundingAABB();
        }
        return cachedAABB;
    }

    private Vec3[] calculateVertices() {
        Vec3[] v = new Vec3[8];
        // 预计算轴向量 * 半尺寸
        Vec3 rx = right.scale(xHalfSize);
        Vec3 uy = up.scale(yHalfSize);
        Vec3 fz = forward.scale(zHalfSize);
        int i = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    // 直接加减，减少对象创建 (Vec3 是不可变的，这里无法避免 new，但逻辑最简)
                    v[i++] = center.add(rx.scale(sx)).add(uy.scale(sy)).add(fz.scale(sz));
                }
            }
        }
        return v;
    }

    private AABB calculateBoundingAABB() {
        Vec3[] v = getVertices();
        double minX = v[0].x, maxX = v[0].x;
        double minY = v[0].y, maxY = v[0].y;
        double minZ = v[0].z, maxZ = v[0].z;

        for (int i = 1; i < 8; i++) {
            Vec3 p = v[i];
            if (p.x < minX) minX = p.x;
            else if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            else if (p.y > maxY) maxY = p.y;
            if (p.z < minZ) minZ = p.z;
            else if (p.z > maxZ) maxZ = p.z;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    /**
     * 根据局部坐标（范围 [-1, 1] 对应半长）返回世界坐标点。
     * @param localX 沿 right 轴的比例，范围 [-1, 1]，-1 表示最左，1 表示最右
     * @param localY 沿 up 轴的比例，范围 [-1, 1]，-1 表示最下，1 表示最上
     * @param localZ 沿 forward 轴的比例，范围 [-1, 1]，-1 表示最后，1 表示最前
     * @return 世界坐标点
     */
    public Vec3 getPoint(float localX, float localY, float localZ) {
        return center.add(right.scale(localX * xHalfSize))
                .add(up.scale(localY * yHalfSize))
                .add(forward.scale(localZ * zHalfSize));
    }

    /**
     * 根据局部高度（绝对距离，沿 up 轴）返回世界坐标点。
     * @param height 从中心沿 up 轴偏移的距离，正值向上，负值向下
     * @return 世界坐标点
     */
    public Vec3 getPointAtLocalHeight(float height) {
        return center.add(up.scale(height));
    }
    /**
     * 返回一个位于指定局部高度的新 OBB，方向与原 OBB 相同。
     * @param height  相对中心点位置 沿 up 轴的局部高度（从原中心向上为正，向下为负），单位为世界坐标距离
     * @param halfThick 半厚
     * @return 新的 OBB 对象
     */
    public OBB getSubOBBAtLocalHeight(float height, float halfThick) {
        Vec3 newCenter = center.add(up.scale(height));
        return new OBB(newCenter, xHalfSize, halfThick, zHalfSize, forward, up);
    }

    /**
     * 获取局部高度的切片 OBB（厚度0.01）。
     * 常用于渲染截面轮廓，如眼睛高度处。
     * @param height 相对中心点位置 沿 up 轴的局部高度
     * @return 厚度为 0.01f 的扁平 OBB
     */
    public OBB getSliceAtLocalHeight(float height) {
        return getSubOBBAtLocalHeight(height, 0.005f);  // 总厚度 0.01
    }
    /**
     * 获取相对于实体脚部的切片 OBB （厚度0.01）。
     * @param height 从脚底向上的绝对高度（如 entity.getEyeHeight()）
     * @return 新 OBB
     */
    public OBB getSliceRelativeToEntityFeet(float height) {
        // 脚底 = center - up * yHalfSize
        // 绝对高度点 = 脚底 + up * absoluteHeightFromFoot
        // 所以相对中心的偏移 = (absoluteHeightFromFoot - yHalfSize)
        return getSubOBBAtLocalHeight(height - yHalfSize, 0.005f);
    }

    // ============== 变换操作 (Immutable) ==============

    /**
     * 移动 OBB（返回新对象）
     * 【修复】新对象会自动重置缓存
     */
    public OBB move(Vec3 delta) {
        return new OBB(center.add(delta), xHalfSize, yHalfSize, zHalfSize, forward, up);
    }

    public OBB move(double x, double y, double z) {
        return new OBB(center.add(x, y, z), xHalfSize, yHalfSize, zHalfSize, forward, up);
    }

    /**
     * 旋转 OBB 到指定方向（返回新对象）
     */
    public OBB rotateTo(Vec3 newForward, Vec3 newUp) {
        return new OBB(center, xHalfSize, yHalfSize, zHalfSize, newForward, newUp);
    }

    /**
     * 绕指定轴和 pivot 点旋转 OBB (全 float 版本)
     */
    public OBB rotateAround(float radian, Vec3 axis, Vec3 pivot) {
        if (Mth.abs(radian) < Mth.EPSILON) return this;
        float ax = (float) axis.x;
        float ay = (float) axis.y;
        float az = (float) axis.z;
        // 使用 JOML float 版本
        Quaternionf quat = new Quaternionf().rotationAxis(radian, ax, ay, az);
        // 1. 计算新中心
        Vector3f centerRel = new Vector3f(
                (float) (this.center.x - pivot.x),
                (float) (this.center.y - pivot.y),
                (float) (this.center.z - pivot.z)
        );
        centerRel.rotate(quat);
        Vec3 newCenter = new Vec3(
                centerRel.x + pivot.x,
                centerRel.y + pivot.y,
                centerRel.z + pivot.z
        );
        // 2. 计算新轴向
        Vector3f jomlF = new Vector3f((float) this.forward.x, (float) this.forward.y, (float) this.forward.z);
        jomlF.rotate(quat);
        Vec3 newF = new Vec3(jomlF.x, jomlF.y, jomlF.z).normalize();
        Vector3f jomlU = new Vector3f((float) this.up.x, (float) this.up.y, (float) this.up.z);
        jomlU.rotate(quat);
        Vec3 newU = new Vec3(jomlU.x, jomlU.y, jomlU.z);
        // 3. 重新正交化
        Vec3 r = newF.cross(newU).normalize();
        Vec3 u = r.cross(newF).normalize();
        return new OBB(newCenter, this.xHalfSize, this.yHalfSize, this.zHalfSize, newF, u);
    }

    /**
     * 使用角速度向量进行旋转（使用 JOML 的 integrate 方法，不显式分解角度和轴）
     * 注意：angularVelocity 向量必须使用弧度制
     * @param angularVelocity 已按时间缩放的角速度向量（方向=旋转轴，大小=旋转角度，弧度）
     * @param pivot 旋转锚点
     * @return 旋转后的 OBB
     */
    public OBB rotateByAngularVelocity(Vector3f angularVelocity, Vec3 pivot) {
        // 使用 JOML 的 integrate 方法，时间设为 1（因为角速度向量已经按时间缩放）
        Quaternionf qRotation = new Quaternionf().integrate(1.0f, angularVelocity.x, angularVelocity.y, angularVelocity.z);
        // 1. 计算新中心（相对于 pivot 旋转）
        Vector3f centerRel = new Vector3f(
                (float) (this.center.x - pivot.x),
                (float) (this.center.y - pivot.y),
                (float) (this.center.z - pivot.z)
        );
        centerRel.rotate(qRotation);
        Vec3 newCenter = new Vec3(
                centerRel.x + pivot.x,
                centerRel.y + pivot.y,
                centerRel.z + pivot.z
        );
        // 2. 计算新轴向
        Vector3f jomlF = new Vector3f((float) this.forward.x, (float) this.forward.y, (float) this.forward.z);
        jomlF.rotate(qRotation);
        Vec3 newF = new Vec3(jomlF.x, jomlF.y, jomlF.z).normalize();
        Vector3f jomlU = new Vector3f((float) this.up.x, (float) this.up.y, (float) this.up.z);
        jomlU.rotate(qRotation);
        Vec3 newU = new Vec3(jomlU.x, jomlU.y, jomlU.z);
        // 3. 重新正交化
        Vec3 r = newF.cross(newU).normalize();
        Vec3 u = r.cross(newF).normalize();
        return new OBB(newCenter, this.xHalfSize, this.yHalfSize, this.zHalfSize, newF, u);
    }

    public OBB expandTowards(Vec3 extension) {
        if (extension.lengthSqr() < 1e-7) return this;
        double er = extension.dot(right);
        double eu = extension.dot(up);
        double ef = extension.dot(forward);
        return new OBB(
                center.add(extension.scale(0.5)),
                xHalfSize + (float) Math.abs(er),
                yHalfSize + (float) Math.abs(eu),
                zHalfSize + (float) Math.abs(ef),
                forward, up
        );
    }

    public OBB inflate(double amount) {
        return new OBB(center,
                xHalfSize + (float) amount,
                yHalfSize + (float) amount,
                zHalfSize + (float) amount,
                forward, up);
    }

    public OBB scale(float scale) {
        if (scale <= 0) return this; // 防御性
        return new OBB(center, xHalfSize * scale, yHalfSize * scale, zHalfSize * scale, forward, up);
    }

    public OBB scale(float xScale, float yScale, float zScale) {
        return new OBB(center, xHalfSize * xScale, yHalfSize * yScale, zHalfSize * zScale, forward, up);
    }

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

    public OBB copy() {
        return new OBB(center, xHalfSize, yHalfSize, zHalfSize, forward, up);
    }


    // ============== 碰撞检测 ==============
    public boolean intersects(AABB target) {
        // 1. 快速拒绝：AABB 包围盒相交
        if (!getBoundingAABB().intersects(target)) return false;
        // 2. SAT 精确检测
        return checkSAT(target);
    }
    public boolean checkSAT(AABB target) {
        Vec3 aabbCenter = new Vec3((target.minX + target.maxX) * 0.5, (target.minY + target.maxY) * 0.5, (target.minZ + target.maxZ) * 0.5);
        Vec3 aabbHalf = new Vec3((target.maxX - target.minX) * 0.5, (target.maxY - target.minY) * 0.5, (target.maxZ - target.minZ) * 0.5);
        Vec3 d = center.subtract(aabbCenter); // 中心差向量
        Vec3[] axes = getAxes(); // [Right, Up, Forward]
        float[] sizes = {xHalfSize, yHalfSize, zHalfSize};
        // --- 检测 OBB 的 3 个局部轴 ---
        for (int i = 0; i < 3; i++) {
            Vec3 axis = axes[i];
            // AABB 在 OBB 轴上的投影半径 = sum(h_i * |axis_i|)
            double r2 = aabbHalf.x * Math.abs(axis.x) + aabbHalf.y * Math.abs(axis.y) + aabbHalf.z * Math.abs(axis.z);
            double dist = Math.abs(d.dot(axis));
            if (dist > sizes[i] + r2) return false;
        }
        // --- 检测世界坐标系的 3 个轴 (X, Y, Z) ---
        // 这是 OBB vs AABB 的特化优化，无需测试 9 个叉乘轴
        // Axis X (1,0,0)
        double r1_x = Math.abs(xHalfSize * right.x) + Math.abs(yHalfSize * up.x) + Math.abs(zHalfSize * forward.x);
        if (Math.abs(d.x) > r1_x + aabbHalf.x) return false;
        // Axis Y (0,1,0)
        double r1_y = Math.abs(xHalfSize * right.y) + Math.abs(yHalfSize * up.y) + Math.abs(zHalfSize * forward.y);
        if (Math.abs(d.y) > r1_y + aabbHalf.y) return false;
        // Axis Z (0,0,1)
        double r1_z = Math.abs(xHalfSize * right.z) + Math.abs(yHalfSize * up.z) + Math.abs(zHalfSize * forward.z);
        return !(Math.abs(d.z) > r1_z + aabbHalf.z);
    }
    /**
     * 通用碰撞检测入口：与 OBB 检测
     */
    public boolean intersects(OBB target) {
        if (!getBoundingAABB().intersects(target.getBoundingAABB())) return false;
        return checkSAT(target);
    }
    /**
     * SAT 检测：OBB vs OBB
     * 测试轴：OBB A的3个轴 + OBB B的3个轴 + 9个叉乘轴 (共15条)
     */
    private boolean checkSAT(OBB target) {
        Vec3 d = this.center.subtract(target.center);

        Vec3[] axesA = this.getAxes();
        float[] sizesA = {this.xHalfSize, this.yHalfSize, this.zHalfSize};

        Vec3[] axesB = target.getAxes();
        float[] sizesB = {target.xHalfSize, target.yHalfSize, target.zHalfSize};

        // 1. 测试 A 的 3 个轴
        for (int i = 0; i < 3; i++) {
            Vec3 axis = axesA[i];
            float r1 = sizesA[i];
            // 投影 B 到 A 的轴上
            float r2 = 0;
            for (int j = 0; j < 3; j++) {
                r2 += sizesB[j] * Math.abs((float) axis.dot(axesB[j]));
            }
            float dist = Math.abs((float) d.dot(axis));
            if (dist > r1 + r2) return false;
        }

        // 2. 测试 B 的 3 个轴
        for (int i = 0; i < 3; i++) {
            Vec3 axis = axesB[i];
            float r2 = sizesB[i];
            // 投影 A 到 B 的轴上
            float r1 = 0;
            for (int j = 0; j < 3; j++) {
                r1 += sizesA[j] * Math.abs((float) axis.dot(axesA[j]));
            }
            float dist = Math.abs((float) d.dot(axis));
            if (dist > r1 + r2) return false;
        }

        // 3. 测试 9 个叉乘轴 (A_i x B_j)
        // 如果叉乘结果接近零向量（轴平行），则跳过该轴（因为前面的测试已经覆盖了）
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vec3 axis = axesA[i].cross(axesB[j]);
                float lenSq = (float) axis.lengthSqr();
                if (lenSq < 1e-6f) continue; // 平行，跳过

                Vec3 axisNorm = axis.normalize();
                float r1 = 0, r2 = 0;

                // 投影 A
                for (int k = 0; k < 3; k++) {
                    r1 += sizesA[k] * Math.abs((float) axisNorm.dot(axesA[k]));
                }
                // 投影 B
                for (int k = 0; k < 3; k++) {
                    r2 += sizesB[k] * Math.abs((float) axisNorm.dot(axesB[k]));
                }

                float dist = Math.abs((float) d.dot(axisNorm));
                if (dist > r1 + r2) return false;
            }
        }

        return true; // 所有轴都重叠，发生碰撞
    }

    @Override
    public String toString() {
        return String.format("OBB{center=%s, size=(%.2f, %.2f, %.2f), pitch=%.2f,yaw=%.2f}",
                center, getWidth(), getHeight(), getLength(), getPitch(), getYaw());
    }
}