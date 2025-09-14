package com.sakpeipei.mod.undertale.network;

import com.sakpeipei.mod.undertale.Undertale;
import com.sakpeipei.mod.undertale.registry.AttachmentTypeRegistry;
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
 * @since 2025-09-13 22:52
 */
public record KaramPacket(int id,byte value) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KaramPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "karam_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KaramPacket> STREAM_CODEC = CustomPacketPayload.codec(KaramPacket::write, KaramPacket::new);

    public KaramPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readByte());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeByte(value);
    }

    public static void handle(KaramPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity entity = level.getEntity(packet.id);
                if (entity != null) {
                    entity.getData(AttachmentTypeRegistry.KARMA_MOB_EFFECT).setValue(packet.value);
                }
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<KaramPacket> type() {
        return TYPE;
    }
}
