package com.sakpeipei.undertale.entity.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sakpeipei.undertale.common.phys.LocalDirection;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.utils.CoordsUtils;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Targeting;
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
public class GravityData {
    private static final Logger log = LoggerFactory.getLogger(GravityData.class);
    // 预定义的重力方向四元数
    public static final Quaternionf QUAT_NORTH = new Quaternionf().rotationX(Mth.PI * 0.5f);  // 绕X转90度
    public static final Quaternionf QUAT_SOUTH = new Quaternionf().rotationX(-Mth.PI * 0.5f); // 绕X转-90度
    public static final Quaternionf QUAT_EAST = new Quaternionf().rotationZ(Mth.PI * 0.5f);   // 绕Z转90度
    public static final Quaternionf QUAT_WEST = new Quaternionf().rotationZ(-Mth.PI * 0.5f);  // 绕Z转-90度

    // Codec用于序列化，支持网络同步
    public static final Codec<GravityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Direction.CODEC.fieldOf("gravity").forGetter(data -> data.gravity)
    ).apply(instance, GravityData::new));

    // 控制方向
    private Direction gravity = Direction.DOWN;
    private Quaternionf logicToWorld = new Quaternionf();
    private Quaternionf worldToLogic = new Quaternionf();
    private Quaternionf oldToNewWorld = new Quaternionf();

    public GravityData() {
    }

    public GravityData(Direction gravity) {
        this.gravity = gravity;
        this.logicToWorld = getGravityQuaternionf(gravity);
        this.worldToLogic = logicToWorld.invert(new Quaternionf());
    }
    public GravityData(Direction forward, LocalDirection logicGravity) {
        this.gravity = switch (logicGravity) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case FRONT -> forward;
            case BACK -> forward.getOpposite();
            case LEFT -> forward.getCounterClockWise();
            case RIGHT -> forward.getClockWise();
        };
        this.logicToWorld = getGravityQuaternionf(gravity);
        this.worldToLogic = logicToWorld.invert(new Quaternionf());
    }
    public GravityData(Direction forward, Quaternionf oldWorldToLogic) {
        this.gravity = forward;
        this.logicToWorld = getGravityQuaternionf(gravity);
        this.worldToLogic = logicToWorld.invert(new Quaternionf());
        this.oldToNewWorld = logicToWorld.mul(oldWorldToLogic,new Quaternionf());
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
        return logicToWorld.transform(new Vector3f(1, 0, 0));
    }

    /**
     * 获取局部坐标系的上方向在世界坐标系中的向量
     * 局部上方向 = (0,1,0)，转换到世界坐标系
     */
    public Vector3f getUp() {
        return logicToWorld.transform(new Vector3f(0, 1, 0));
    }

    /**
     * 获取局部坐标系的前方向在世界坐标系中的向量
     * 局部前方向 = (0,0,1)，转换到世界坐标系
     */
    public Vector3f getForward() {
        return logicToWorld.transform(new Vector3f(0, 0, 1));
    }



    public Quaternionf getLogicToWorld() {
        return logicToWorld;

    }

    public Quaternionf getWorldToLogic() {
        return worldToLogic;
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

    /**
     * 对目标实体应用攻击者的相对重力
     */
    public static GravityData applyRelativeGravity(Entity attacker, Entity target, LocalDirection logicGravity) {
        Direction forward = Direction.fromYRot(attacker.getYHeadRot());
        GravityData oldGravity = target.getData(AttachmentTypeRegistry.GRAVITY);
        GravityData gravityData = new GravityData(calculateGravity(forward, logicGravity),oldGravity.getWorldToLogic());
        log.info("应用的重力坐标系：{},逻辑转世界矩阵：{},世界转逻辑矩阵：{}",gravityData,gravityData.getLogicToWorld(),gravityData.getWorldToLogic());
        target.setData(AttachmentTypeRegistry.GRAVITY, gravityData);
        gravityData.applyGravity(target,oldGravity);
        return gravityData;
    }


    public void applyGravity(Entity target,GravityData oldGravity) {
        Vec3 posOld = target.position();
        Vec3 logicPos = switch (oldGravity.getGravity()) {
            case DOWN -> target.position();
            case UP   -> target.position().subtract(0,target.getBbHeight(),0);
            case NORTH -> null;
            case SOUTH -> null;
            case WEST -> null;
            case EAST -> null;
        };
        float xRotOld = target.getXRot();
        float yRotOld = target.getYRot();
        switch (this.gravity) {
            case UP   -> {
                target.setPos(logicPos.add(0,target.getBbHeight(),0));
            }
//            case EAST -> ppos.add(target.getBbHeight()*0.5f,target.getBbWidth(),0);
//            case WEST -> ppos.add(-target.getBbHeight()*0.5f,target.getBbWidth(),0);
//            case SOUTH -> ppos.add(0,target.getBbWidth(),target.getBbHeight()*0.5f);
//            case NORTH -> ppos.add(0,target.getBbWidth(),-target.getBbHeight()*0.5f);
        };
            log.info("target之前世界坐标系的角度：({},{})，位置：{}，之后世界坐标系的角度：({},{})，位置：{}",xRotOld,yRotOld, posOld,target.getXRot(),target.getYRot(),target.position());
    }

    public void applyPos(Entity target,Direction gravity,Quaternionf oldWorldToLogic) {
        Vec3 logicPos = CoordsUtils.transform(target.position(), oldWorldToLogic);
        switch (this.gravity) {
            case UP -> {
                Quaternionf localRotation = new Quaternionf().rotationY(target.getYRot()).rotateX(target.getXRot());
                Quaternionf re = localRotation.mul(this.logicToWorld, new Quaternionf());
                Vector3f eulerAnglesXYZ = re.getEulerAnglesXYZ(new Vector3f());
                target.setXRot(eulerAnglesXYZ.x);
                target.setYRot(eulerAnglesXYZ.y);
                target.setPos(logicPos.add(0, target.getBbHeight(), 0));
            }
        }
    }
    private static float[] getYawPitchFromQuaternion(Quaternionf q) {
        // 从四元数计算前向向量
        float x = 2 * (q.x * q.z - q.w * q.y);
        float y = 2 * (q.y * q.z + q.w * q.x);
        float z = 1 - 2 * (q.x * q.x + q.y * q.y);

        // 标准化
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;

        // 计算 yaw 和 pitch
        float yaw = (float)Math.toDegrees(Math.atan2(x, z));
        float pitch = (float)Math.toDegrees(-Math.asin(y));

        return new float[]{yaw, pitch};
    }
    public static Direction calculateGravity(Direction forward, LocalDirection logicGravity) {
        return switch (logicGravity) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case FRONT -> forward;
            case BACK -> forward.getOpposite();
            case LEFT -> forward.getCounterClockWise();
            case RIGHT -> forward.getClockWise();
        };
    }
    /**
     * 根据重力方向获取局部→世界变换矩阵
     */
    public static Quaternionf getGravityQuaternionf(Direction gravity) {
        return switch (gravity) {
            case DOWN -> new Quaternionf();
            case UP -> new Quaternionf().rotationZ(Mth.PI);
            case NORTH -> QUAT_NORTH;
            case SOUTH -> QUAT_SOUTH;
            case EAST -> QUAT_EAST;
            case WEST -> QUAT_WEST;
        };
    }
}