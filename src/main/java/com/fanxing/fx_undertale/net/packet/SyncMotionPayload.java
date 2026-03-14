package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.ISyncablePhysicsMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncMotionPayload(int entityId, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncMotionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "sync_motion"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMotionPayload> STREAM_CODEC = CustomPacketPayload.codec(SyncMotionPayload::write, SyncMotionPayload::new);

    public SyncMotionPayload(FriendlyByteBuf buf) {
        this(buf.readVarInt(),buf.readByteArray());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByteArray(data);
    }

    public static void handle(SyncMotionPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(packet.entityId) instanceof ISyncablePhysicsMotion syncableMotion) {
                syncableMotion.getMotionModel().readSyncData(packet.data);
            }
        });
    }
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}

