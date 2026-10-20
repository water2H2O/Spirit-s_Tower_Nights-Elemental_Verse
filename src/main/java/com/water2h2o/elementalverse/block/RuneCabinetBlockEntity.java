package com.water2h2o.elementalverse.block;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.element.DeclarationPlateItem;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.menu.RuneCabinetMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 符文柜：27 格，**通电时柜内完全不衰变**。
 *
 * <p>只吃**霜/冰元素**（直接喂元素瓶，不用先写成申明板）—— 原理是维持低温。
 * 霜 = 半日，冰 = 一日。
 */
public class RuneCabinetBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SIZE = 27;

    /** 每 20 tick（1 秒）扣 20 点能量；能量单位是 tick，所以 1 秒正好扣 1 秒。 */
    private static final int TICK_PER_ENERGY = 20;

    /** 能量上限：8 游戏日（冰 3 档单块是 4 日，最多叠两块）。 */
    public static final int MAX_ENERGY = 8 * 24000;

    private final net.minecraft.world.SimpleContainer items = new net.minecraft.world.SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            RuneCabinetBlockEntity.this.setChanged();
        }
    };

    /** 剩余能量（tick）。> 0 时不衰变。 */
    private int energy;

    /** 这一轮充满时有多少 tick —— 界面用它算电量条比例。 */
    private int energyMax = 1;

    private int tickCounter;

    public RuneCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(com.water2h2o.elementalverse.registry.EVBlockEntities.RUNE_CABINET.get(), pos, state);
    }

    // ---------------------------------------------------------------- 供能

    public boolean isCharged() {
        return energy > 0;
    }

    public int getEnergySeconds() {
        return energy / 20;
    }

    /** 满电上限（秒）—— 界面的电量条分母。 */
    public int getMaxEnergySeconds() {
        return Math.max(1, energyMax / 20);
    }

    /**
     * 吃一块申明板。
     *
     * @return 吃下去了返回 true（不是霜/冰的申明一律拒绝）
     */
    public boolean feed(ItemStack stack) {
        ElementType type = com.water2h2o.elementalverse.element.ElementVialItem.getElement(stack);
        if (type != ElementType.ICE && type != ElementType.FROST) {
            return false;
        }
        // 霜 = 半日，冰 = 一日（和小包燃料的 2:1 一致）。喂多个会累加，封顶 8 日。
        int ticks = type == ElementType.ICE ? 24000 : 12000;
        energy = Math.min(MAX_ENERGY, energy + ticks);
        // 冷却条的分母永远取上限，喂多块不会把条撑爆
        energyMax = MAX_ENERGY;
        setChanged();
        return true;
    }

    /** 每 20 tick 扣 20 点能量（能量单位是 tick）；不是每 20 tick 只扣 1 点。 */
    public void serverTick() {
        if (energy <= 0) {
            return;
        }
        if (++tickCounter >= TICK_PER_ENERGY) {
            tickCounter = 0;
            int before = energy;
            energy = Math.max(0, energy - TICK_PER_ENERGY);
            if (energy != before) {
                setChanged();
            }
        }
    }

    // ---------------------------------------------------------------- Container

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return items.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return items.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setItem(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(com.water2h2o.elementalverse.util.EVLang.BLOCK_RUNE_CABINET);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RuneCabinetMenu(containerId, inventory, this);
    }

    // ---------------------------------------------------------------- 存档

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy);
        tag.putInt("EnergyMax", energyMax);
        var list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = items.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", stack.save(registries));
                list.add(entry);
            }
        }
        tag.put("Items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getInt("Energy");
        energyMax = Math.max(1, tag.getInt("EnergyMax"));
        items.clearContent();
        var list = tag.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < SIZE) {
                items.setItem(slot, ItemStack.parseOptional(registries, entry.getCompound("Item")));
            }
        }
    }

    public void dropContents(net.minecraft.world.level.Level level, BlockPos pos) {
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = items.getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }
}
