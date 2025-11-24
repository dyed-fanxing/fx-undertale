package com.sakpeipei.mod.undertale.network;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.entity.IAnimatable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sakqiongzi
 * @since 2025-11-19 20:02
 */
public record AnimIDPacket(int entityId, byte id) implements CustomPacketPayload{
    public static final CustomPacketPayload.Type<AnimIDPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "anim_id"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnimIDPacket> STREAM_CODEC = CustomPacketPayload.codec(AnimIDPacket::write, AnimIDPacket::new);


    public AnimIDPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(),buf.readByte());
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.id);
    }

    public static void handle(AnimIDPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.entityId);

                if (entity instanceof IAnimatable entity1) {
                    entity1.setAnimID(packet.id);
                }
            }
        });
    }
    @Override
    public @NotNull Type<AnimIDPacket> type() {
        return TYPE;
    }
}