package com.fanxing.fx_undertale.net.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VehicleInputPacket(byte buttons) {
    // 定义位掩码
    public static final byte BIT_FORWARD = 0b000001; // W
    public static final byte BIT_BACKWARD = 0b000010; // S
    public static final byte BIT_LEFT = 0b000100;    // A
    public static final byte BIT_RIGHT = 0b001000;   // D
    public static final byte BIT_JUMP = 0b010000;    // 空格（上升）
    public static final byte BIT_SHIFT = 0b100000;   // Shift（下降）

    public static VehicleInputPacket decode(FriendlyByteBuf buf) {
        return new VehicleInputPacket(buf.readByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(buttons);
    }

    public static void handle(VehicleInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 确保在服务端主线程执行
            if (context.player() instanceof ServerPlayer player) {
                var vehicle = player.getVehicle();
//                if (vehicle instanceof VehicleInputListener listener) {
//                    listener.setVehicleInput(payload.buttons());
//                }
            }
        });
    }

    // 辅助方法：从客户端按键构建按钮字节
    public static byte pack(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift) {
        byte b = 0;
        if (forward) b |= BIT_FORWARD;
        if (backward) b |= BIT_BACKWARD;
        if (left) b |= BIT_LEFT;
        if (right) b |= BIT_RIGHT;
        if (jump) b |= BIT_JUMP;
        if (shift) b |= BIT_SHIFT;
        return b;
    }

    // 辅助方法：解包
    public boolean isForward()  { return (buttons & BIT_FORWARD) != 0; }
    public boolean isBackward() { return (buttons & BIT_BACKWARD) != 0; }
    public boolean isLeft()     { return (buttons & BIT_LEFT) != 0; }
    public boolean isRight()    { return (buttons & BIT_RIGHT) != 0; }
    public boolean isJump()     { return (buttons & BIT_JUMP) != 0; }
    public boolean isShift()    { return (buttons & BIT_SHIFT) != 0; }
}