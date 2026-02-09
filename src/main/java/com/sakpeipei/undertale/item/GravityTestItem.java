package com.sakpeipei.undertale.item;

import com.sakpeipei.undertale.common.phys.LocalDirection;
import com.sakpeipei.undertale.common.phys.LocalVec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class GravityTestItem extends Item{

    private LocalDirection gravity = LocalDirection.UP;

    public GravityTestItem(Properties properties) {
        super(properties.stacksTo(1));
    }


    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        return super.onLeftClickEntity(stack, player, entity);
    }

    /**
     * 物品使用交互逻辑（右键触发动画）
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if(!level.isClientSide){
        }
        return InteractionResultHolder.consume(itemStack);
    }


    //    @Override
//    public int getMaxStackSize(@NotNull ItemStack stack) {
//        return 1;
//    }
}
