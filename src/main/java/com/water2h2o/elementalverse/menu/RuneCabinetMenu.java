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

    /** 电量同步通道；客户端拿不到方块实体时也能画。 */
    private net.minecraft.world.inventory.ContainerData data;

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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 154));
        }

        // 电量：0 = 剩余秒数，1 = 满电秒数。界面靠它画电量条。
        this.addDataSlots(this.data = new net.minecraft.world.inventory.ContainerData() {
            @Override
            public int get(int index) {
                if (cabinet == null) {
                    return 0;
                }
                return index == 0 ? cabinet.getEnergySeconds() : cabinet.getMaxEnergySeconds();
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        });
    }

    /** 剩余电量（秒）。 */
    public int energySeconds() {
        return this.data == null ? 0 : this.data.get(0);
    }

    /** 满电时有多少秒。 */
    public int maxEnergySeconds() {
        return this.data == null ? 1 : Math.max(1, this.data.get(1));
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
