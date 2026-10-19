package com.water2h2o.elementalverse.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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
 * 元素小包的界面：3×3 共 9 格 + 右侧独立燃料格。
 *
 * <p>内容存在物品的 {@code pouch_contents} 组件里。打开时读出来铺进界面，
 * **关闭时写回去** —— 这是"物品里的容器"的常规做法。
 *
 * <p>存放格只收装着元素的载体；打开期间服务端暂停这个小包的衰变，
 * 否则关闭时会把打开时的旧快照写回，造成进度回退。
 */
public class PouchMenu extends AbstractContainerMenu {

    private static final int GRID_LEFT = 62;
    private static final int GRID_TOP = 17;
    private static final int CELL = 18;

    /** 燃料格的位置：3×3 右侧的空白处（界面贴图里专门开的一个槽）。 */
    private static final int FUEL_LEFT = 134;
    private static final int FUEL_TOP = 17;

    /**
     * 正在被界面打开的小包。服务端在界面打开期间暂停这个小包的衰变/烧冷却，
     * 否则关闭时会把打开时的旧快照写回去，造成进度回退。
     */
    private static final Set<ItemStack> OPEN_POUCHES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private final ItemStack pouch;
    private final SimpleContainer container;

    /** 这个小包是不是正被某个玩家打开。 */
    public static boolean isOpen(ItemStack pouch) {
        return OPEN_POUCHES.contains(pouch);
    }

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

        // 3×3 存放格：只收装着元素的载体（也防止把小包装进自己）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, col + row * 3,
                        GRID_LEFT + col * CELL, GRID_TOP + row * CELL) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return PouchContents.isStorable(stack);
                    }
                });
            }
        }

        // 服务端打开期间暂停这个包的衰变（关闭时写回旧快照会回档）
        if (!playerInventory.player.level().isClientSide()) {
            OPEN_POUCHES.add(pouch);
        }

        // 独立的燃料格（作者要求：像熔炉/酿造台那样单独一格）
        this.addSlot(new Slot(container, PouchContents.FUEL_SLOT, FUEL_LEFT, FUEL_TOP) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PouchContents.isCoolant(stack);
            }
        });

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
        } else if (!this.moveItemStackTo(inSlot, 0, PouchContents.STORAGE, false)) {
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
        OPEN_POUCHES.remove(pouch);
        if (player.level().isClientSide()) {
            return;
        }
        // 写回物品的数据组件
        List<ItemStack> items = new ArrayList<>(PouchContents.SIZE);
        for (int i = 0; i < PouchContents.SIZE; i++) {
            items.add(this.container.getItem(i));
        }
        PouchContents.set(pouch, items);

        // 关界面时把背包里同元素、不同剩余时间的堆叠并一次
        com.water2h2o.elementalverse.element.ElementStackMerger.mergeAll(player);

        // 收口的声音（作者要求：大多数行为都给音效）
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.1F);
    }
}
