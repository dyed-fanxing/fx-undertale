package com.sakpeipei.mod.undertale.mixin;

import com.google.common.primitives.Floats;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;


/**
* @author Sakqiongzi
* @since 2025-10-19 22:42
*/
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    private int tickCount;
    @Shadow
    private double firstGoodX;
    @Shadow
    private double firstGoodY;
    @Shadow
    private double firstGoodZ;
    @Shadow
    private double lastGoodX;
    @Shadow
    private double lastGoodY;
    @Shadow
    private double lastGoodZ;
    @Shadow
    private boolean clientVehicleIsFloating;
    @Shadow
    private int aboveGroundVehicleTickCount;
    @Shadow
    private int receivedMovePacketCount;
    @Shadow
    private int knownMovePacketCount;

    public ServerGamePacketListenerImplMixin(MinecraftServer p_295057_, Connection p_294822_, CommonListenerCookie p_301980_) {
        super(p_295057_, p_294822_, p_301980_);
    }

    @Shadow
    protected abstract boolean updateAwaitingTeleport();
    @Shadow
    protected abstract boolean isPlayerCollidingWithAnythingNew(LevelReader p_289008_, AABB p_288986_, double p_288990_, double p_288991_, double p_288967_);
    /**
     * 重写整个 handleMovePlayer 方法，添加详细日志
     * @author Sakpeipei
     */
    @Overwrite
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {
        ServerGamePacketListenerImpl self = (ServerGamePacketListenerImpl) (Object) this;
        ServerPlayer player = self.player;

        PacketUtils.ensureRunningOnSameThread(packet, self, player.serverLevel());

        // 记录移动包基本信息
//        LOGGER.info("=== 开始处理移动包 ===");
//        LOGGER.info("玩家: {}, 在地面: {}, 骑乘: {}",player.getName().getString(), packet.isOnGround(), player.isPassenger());

        if (containsInvalidValues(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0),
                packet.getYRot(0.0F), packet.getXRot(0.0F))) {
            self.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
            return;
        }

        ServerLevel serverlevel = player.serverLevel();
        if (!player.wonGame) {
            if (this.tickCount == 0) {
                self.resetPosition();
            }

            if (!this.updateAwaitingTeleport()) {
                double clientX = clampHorizontal(packet.getX(player.getX()));
                double clientY = clampVertical(packet.getY(player.getY()));
                double clientZ = clampHorizontal(packet.getZ(player.getZ()));
                float yRot = Mth.wrapDegrees(packet.getYRot(player.getYRot()));
                float xRot = Mth.wrapDegrees(packet.getXRot(player.getXRot()));

//                LOGGER.info("客户端位置: ({}, {}, {}), 服务端位置: ({}, {}, {})",clientX, clientY, clientZ, player.getX(), player.getY(), player.getZ());

                if (player.isPassenger()) {
                    player.absMoveTo(player.getX(), player.getY(), player.getZ(), yRot, xRot);
                    player.serverLevel().getChunkSource().move(player);
                } else {
                    double startX = player.getX();
                    double startY = player.getY();
                    double startZ = player.getZ();

                    // 记录移动前的关键状态
//                    LOGGER.info("移动前 - lastGood: ({}, {}, {}), 位置: ({}, {}, {})",this.lastGoodX, this.lastGoodY, this.lastGoodZ, startX, startY, startZ);

                    // 预期移动向量
                    double expectedDx = clientX - this.lastGoodX;
                    double expectedDy = clientY - this.lastGoodY;
                    double expectedDz = clientZ - this.lastGoodZ;
                    double expectedDistSqr = expectedDx * expectedDx + expectedDy * expectedDy + expectedDz * expectedDz;

//                    LOGGER.info("预期移动: ({}, {}, {}), 距离平方: {}",expectedDx, expectedDy, expectedDz, expectedDistSqr);

                    if (player.isSleeping()) {
                        if (expectedDistSqr > 1.0) {
                            self.teleport(player.getX(), player.getY(), player.getZ(), yRot, xRot);
                        }
                    } else {
                        // 防作弊检查等逻辑保持不变...
                        boolean isFallFlying = player.isFallFlying();
                        if (serverlevel.tickRateManager().runsNormally()) {
                            ++this.receivedMovePacketCount;
                            int i = this.receivedMovePacketCount - this.knownMovePacketCount;
                            if (i > 5) {
//                                LOGGER.debug("{} 移动包过于频繁 ({} packets since last tick)", player.getName().getString(), i);
                                i = 1;
                            }

                            if (!player.isChangingDimension() &&
                                    (!player.level().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) || !isFallFlying)) {
                                float threshold = isFallFlying ? 300.0F : 100.0F;
                                if (expectedDistSqr - player.getDeltaMovement().lengthSqr() > (threshold * i) && !this.isSingleplayerOwner()) {
//                                    LOGGER.warn("{} 移动过快! {},{},{}", player.getName().getString(), expectedDx, expectedDy, expectedDz);
                                    self.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                                    return;
                                }
                            }
                        }

                        AABB aabb = player.getBoundingBox();

                        // 实际移动向量（相对于上次合法位置）
                        double actualDx = clientX - this.lastGoodX;
                        double actualDy = clientY - this.lastGoodY;
                        double actualDz = clientZ - this.lastGoodZ;
                        boolean isJumping = actualDy > 0.0;

//                        LOGGER.info("实际移动向量: ({}, {}, {}), 是否跳跃: {}", actualDx, actualDy, actualDz, isJumping);

                        if (player.onGround() && !packet.isOnGround() && isJumping) {
                            player.jumpFromGround();
//                            LOGGER.info("触发跳跃");
                        }

                        boolean wasOnGround = player.verticalCollisionBelow;

                        // 执行移动
                        player.move(MoverType.PLAYER, new Vec3(actualDx, actualDy, actualDz));

                        // 移动后的差异计算
                        double diffX = clientX - player.getX();
                        double diffY = clientY - player.getY();
                        double diffZ = clientZ - player.getZ();

//                        LOGGER.info("移动后差异 - 原始: ({}, {}, {})", diffX, diffY, diffZ);

                        // Y轴差异的特殊处理
                        if (diffY > -0.5 || diffY < 0.5) {
                            double originalDiffY = diffY;
                            diffY = 0.0;
//                            LOGGER.info("Y轴差异归零: {} -> {}", originalDiffY, diffY);
                        }

                        double totalDiffSqr = diffX * diffX + diffY * diffY + diffZ * diffZ;
                        boolean movedWrongly = false;

                        if (!player.isChangingDimension() && totalDiffSqr > 0.0625 &&
                                !player.isSleeping() && !player.gameMode.isCreative() &&
                                player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                            movedWrongly = true;
//                            LOGGER.warn("{} 移动异常! 差异平方: {}", player.getName().getString(), totalDiffSqr);
                        }

//                        LOGGER.info("移动异常检测 - 差异平方: {}, 阈值: 0.0625, 结果: {}", totalDiffSqr, movedWrongly);

                        if (player.noPhysics || player.isSleeping() ||
                                (!movedWrongly || !serverlevel.noCollision(player, aabb)) &&
                                        !this.isPlayerCollidingWithAnythingNew(serverlevel, aabb, clientX, clientY, clientZ)) {

                            // 接受客户端位置
                            player.absMoveTo(clientX, clientY, clientZ, yRot, xRot);

                            // 计算最终移动向量
                            Vec3 finalMovement = new Vec3(player.getX() - startX, player.getY() - startY, player.getZ() - startZ);
//                            LOGGER.info("最终移动向量: {}，最终是否移动：{}", finalMovement, Mth.square(finalMovement.y) > 0.01);

                            player.setKnownMovement(finalMovement);
                            player.setOnGroundWithMovement(packet.isOnGround(), finalMovement);
                            player.doCheckFallDamage(finalMovement.x, finalMovement.y, finalMovement.z, packet.isOnGround());

                            // 更新最后合法位置
                            this.lastGoodX = player.getX();
                            this.lastGoodY = player.getY();
                            this.lastGoodZ = player.getZ();

//                            LOGGER.info("更新lastGood位置: ({}, {}, {})", this.lastGoodX, this.lastGoodY, this.lastGoodZ);

                            if (isJumping) {
                                player.resetFallDistance();
                            }

                            player.checkMovementStatistics(finalMovement.x, finalMovement.y, finalMovement.z);

                        } else {
//                            LOGGER.info("移动被拒绝，传送回原位置");
                            self.teleport(startX, startY, startZ, yRot, xRot);
                            player.doCheckFallDamage(player.getX() - startX, player.getY() - startY, player.getZ() - startZ, packet.isOnGround());
                        }
                    }
                }
            }
        }
//        LOGGER.info("=== 移动包处理结束 ===\n");
    }

    // 辅助方法 - 需要复制原版的方法
    private static boolean containsInvalidValues(double x, double y, double z, float yRot, float xRot) {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) ||
                !Floats.isFinite(yRot) || !Floats.isFinite(xRot);
    }

    private double clampHorizontal(double value) {
        return Mth.clamp(value, -3.0E7, 3.0E7);
    }

    private double clampVertical(double value) {
        return Mth.clamp(value, -2.0E7, 2.0E7);
    }
}
