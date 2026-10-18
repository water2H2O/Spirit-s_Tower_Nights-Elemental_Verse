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
 * <p>布局沿用发射器的 3×3 底图（省一张贴图，玩家也熟）：
 * <pre>
 *   上排三个格子：元素投入口 1 / 2 / 3
 *   左中：申明板      右中：产出
 * </pre>
 *
 * <p>**第 2、3 口本期不参与计算**：可以放，但界面会在对应位置给出提示
 * （提示由贴图/文字负责，见 `docs/分阶段/04-仪式台与申明板.md` 第九节第 3 条）。
 */
public class RitualAltarMenu extends AbstractContainerMenu {

    private static final int CELL = 18;
    private static final int GRID_LEFT = 62;
    private static final int GRID_TOP = 17;

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
            // 三个元素投入口（上排）
            for (int i = 0; i < 3; i++) {
                this.addSlot(new Slot(altar, i, GRID_LEFT + i * CELL, GRID_TOP));
            }
            // 申明板
            this.addSlot(new Slot(altar, RitualAltarBlockEntity.SLOT_PLATE, GRID_LEFT, GRID_TOP + CELL));
            // 工具槽（剑）：放在申明板和产出中间
            this.addSlot(new Slot(altar, RitualAltarBlockEntity.SLOT_TOOL,
                    GRID_LEFT + CELL, GRID_TOP + CELL));
            // 产出：只能拿，不能放
            this.addSlot(new Slot(altar, RitualAltarBlockEntity.SLOT_OUTPUT,
                    GRID_LEFT + 2 * CELL, GRID_TOP + CELL) {
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
                        8 + col * CELL, 84 + row * CELL));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * CELL, 142));
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
        return altar != null && altar.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
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
