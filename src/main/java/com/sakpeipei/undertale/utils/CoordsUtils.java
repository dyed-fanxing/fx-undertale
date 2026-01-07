package com.sakpeipei.undertale.utils;

import com.sakpeipei.undertale.common.LocalDirection;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * @author yujinbao
 * @since 2026/1/7 15:40
 */
public class CoordsUtils {
    /**
     * 坐标变换
     */
    public static Vector3f transform(Vector3f vec, Matrix4f matrix) {
        Vector4f in = new Vector4f(vec.x, vec.y, vec.z, 1);
        Vector4f out = matrix.transform(in);
        return new Vector3f(out.x, out.y, out.z);
    }
    /**
     * 坐标变换
     */
    public static Vec3 transform(Vec3 vec, Matrix4f matrix) {
        Vector4f in = new Vector4f((float)vec.x, (float)vec.y, (float)vec.z, 1);
        Vector4f out = matrix.transform(in);
        return new Vec3(out.x, out.y, out.z);
    }
    /**
     * 根据全局重力方向构建 旋转矩阵
     */
    public static Matrix4f buildMatrix4f(Vec3 gravity) {
        Vec3 up = gravity.scale(-1);
        // 总是先用(1,0,0)，计算叉乘
        Vec3 temp = new Vec3(1, 0, 0);
        Vec3 right = temp.cross(up);
        // 如果叉乘结果是零向量（平行），换一个
        if (right.lengthSqr() < 1e-12) {
            temp = new Vec3(0, 1, 0);
            right = temp.cross(up);
        }
        right = right.normalize();
        Vec3 front = up.cross(right).normalize();
        return new Matrix4f(
                (float)right.x, (float)right.y, (float)right.z, 0,
                (float)up.x, (float)up.y, (float)up.z, 0,
                (float)front.x, (float)front.y, (float)front.z, 0,
                0, 0, 0, 1
        );
    }
    /**
     * 获取全局重力向量
     * @param direction 局部重力方向
     * @param lookAngle 全局视线方向
     */
    public static Vec3 getGravity(LocalDirection direction, Vec3 lookAngle) {
        Vec3 forward = lookAngle.normalize();
        Vec3 local = direction.getVec3();
        return switch (direction){
            case UP,DOWN -> direction.getVec3();
            case FRONT -> forward;
            case BACK -> forward.scale(-1);
            case LEFT,RIGHT -> {
                // 视线水平投影
                Vec3 horizontal = new Vec3(forward.x, 0, forward.z).normalize();
                if (horizontal.lengthSqr() < 0.0001) {
                    horizontal = new Vec3(0, 0, 1);
                }
                // 右方向 = 上 × 水平视线
                Vec3 right = new Vec3(0, 1, 0).cross(horizontal).normalize();
                yield  right.scale(local.x);
            }
        };
    }

    /**
     * 提取3x3矩阵
     */
    public static Matrix3f get3x3(Matrix4f matrix) {
        Matrix3f result = new Matrix3f();
        matrix.get3x3(result);
        return result;
    }
}
