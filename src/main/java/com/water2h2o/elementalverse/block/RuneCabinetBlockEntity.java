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
 * <p>作者定：只吃**霜/冰**的申明 —— 原理是维持低温。
 * 霜/冰的元素值档位越高，维持越久。
 */
public class RuneCabinetBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SIZE = 27;

    /** 每 20 tick 消耗 1 点能量。 */
    private static final int TICK_PER_ENERGY = 20;

    private final net.minecraft.world.SimpleContainer items = new net.minecraft.world.SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            RuneCabinetBlockEntity.this.setChanged();
        }
    };

    /** 剩余能量（tick）。> 0 时不衰变。 */
    private int energy;

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

    /**
     * 吃一块申明板。
     *
     * @return 吃下去了返回 true（不是霜/冰的申明一律拒绝）
     */
    public boolean feed(ItemStack plate) {
        Declaration declaration = DeclarationPlateItem.getDeclaration(plate);
        if (!declaration.isDefined()) {
            return false;
        }
        ElementType element = declaration.element();
        boolean cold = element == ElementType.FROST || element == ElementType.ICE;
        if (!cold) {
            return false;
        }
        // 霜：0.5 / 1 / 2 游戏日；冰：同档 x2
        int days = switch (declaration.tier()) {
            case 3 -> 2;
            case 2 -> 1;
            default -> 1;
        };
        double scale = switch (declaration.tier()) {
            case 3 -> 2.0D;
            case 2 -> 1.0D;
            default -> 0.5D;
        };
        int ticks = (int) (days * 24000L * scale * (element == ElementType.ICE ? 2 : 1));
        energy = Math.min(ticks, days * 24000 * 4);
        setChanged();
        return true;
    }

    /** 每 20 tick 扣 1 点能量。 */
    public void serverTick() {
        if (energy <= 0) {
            return;
        }
        if (++tickCounter >= TICK_PER_ENERGY) {
            tickCounter = 0;
            energy--;
            setChanged();
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
