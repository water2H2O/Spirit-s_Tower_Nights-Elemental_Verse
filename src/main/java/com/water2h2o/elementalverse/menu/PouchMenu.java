package com.water2h2o.elementalverse.menu;

import java.util.ArrayList;
import java.util.List;

import com.water2h2o.elementalverse.element.PouchContents;
import com.water2h2o.elementalverse.registry.EVMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 元素小包的界面：3×3 共 9 格，与密封仓同样的布局（复用发射器 GUI 贴图）。
 *
 * <p>内容存在物品的 {@code pouch_contents} 组件里。打开时读出来铺进界面，
 * **关闭时写回去** —— 这是"物品里的容器"的常规做法。
 */
public class PouchMenu extends AbstractContainerMenu {

    private static final int GRID_LEFT = 62;
    private static final int GRID_TOP = 17;
    private static final int CELL = 18;

    private final ItemStack pouch;
    private final SimpleContainer container;

    /** 客户端：按服务端传过来的"哪只手"找回那个堆叠。 */
    public PouchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                playerInventory.player.getItemInHand(buffer.readEnum(InteractionHand.class)));
    }

    public PouchMenu(int containerId, Inventory playerInventory, ItemStack pouch) {
        super(EVMenus.POUCH.get(), containerId);
        this.pouch = pouch;
        this.container = new SimpleContainer(PouchContents.SIZE);

        List<ItemStack> stored = PouchContents.get(pouch);
        for (int i = 0; i < stored.size(); i++) {
            this.container.setItem(i, stored.get(i));
        }

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
        // 小包拿在手上，随时有效（丢掉了也只是写回空气，不会崩）
        return true;
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
        int storageEnd = PouchContents.SIZE;

        if (index < storageEnd) {
            if (!this.moveItemStackTo(inSlot, storageEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(inSlot, 0, storageEnd, false)) {
            return ItemStack.EMPTY;
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
        if (player.level().isClientSide()) {
            return;
        }
        // 写回物品的数据组件
        List<ItemStack> items = new ArrayList<>(PouchContents.SIZE);
        for (int i = 0; i < PouchContents.SIZE; i++) {
            items.add(this.container.getItem(i));
        }
        PouchContents.set(pouch, items);
    }
}
