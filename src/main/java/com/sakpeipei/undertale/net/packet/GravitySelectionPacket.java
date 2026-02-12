package com.sakpeipei.undertale.net.packet;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.common.phys.LocalDirection;
import com.sakpeipei.undertale.item.GravityTestItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GravitySelectionPacket(LocalDirection direction) implements CustomPacketPayload {

    public static final Type<GravitySelectionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Undertale.MOD_ID, "gravity_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GravitySelectionPacket> STREAM_CODEC = CustomPacketPayload.codec(GravitySelectionPacket::write, GravitySelectionPacket::new);


    public GravitySelectionPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(LocalDirection.class));
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(direction);
    }

    public static void handle(GravitySelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // 获取玩家手中的物品
                ItemStack heldItem = serverPlayer.getMainHandItem();
                // 检查是否是重力测试物品
                if (heldItem.getItem() instanceof GravityTestItem gravityItem) {
                    gravityItem.setGravityDirection(heldItem, packet.direction);
                    // 发送提示消息
                    serverPlayer.sendSystemMessage(
                            Component.translatable("message.undertale.direction_saved",
                                    Component.translatable("direction.undertale." + packet.direction.name().toLowerCase()))
                    );
                }
            }
        });
    }
    @Override
    public @NotNull CustomPacketPayload.Type<GravitySelectionPacket> type() {
        return TYPE;
    }
}