package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.utils.CoordsUtils;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * @author Sakpeipei
 * @since 2025/11/14 10:46
 * 服务端处理客户端的位移：世界转局部
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplGravityMixin {
    @Shadow
    public ServerPlayer player;



    /**
     * d6为客户端发送的X位置和服务端X位置的差值
     */
    @ModifyVariable(method = "handleMovePlayer", at = @At(value = "STORE"), ordinal = 6)
    private double d6_dx(double d6) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || d6 == 0) return d6;
        return CoordsUtils.transform(d6, 0, 0, data.getWorldToLogic()).x;
    }
    /**
     * d7为客户端发送的Y位置和服务端Y位置的差值
     */
    @ModifyVariable(method = "handleMovePlayer", at = @At(value = "STORE"), ordinal = 7)
    private double d7_dy(double d7) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || d7 == 0) return d7;
        return CoordsUtils.transform(0, d7, 0, data.getWorldToLogic()).y;
    }
    /**
     * d8为客户端发送的Z位置和服务端Z位置的差值
     */
    @ModifyVariable(method = "handleMovePlayer", at = @At(value = "STORE"), ordinal = 8)
    private double d8_dz(double d8) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || d8 == 0) return d8;
        return CoordsUtils.transform(0, 0, d8, data.getWorldToLogic()).z;
    }


    /**
     * 用于调用move方法，进行移动的位移
     */
    @Redirect(method = "handleMovePlayer", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 1))
    private Vec3 newVec3MovementInHandleMovePlayer(double dx, double dy, double dz) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || (dx + dy + dz == 0)) return new Vec3(dx, dy, dz);
        return CoordsUtils.transform(dx, dy, dz, data.getWorldToLogic());
    }

    /**
     * 检查坠落伤害的位移
     */
    @ModifyArgs(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doCheckFallDamage(DDDZ)V"))
    private void doCheckFallDamageInHandleMovePlayer(Args args) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        double dx = args.get(0), dy = args.get(1), dz = args.get(2);
        if (data.getGravity() == Direction.DOWN || (dx + dy + dz == 0)) return;
        Vec3 logicDD = CoordsUtils.transform(dx, dy, dz, data.getWorldToLogic());
        args.set(0, logicDD.x);
        args.set(1, logicDD.y);
        args.set(2, logicDD.z);
    }

    /**
     * 检查玩家统计数据的位移
     */
    @ModifyArgs(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;checkMovementStatistics(DDD)V"))
    private void checkMovementStatisticsInHandleMovePlayer(Args args) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        double dx = args.get(0), dy = args.get(1), dz = args.get(2);
        if (data.getGravity() == Direction.DOWN || (dx + dy + dz == 0)) return;
        Vec3 logicDD = CoordsUtils.transform(dx, dy, dz, data.getWorldToLogic());
        args.set(0, logicDD.x);
        args.set(1, logicDD.y);
        args.set(2, logicDD.z);
    }

}
