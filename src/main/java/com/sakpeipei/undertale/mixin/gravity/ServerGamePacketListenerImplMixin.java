package com.sakpeipei.undertale.mixin.gravity;

import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import com.sakpeipei.undertale.utils.CoordsUtils;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * @author Sakpeipei
 * @since 2025/11/14 10:46
 * Entity重力相关方法
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;
    private static final Logger log = LoggerFactory.getLogger(ServerGamePacketListenerImplMixin.class);

    @ModifyVariable(method = "handleMovePlayer", at = @At(value = "STORE"), ordinal = 6)
    private double d6(double value) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || value == 0) return value;
        return CoordsUtils.transform(value, 0, 0, data.getWorldToLogic()).x;
    }
    @ModifyVariable(method = "handleMovePlayer", at = @At(value = "STORE"), ordinal = 7)
    private double d7(double value) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || value == 0) return value;
        return CoordsUtils.transform(0, value, 0, data.getWorldToLogic()).y;
    }
    @ModifyVariable(method = "handleMovePlayer", at = @At(value = "STORE"), ordinal = 8)
    private double d8(double value) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || value == 0) return value;
        return CoordsUtils.transform(0, 0, value, data.getWorldToLogic()).z;
    }

    @Redirect(method = "handleMovePlayer", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 1))
    private Vec3 newVec3MovementInHandleMovePlayer(double dx, double dy, double dz) {
        GravityData data = player.getData(AttachmentTypeRegistry.GRAVITY);
        if (data.getGravity() == Direction.DOWN || (dx + dy + dz == 0)) return new Vec3(dx, dy, dz);
        return CoordsUtils.transform(dx, dy, dz, data.getWorldToLogic());
    }

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
