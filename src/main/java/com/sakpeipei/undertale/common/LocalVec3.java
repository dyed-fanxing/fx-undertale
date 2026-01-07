package com.sakpeipei.undertale.common;

import com.sakpeipei.undertale.utils.CoordsUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.EnumSet;

/**
 * 局部向量类：输入为局部坐标，存储为转换后的全局坐标
 */
public class LocalVec3 extends Vec3 {

    // 预设矩阵（局部→全局）
    public static final Matrix4f MATRIX_DOWN = new Matrix4f();
    public static final Matrix4f MATRIX_UP = new Matrix4f().rotateZ(Mth.PI);
    public static final Matrix4f MATRIX_NORTH = new Matrix4f().rotateX(-Mth.PI/2);
    public static final Matrix4f MATRIX_SOUTH = new Matrix4f().rotateX(Mth.PI/2);
    public static final Matrix4f MATRIX_EAST = new Matrix4f().rotateZ( Mth.PI/2);
    public static final Matrix4f MATRIX_WEST = new Matrix4f().rotateZ(-Mth.PI/2);

    // 预设渲染矩阵
    public static final Matrix3f RENDER_DOWN = CoordsUtils.get3x3(MATRIX_DOWN);
    public static final Matrix3f RENDER_UP = CoordsUtils.get3x3(MATRIX_UP);
    public static final Matrix3f RENDER_NORTH = CoordsUtils.get3x3(MATRIX_NORTH);
    public static final Matrix3f RENDER_SOUTH = CoordsUtils.get3x3(MATRIX_SOUTH);
    public static final Matrix3f RENDER_EAST = CoordsUtils.get3x3(MATRIX_EAST);
    public static final Matrix3f RENDER_WEST = CoordsUtils.get3x3(MATRIX_WEST);

    private final Vec3 gravity;
    private final Matrix4f matrix4f;
    private final Matrix3f matrix3f;

    /**
     * 私有构造函数
     */
    private LocalVec3(double x, double y, double z, Vec3 gravity, Matrix4f matrix) {
        super(x, y, z);
        this.gravity = gravity;
        this.matrix4f = matrix;
        this.matrix3f = CoordsUtils.get3x3(matrix);
    }
    private LocalVec3(Vec3 gravity, Matrix4f matrix,Vec3 vec3) {
        super(vec3.x, vec3.y, vec3.z);
        this.gravity = gravity;
        this.matrix4f = matrix;
        this.matrix3f = CoordsUtils.get3x3(matrix);
    }
    /**
     * 工厂方法：从局部坐标创建
     */
    public static LocalVec3 fromLocal(LocalDirection direction, Vec3 lookAngle, Vec3 localCoords) {
        Vec3 gravity = CoordsUtils.getGravity(direction, lookAngle);
        Matrix4f matrix = CoordsUtils.buildMatrix4f(gravity);
        Vec3 worldCoords = CoordsUtils.transform(localCoords, matrix);
        return new LocalVec3(gravity, matrix,worldCoords);
    }


    /**
     * 获取重力方向
     */
    public Vec3 getGravity() {
        return gravity;
    }

    /**
     * 获取渲染矩阵
     */
    public Matrix3f getRenderMatrix() {
        return matrix3f;
    }

    /**
     * 获取转换矩阵
     */
    public Matrix4f getMatrix() {
        return matrix4f;
    }

    /**
     * 将当前向量转回局部坐标
     */
    public Vec3 toLocal() {
        Matrix4f invMatrix = new Matrix4f(this.matrix4f);
        invMatrix.invert();
        return CoordsUtils.transform(this, invMatrix);
    }



    @Override
    public @NotNull Vec3 vectorTo(@NotNull Vec3 p_82506_) {
        return super.vectorTo(CoordsUtils.transform(p_82506_, matrix4f));
    }

    @Override
    public double dot(@NotNull Vec3 p_82527_) {
        return super.dot(CoordsUtils.transform(p_82527_, matrix4f));
    }

    @Override
    public @NotNull Vec3 cross(@NotNull Vec3 p_82538_) {
        return super.cross(CoordsUtils.transform(p_82538_, matrix4f));
    }

    @Override
    public @NotNull Vec3 subtract(@NotNull Vec3 p_82547_) {
        return super.subtract(CoordsUtils.transform(p_82547_, matrix4f));
    }

    @Override
    public @NotNull Vec3 add(@NotNull Vec3 p_82550_) {
        return super.add(CoordsUtils.transform(p_82550_, matrix4f));
    }

    @Override
    public boolean closerThan(@NotNull Position p_82510_, double p_82511_) {
        if (p_82510_ instanceof Vec3 vec) {
            return super.closerThan(CoordsUtils.transform(vec, matrix4f), p_82511_);
        }
        return super.closerThan(p_82510_, p_82511_);
    }

    @Override
    public double distanceTo(@NotNull Vec3 p_82555_) {
        return super.distanceTo(CoordsUtils.transform(p_82555_, matrix4f));
    }

    @Override
    public double distanceToSqr(@NotNull Vec3 p_82558_) {
        return super.distanceToSqr(CoordsUtils.transform(p_82558_, matrix4f));
    }

    @Override
    public boolean closerThan(@NotNull Vec3 p_312866_, double p_312928_, double p_312788_) {
        return super.closerThan(CoordsUtils.transform(p_312866_, matrix4f), p_312928_, p_312788_);
    }

    @Override
    public @NotNull Vec3 multiply(@NotNull Vec3 p_82560_) {
        return super.multiply(CoordsUtils.transform(p_82560_, matrix4f));
    }

    @Override
    public @NotNull Vec3 lerp(@NotNull Vec3 p_165922_, double p_165923_) {
        return super.lerp(CoordsUtils.transform(p_165922_, matrix4f), p_165923_);
    }

    @Override
    public @NotNull Vec3 align(@NotNull EnumSet<Direction.Axis> p_82518_) {
        return super.align(p_82518_);
    }

    @Override
    public @NotNull Vec3 relative(@NotNull Direction p_231076_, double p_231077_) {
        // 将世界坐标系的方向向量转换到当前重力坐标系
        Vec3 worldDir = new Vec3(
                p_231076_.getNormal().getX(),
                p_231076_.getNormal().getY(),
                p_231076_.getNormal().getZ()
        );
        Vec3 localDir = CoordsUtils.transform(worldDir, matrix4f);

        return new Vec3(
                this.x + p_231077_ * localDir.x,
                this.y + p_231077_ * localDir.y,
                this.z + p_231077_ * localDir.z
        );
    }


    @Override
    public @NotNull Vec3 xRot(float angle) {
        return rotateAroundAxis(matrix4f.m00(), matrix4f.m10(), matrix4f.m20(), angle);
    }

    @Override
    public @NotNull Vec3 yRot(float angle) {
        return rotateAroundAxis(matrix4f.m01(), matrix4f.m11(), matrix4f.m21(), angle);
    }

    @Override
    public @NotNull Vec3 zRot(float angle) {
        return rotateAroundAxis(matrix4f.m02(), matrix4f.m12(), matrix4f.m22(), angle);
    }
    /**
     * 绕任意轴旋转（罗德里格斯公式）
     */
    private Vec3 rotateAroundAxis(float ax, float ay, float az, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double oneMinusCos = 1 - cos;

        // 旋转矩阵
        double m00 = cos + ax * ax * oneMinusCos;
        double m01 = ax * ay * oneMinusCos - az * sin;
        double m02 = ax * az * oneMinusCos + ay * sin;

        double m10 = ay * ax * oneMinusCos + az * sin;
        double m11 = cos + ay * ay * oneMinusCos;
        double m12 = ay * az * oneMinusCos - ax * sin;

        double m20 = az * ax * oneMinusCos - ay * sin;
        double m21 = az * ay * oneMinusCos + ax * sin;
        double m22 = cos + az * az * oneMinusCos;

        return new Vec3(
                this.x * m00 + this.y * m01 + this.z * m02,
                this.x * m10 + this.y * m11 + this.z * m12,
                this.x * m20 + this.y * m21 + this.z * m22
        );
    }
}