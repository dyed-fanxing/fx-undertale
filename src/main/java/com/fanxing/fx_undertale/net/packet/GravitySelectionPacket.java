package com.fanxing.fx_undertale.net.packet;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.item.GravityDebugStick;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GravitySelectionPacket(Direction direction) implements CustomPacketPayload {

    public static final Type<GravitySelectionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FxUndertale.MOD_ID, "gravity_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GravitySelectionPacket> STREAM_CODEC = CustomPacketPayload.codec(GravitySelectionPacket::write, GravitySelectionPacket::new);


    public GravitySelectionPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(Direction.class));
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
                if (heldItem.getItem() instanceof GravityDebugStick gravityItem) {
                    gravityItem.setGravityDirection(heldItem, packet.direction);
                }
            }
        });
    }
    @Override
    public @NotNull CustomPacketPayload.Type<GravitySelectionPacket> type() {
        return TYPE;
    }
}