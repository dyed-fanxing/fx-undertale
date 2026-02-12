package com.sakpeipei.undertale.net.packet;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.registry.AttachmentTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sakqiongzi
 * @since 2025-09-13 22:52
 */
public record KaramPacket(int entityId,byte value,float absorptionAmount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KaramPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MODID, "karam_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KaramPacket> STREAM_CODEC = CustomPacketPayload.codec(KaramPacket::write, KaramPacket::new);

    public KaramPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readByte(),buf.readFloat());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByte(value);
        buf.writeFloat(absorptionAmount);
    }

    public static void handle(KaramPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(packet.entityId) instanceof LivingEntity entity) {
                entity.getData(AttachmentTypeRegistry.KARMA_MOB_EFFECT).setValue(packet.value);
                if(packet.absorptionAmount != -1){
                    entity.setAbsorptionAmount(packet.absorptionAmount);
                }
            }
        });
    }

    @Override
    public @NotNull CustomPacketPayload.Type<KaramPacket> type() {
        return TYPE;
    }
}
