package com.fanxing.fx_undertale.entity.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.fanxing.fx_undertale.common.phys.LocalDirection;
import com.fanxing.fx_undertale.registry.AttachmentTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sakpeipei
 * @since 2025/11/14 10:17
 * 重力方向数据 - 存储实体的重力状态
 */
public class Gravity {
    private static final Logger log = LoggerFactory.getLogger(Gravity.class);

    // Codec用于序列化
    public static final Codec<Gravity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Direction.CODEC.fieldOf("gravity").forGetter(data -> data.gravity)
    ).apply(instance, Gravity::new));

    // 控制方向
    private Direction gravity = Direction.DOWN;
    private Quaternionf localToWorld = new Quaternionf();
    private Quaternionf worldToLocal = new Quaternionf();


    public Gravity() {
    }

    public Gravity(Direction gravity) {
        this.gravity = gravity;
        this.localToWorld = getRotation(gravity);
        this.worldToLocal = localToWorld.invert(new Quaternionf());
    }

    /**
     * 对目标实体应用攻击者的相对重力
     */
    public static Gravity applyRelativeGravity(Entity attacker, Entity target, LocalDirection localGravity) {
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
    public static Gravity applyGravity(Entity target, Direction gravity) {
        Gravity gravityData = new Gravity(gravity);
        Gravity oldGravity = target.getData(AttachmentTypes.GRAVITY);
        target.setData(AttachmentTypes.GRAVITY, gravityData);
        Vec3 localPos = switch (oldGravity.getGravity()) {
            case DOWN -> target.position();
            case UP -> target.position().add(0, -target.getBbHeight(), 0);
            case EAST -> target.position().add(-target.getBbHeight() * 0.5f, -target.getBbWidth() * 0.5f, 0);
            case WEST -> target.position().add(target.getBbHeight() * 0.5f, -target.getBbWidth() * 0.5f, 0);
            case SOUTH -> target.position().add(0, -target.getBbWidth() * 0.5f, -target.getBbHeight() * 0.5f);
            case NORTH -> target.position().add(0, -target.getBbWidth() * 0.5f, target.getBbHeight() * 0.5f);
        };
        switch (gravityData.gravity) {
            case DOWN -> target.setPos(localPos);
            case UP -> target.setPos(localPos.add(0, target.getBbHeight(), 0));
            case EAST -> target.setPos(localPos.add(target.getBbHeight() * 0.5f, target.getBbWidth() * 0.5f, 0));
            case WEST -> target.setPos(localPos.add(-target.getBbHeight() * 0.5f, target.getBbWidth() * 0.5f, 0));
            case SOUTH -> target.setPos(localPos.add(0, target.getBbWidth() * 0.5f, target.getBbHeight() * 0.5f));
            case NORTH -> target.setPos(localPos.add(0, target.getBbWidth() * 0.5f, -target.getBbHeight() * 0.5f));
        }
//        log.debug("gravity：{},target之前世界坐标系的位置：{}，之后世界坐标系的位置：{}", gravity, localPos, target.position());
        return gravityData;
    }


    public Vec3 localToWorld(Vec3 vec) {
        Vector3f out = this.localToWorld.transform(vec.toVector3f());
        return new Vec3(out.x, out.y, out.z);
    }

    public Vec3 localToWorld(double x, double y, double z) {
        Vector3f out = this.localToWorld.transform(new Vector3f((float) x, (float) y, (float) z));
        return new Vec3(out.x, out.y, out.z);
    }

    public Vector3f localToWorld(Vector3f vector3f) {
        return this.localToWorld.transform(vector3f);
    }

    public Vector3f localToWorld(float x, float y, float z) {
        return this.localToWorld.transform(new Vector3f(x, y, z));
    }


    public Vec3 worldToLocal(double x, double y, double z) {
        Vector3f out = this.worldToLocal.transform(new Vector3f((float) x, (float) y, (float) z));
        return new Vec3(out.x, out.y, out.z);
    }

    public Vec3 worldToLocal(Vec3 vec) {
        Vector3f out = this.worldToLocal.transform(vec.toVector3f());
        return new Vec3(out.x, out.y, out.z);
    }

    public Vector3f worldToLocal(Vector3f vector3f) {
        return this.worldToLocal.transform(vector3f);
    }


    public Direction getGravity() {
        return gravity;
    }

    /**
     * 获取局部坐标系的右方向在世界坐标系中的向量
     * 局部右方向 = (1,0,0)，转换到世界坐标系
     */
    public Vector3f getRight() {
        // 局部右方向 (1,0,0) 转换到世界坐标系
        return localToWorld.transform(new Vector3f(1, 0, 0));
    }

    /**
     * 获取局部坐标系的上方向在世界坐标系中的向量
     * 局部上方向 = (0,1,0)，转换到世界坐标系
     */
    public Vector3f getUp() {
        return localToWorld.transform(new Vector3f(0, 1, 0));
    }

    /**
     * 获取局部坐标系的前方向在世界坐标系中的向量
     * 局部前方向 = (0,0,1)，转换到世界坐标系
     */
    public Vector3f getForward() {
        return localToWorld.transform(new Vector3f(0, 0, 1));
    }


    public Quaternionf getLocalToWorld() {
        return localToWorld;

    }

    public Quaternionf getWorldToLocal() {
        return worldToLocal;
    }

    @Override
    public String toString() {
        return "GravityData{" +
                "gravity=" + gravity +
                ",gravityNormal=" + gravity.getNormal() +
                ",forward=" + getForward() +
                ",up=" + getUp() +
                ",right=" + getRight() +
                '}';
    }


    public static Quaternionf getRotation(Direction gravity) {
        return switch (gravity) {
            case DOWN -> new Quaternionf();
            case UP -> new Quaternionf().rotationZ(Mth.PI);
            case EAST -> new Quaternionf().rotationY(Mth.PI * 0.5f).rotateX(-Mth.HALF_PI);
            case WEST -> new Quaternionf().rotationY(-Mth.PI * 0.5f).rotateX(-Mth.HALF_PI);
            case SOUTH -> new Quaternionf().rotationX(-Mth.HALF_PI);
            case NORTH -> new Quaternionf().rotationY(Mth.PI).rotateX(-Mth.HALF_PI);
        };
    }
}