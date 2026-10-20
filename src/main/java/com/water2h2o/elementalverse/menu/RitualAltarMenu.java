package com.water2h2o.elementalverse.menu;

import com.water2h2o.elementalverse.block.RitualAltarBlockEntity;
import com.water2h2o.elementalverse.registry.EVMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 仪式台界面。
 *
 * <p>布局用作者给的仪式台底图：
 * <pre>
 *   上排三个格子：主料 / 辅料 / 态
 *   左中：申明板      中下：剑槽（刻印）      右下：产出
 * </pre>
 *
 * <p>三个投入口都参与计算：主料 + 辅料构成反应输入，态决定 SpellParams。
 * 剑槽在贴图原来的“箭头”位置，屏幕会补画一个槽位底图并标注“剑槽”。
 */
public class RitualAltarMenu extends AbstractContainerMenu {

    private static final int CELL = 18;
    private static final int GRID_LEFT = 62;
    /** 三个配料口在第二行；第一行是命名槽，两行之间留一行空隙。 */
    private static final int GRID_TOP = 45;

    private final RitualAltarBlockEntity altar;

    /** 仪式台没有进度条之类的同步数据，给个空实现。 */
    private static final ContainerData NO_DATA = new ContainerData() {
        @Override
        public int get(int index) {
            return 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 0;
        }
    };

    /** 客户端：按服务端传来的坐标找回方块实体。 */
    public RitualAltarMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, resolve(playerInventory.player, buffer), NO_DATA);
    }

    private static RitualAltarBlockEntity resolve(Player player, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        var be = player.level().getBlockEntity(pos);
        return be instanceof RitualAltarBlockEntity altar ? altar : null;
    }

    public RitualAltarMenu(int containerId, Inventory playerInventory,
                           RitualAltarBlockEntity altar, ContainerData data) {
        super(EVMenus.RITUAL_ALTAR.get(), containerId);
        this.altar = altar;

        if (altar != null) {
            // 三个配料口：料 + 辅料 + 态
            for (int i = 0; i < 3; i++) {
                this.addSlot(new Slot(altar, i, GRID_LEFT + i * CELL, GRID_TOP));
            }
            // 申明板（空隙下面）
            this.addSlot(new Slot(altar, RitualAltarBlockEntity.SLOT_PLATE,
                    GRID_LEFT, GRID_TOP + 2 * CELL));
            // 产出：只能拿，不能放
            this.addSlot(new Slot(altar, RitualAltarBlockEntity.SLOT_OUTPUT,
                    GRID_LEFT + 2 * CELL, GRID_TOP + 2 * CELL) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * CELL, 112 + row * CELL));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * CELL, 170));
        }
    }

    /** 界面上的命名框改了字 → 交给方块实体。 */
    public void setCustomName(String name) {
        if (altar != null) {
            altar.setCustomName(name);
        }
    }

    /** 命名框当前的内容（客户端留空即可，服务端才是权威）。 */
    public String customName() {
        return altar == null ? "" : altar.getCustomName();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            // 名字只在这一轮里有效，关界面就清掉，下次打开不会出现"框里空但产物带名"
            if (altar != null) {
                altar.setCustomName("");
            }
            com.water2h2o.elementalverse.element.ElementStackMerger.mergeAll(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return altar != null && altar.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        // 产出格必须走 removeItem（会扣材料）。直接 moveItemStackTo 会绕过消耗，
        // 再被 setByPlayer(EMPTY) 清空，等于无限白拿。
        if (altar != null && slot.container == altar
                && slot.getContainerSlot() == RitualAltarBlockEntity.SLOT_OUTPUT) {
            ItemStack extracted = slot.remove(slot.getItem().getCount());
            if (extracted.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack result = extracted.copy();
            this.moveItemStackTo(extracted, RitualAltarBlockEntity.SIZE, this.slots.size(), true);
            if (!extracted.isEmpty()) {
                player.drop(extracted, false);
                // 背包满了：这一份已经掉出来了，别再让 doClick 的 shift 循环继续消耗输入。
                return ItemStack.EMPTY;
            }
            return result;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int machineSlots = altar == null ? 0 : RitualAltarBlockEntity.SIZE;
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
