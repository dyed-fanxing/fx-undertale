package com.fanxing.fx_undertale.menu;

import com.fanxing.fx_undertale.registry.MenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GravitySelectionMenu extends AbstractContainerMenu {
    private final Player player;
    private final ItemStack itemStack;  // 只需要 ItemStack
    private final ContainerLevelAccess access;

    // ✅ 客户端构造函数 - 供MenuType调用
    public GravitySelectionMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory,
                playerInventory.player.getMainHandItem(),
                ContainerLevelAccess.NULL);
    }

    // ✅ 服务端构造函数 - 只有4个参数！
    public GravitySelectionMenu(int containerId, Inventory playerInventory,
                                ItemStack itemStack,
                                ContainerLevelAccess access) {
        super(MenuTypes.GRAVITY_SELECTION_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.itemStack = itemStack;
        this.access = access;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // 检查玩家是否仍然持有这个物品堆
        return player.getMainHandItem() == itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}