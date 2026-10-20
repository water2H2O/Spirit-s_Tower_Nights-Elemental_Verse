package com.water2h2o.elementalverse.block;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.element.DeclarationPlateItem;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.menu.RuneCabinetMenu;
import com.water2h2o.elementalverse.registry.EVItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
 * <p>喂冷源两种写法都收：**元素瓶**（霜/冰）或**已写好的申明板**。
 * 原理是维持低温；霜 = 半日、冰 = 一日，申明板按档位放大（霜 0.5/1/2 日、冰 1/2/4 日）。
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
     * 吃一份冷源：元素瓶（霜/冰）或已写好的霜/冰申明板。
     *
     * <p>申明板按档位放大（霜 0.5/1/2 日、冰 1/2/4 日），元素瓶按 1 档算。
     *
     * @return 吃下去了返回 true（不是霜/冰一律拒绝）
     */
    public boolean feed(ItemStack stack) {
        ElementType type = ElementVialItem.getElement(stack);
        int tier = 1;

        // 设计文档（04 第九节）写的是喂“声明霜/冰的申明板”；
        // 这里同时保留直接喂元素瓶的旧行为，两种都支持。
        if (stack.is(EVItems.DECLARATION_PLATE.get())) {
            Declaration declaration = DeclarationPlateItem.getDeclaration(stack);
            if (declaration.isDefined()) {
                type = declaration.element();
                tier = Math.max(1, Math.min(3, declaration.tier()));
            }
        }

        // 判定放得很宽：只要带**负火**（冰 = 火-1.0、霜 = 火-0.5）就算冷源。
        // 之前写死 `type == ICE || type == FROST`，任何"看着像冰"的新元素都会被挡在外面。
        if (!isCold(type)) {
            return false;
        }
        energy = Math.min(MAX_ENERGY, energy + energyTicks(type, tier));
        // 冷却条的分母永远取上限，喂多块不会把条撑爆
        energyMax = MAX_ENERGY;
        setChanged();
        return true;
    }

    /** 一份冷源能撑多少 tick：霜 0.5/1/2 日，冰 1/2/4 日。 */
    private static int energyTicks(ElementType type, int tier) {
        int level = Math.max(1, Math.min(3, tier));
        if (type == ElementType.ICE) {
            return switch (level) {
                case 3 -> 4 * 24000;
                case 2 -> 2 * 24000;
                default -> 24000;
            };
        }
        // 霜（以及以后任何带负火的冷源）按“半日”为 1 档基数
        return switch (level) {
            case 3 -> 2 * 24000;
            case 2 -> 24000;
            default -> 12000;
        };
    }

    /** 带负火的元素都算冷源（霜 / 冰，以及以后可能加的同类）。 */
    public static boolean isCold(ElementType type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        if (type == ElementType.ICE || type == ElementType.FROST) {
            return true;
        }
        return type.value().get(com.water2h2o.elementalverse.element.ElementAxis.FIRE) < 0.0D;
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
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            list.set(i, items.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, list, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getInt("Energy");
        energyMax = Math.max(1, tag.getInt("EnergyMax"));
        items.clearContent();
        var listTag = tag.getList("Items", 10);
        if (!listTag.isEmpty() && listTag.getCompound(0).contains("Item")) {
            // 兼容旧版存档：旧代码写的是 {Slot, Item:{...}} 嵌套格式。
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entry = listTag.getCompound(i);
                int slot = entry.getByte("Slot") & 255;
                if (slot < SIZE) {
                    items.setItem(slot, ItemStack.parseOptional(registries, entry.getCompound("Item")));
                }
            }
        } else {
            NonNullList<ItemStack> loaded = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, loaded, registries);
            for (int i = 0; i < SIZE; i++) {
                items.setItem(i, loaded.get(i));
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
