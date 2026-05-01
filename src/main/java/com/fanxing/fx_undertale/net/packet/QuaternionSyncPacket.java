package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.summon.RotationBone;
import com.fanxing.lib.entity.capability.OBBRotationCollider;
import com.fanxing.lib.entity.capability.QuaternionRotatable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 强制同步 QuaternionRotatable 实体的四元数和角速度（用于碰撞反弹等需要快速同步的场景）
 * 对应接口：QuaternionRotatable, OBBRotationCollider
 */
public record QuaternionSyncPacket(int entityId, float quatX, float quatY, float quatZ, float quatW,
                                    float angularVelX, float angularVelY, float angularVelZ) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QuaternionSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "quaternion_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuaternionSyncPacket> STREAM_CODEC =
        CustomPacketPayload.codec(QuaternionSyncPacket::write, QuaternionSyncPacket::new);

    public QuaternionSyncPacket(RegistryFriendlyByteBuf buf) {
        this(
            buf.readVarInt(),
            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readFloat()
        );
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeFloat(quatX);
        buf.writeFloat(quatY);
        buf.writeFloat(quatZ);
        buf.writeFloat(quatW);
        buf.writeFloat(angularVelX);
        buf.writeFloat(angularVelY);
        buf.writeFloat(angularVelZ);
    }

    public static void handle(QuaternionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            var entity = level.getEntity(packet.entityId);
            if (entity == null) return;

            // 强制设置四元数（所有 QuaternionRotatable 实体）
            if (entity instanceof QuaternionRotatable rotatable) {
                Quaternionf quat = rotatable.getOrientation();
                quat.set(packet.quatX, packet.quatY, packet.quatZ, packet.quatW);

                // 对于 RotationBone，还需要设置 previousOrientation
                if (entity instanceof RotationBone bone) {
                    bone.previousOrientation.set(quat);
                }
            }

            // 强制设置角速度（只有 OBBRotationCollider 实体才有角速度）
            if (entity instanceof OBBRotationCollider collider) {
                Vector3f angularVel = new Vector3f(packet.angularVelX, packet.angularVelY, packet.angularVelZ);
                collider.setAngularVelocity(angularVel);

                // 对于 RotationBone，同步欧拉角
                if (entity instanceof RotationBone bone) {
                    bone.syncEulerAnglesFromQuaternion();
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}