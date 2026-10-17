package com.water2h2o.elementalverse.menu;

import com.water2h2o.elementalverse.block.SealedVaultBlockEntity;
import com.water2h2o.elementalverse.registry.EVMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 石制密封仓的界面：3×3 共 9 格，布局与**原版发射器**一致
 * （作者要求"和发射器一样"），因此可以直接复用发射器的 GUI 贴图。
 */
public class SealedVaultMenu extends AbstractContainerMenu {

    /** 发射器 GUI 里 3×3 格子的起始坐标与间距。 */
    private static final int GRID_LEFT = 62;
    private static final int GRID_TOP = 17;
    private static final int CELL = 18;

    private final Container container;

    /**
     * 客户端构造：服务端打开界面时会把方块坐标传过来，据此找回容器。
     * （由 `IMenuTypeExtension.create` 调用，所以参数里带 FriendlyByteBuf。）
     */
    public SealedVaultMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolve(playerInventory.player, buffer.readBlockPos()));
    }

    /** 服务端构造用。 */
    public SealedVaultMenu(int containerId, Inventory playerInventory, Container container) {
        super(EVMenus.SEALED_VAULT.get(), containerId);
        this.container = container;
        checkContainerSize(container, SealedVaultBlockEntity.SIZE);
        container.startOpen(playerInventory.player);

        // 3×3 存储格
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, col + row * 3,
                        GRID_LEFT + col * CELL, GRID_TOP + row * CELL));
            }
        }

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * CELL, 84 + row * CELL));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * CELL, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }

        ItemStack inSlot = slot.getItem();
        moved = inSlot.copy();

        int storageEnd = 9;

        if (index < storageEnd) {
            // 仓库 → 背包
            if (!this.moveItemStackTo(inSlot, storageEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 背包 → 仓库
            if (!this.moveItemStackTo(inSlot, 0, storageEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    /** 客户端按坐标找回方块实体；找不到就给个空容器兜底，避免界面崩。 */
    private static Container resolve(Player player, BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof Container found) {
            return found;
        }
        return new SimpleContainer(SealedVaultBlockEntity.SIZE);
    }
}
