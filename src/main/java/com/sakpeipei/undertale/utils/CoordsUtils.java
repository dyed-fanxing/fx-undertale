package com.sakpeipei.undertale.utils;

import com.sakpeipei.undertale.common.phys.LocalDirection;
import dev.kosmx.playerAnim.core.util.Vector3;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

/**
 * @author Sakpeipei
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
     * 坐标旋转
     */
    public static Vec3 transform(Vec3 vec, Matrix3f matrix) {
        Vector3f out = matrix.transform(vec.toVector3f());
        return new Vec3(out.x, out.y, out.z);
    }
    public static Vector3f transform3F(Vec3 vec, Matrix3f matrix) {
        return matrix.transform(vec.toVector3f());
    }
    /**
     * 坐标变换
     */
    public static double[] transformArray(double x,double y,double z, Matrix3f matrix) {
        Vector3f out = matrix.transform(new Vector3f((float) x, (float) y, (float) z));
        return new double[]{out.x, out.y, out.z};
    }
    public static Vec3 transform(double x,double y,double z, Matrix3f matrix) {
        return new Vec3(matrix.transform(new Vector3f((float) x, (float) y, (float) z)));
    }





    /**
     * 坐标变换
     */
    public static Vector3f transform(Vector3f vec, Quaternionf quaternionf) {
        Vector4f in = new Vector4f(vec.x, vec.y, vec.z, 1);
        Vector4f out = quaternionf.transform(in);
        return new Vector3f(out.x, out.y, out.z);
    }
    /**
     * 坐标旋转
     */
    public static Vec3 transform(Vec3 vec, Quaternionf quaternionf) {
        Vector3f out = quaternionf.transform(vec.toVector3f());
        return new Vec3(out.x, out.y, out.z);
    }
    public static Vector3f transform3F(Vec3 vec, Quaternionf quaternionf) {
        return quaternionf.transform(vec.toVector3f());
    }
    /**
     * 坐标变换
     */
    public static double[] transformArray(double x,double y,double z, Quaternionf quaternionf) {
        Vector3f out = quaternionf.transform(new Vector3f((float) x, (float) y, (float) z));
        return new double[]{out.x, out.y, out.z};
    }
    public static Vec3 transform(double x,double y,double z, Quaternionf quaternionf) {
        return new Vec3(quaternionf.transform(new Vector3f((float) x, (float) y, (float) z)));
    }


}
