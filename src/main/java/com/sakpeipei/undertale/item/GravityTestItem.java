package com.sakpeipei.undertale.item;

import com.sakpeipei.undertale.Undertale;
import com.sakpeipei.undertale.common.phys.LocalDirection;
import com.sakpeipei.undertale.entity.attachment.GravityData;
import com.sakpeipei.undertale.menu.GravitySelectionMenu;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class GravityTestItem extends Item {

    private static final String TAG_GRAVITY = "GravityDirection";

    public GravityTestItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide) {
            // 从物品NBT中读取保存的重力方向
            LocalDirection gravity = getGravityDirection(stack);
            GravityData.applyRelativeGravity(player, entity, gravity);

            // 发送提示消息
            player.sendSystemMessage(
                    Component.translatable("message.undertale.gravity_applied",
                            Component.translatable("direction.undertale." + gravity.name().toLowerCase()),
                            entity.getName())
            );
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
                        Component.translatable("menu." + Undertale.MOD_ID + ".gravity_selection")
                ));
            }else{
                GravityData.applyRelativeGravity(player,player,getGravityDirection(player.getItemInHand(hand)));
            }
        }

        return InteractionResultHolder.consume(itemStack);
    }

    // 从物品NBT中获取重力方向
    public LocalDirection getGravityDirection(ItemStack stack) {
        // 使用 getOrDefault 获取 CustomData
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        // 使用 getUnsafe 或 copyTag
        CompoundTag tag = customData.copyTag();  // 或者 try/catch

        if (!tag.isEmpty() && tag.contains(TAG_GRAVITY)) {
            String directionName = tag.getString(TAG_GRAVITY);
            try {
                return LocalDirection.valueOf(directionName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return LocalDirection.UP;
            }
        }
        return LocalDirection.UP;
    }

    // 保存重力方向到物品NBT
    public void setGravityDirection(ItemStack stack, LocalDirection direction) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_GRAVITY, direction.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // 显示当前保存的重力方向
        LocalDirection direction = getGravityDirection(stack);
        tooltip.add(Component.translatable("tooltip.undertale.current_direction",
                Component.translatable("direction.undertale." + direction.name().toLowerCase())));
    }
}
