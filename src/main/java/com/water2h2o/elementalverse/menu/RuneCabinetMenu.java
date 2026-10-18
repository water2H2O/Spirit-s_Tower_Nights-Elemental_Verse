package com.water2h2o.elementalverse.menu;

import com.water2h2o.elementalverse.block.RuneCabinetBlockEntity;
import com.water2h2o.elementalverse.registry.EVMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 符文柜界面：3x9 + 玩家背包（沿用原版箱子布局）。 */
public class RuneCabinetMenu extends AbstractContainerMenu {

    private final RuneCabinetBlockEntity cabinet;

    public RuneCabinetMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolve(playerInventory.player, buffer));
    }

    private static RuneCabinetBlockEntity resolve(Player player, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        var be = player.level().getBlockEntity(pos);
        return be instanceof RuneCabinetBlockEntity cabinet ? cabinet : null;
    }

    public RuneCabinetMenu(int containerId, Inventory playerInventory, RuneCabinetBlockEntity cabinet) {
        super(EVMenus.RUNE_CABINET.get(), containerId);
        this.cabinet = cabinet;

        if (cabinet != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new Slot(cabinet, col + row * 9, 8 + col * 18, 18 + row * 18));
                }
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            com.water2h2o.elementalverse.element.ElementStackMerger.mergeAll(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return cabinet != null && cabinet.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = cabinet == null ? 0 : RuneCabinetBlockEntity.SIZE;
        if (index < machineSlots) {
            if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
