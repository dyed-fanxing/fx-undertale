package com.fanxing.fx_undertale.item;

import com.fanxing.fx_undertale.FxUndertale;
import com.fanxing.fx_undertale.entity.attachment.Gravity;
import com.fanxing.fx_undertale.menu.GravitySelectionMenu;
import com.fanxing.fx_undertale.net.packet.GravityPacket;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class GravityDebugStick extends Item {

    private static final String TAG_GRAVITY = "GravityDirection";

    public GravityDebugStick(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, Player player, @NotNull Entity entity) {
        if (!player.level().isClientSide) {
            // 从物品NBT中读取保存的重力方向
            Direction gravity = getGravityDirection(stack);
            Gravity.applyGravity(entity, gravity);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new GravityPacket(entity.getId(), gravity));
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level,
                                                           @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if(player.isShiftKeyDown()) {
                // 右键打开菜单选择方向
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new GravitySelectionMenu(id, inventory,
                                itemStack,  // 传递ItemStack而不是this
                                ContainerLevelAccess.create(level, player.blockPosition())),
                        Component.translatable("menu." + FxUndertale.MOD_ID + ".gravity_selection")
                ));
            }else{
                Gravity gravityData = Gravity.applyGravity(player, getGravityDirection(player.getItemInHand(hand)));
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GravityPacket(player.getId(), gravityData.getGravity()));
            }
        }

        return InteractionResultHolder.consume(itemStack);
    }


    // 从物品NBT中获取重力方向
    public Direction getGravityDirection(ItemStack stack) {
        // 使用 getOrDefault 获取 CustomData
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        // 使用 getUnsafe 或 copyTag
        CompoundTag tag = customData.copyTag();  // 或者 try/catch

        if (!tag.isEmpty() && tag.contains(TAG_GRAVITY)) {
            String directionName = tag.getString(TAG_GRAVITY);
            try {
                return Direction.valueOf(directionName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Direction.DOWN;
            }
        }
        return Direction.DOWN;
    }

    // 保存重力方向到物品NBT
    public void setGravityDirection(ItemStack stack, Direction direction) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_GRAVITY, direction.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }


}
