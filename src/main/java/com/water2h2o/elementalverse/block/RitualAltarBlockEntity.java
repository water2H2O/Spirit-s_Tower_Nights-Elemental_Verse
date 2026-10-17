package com.water2h2o.elementalverse.block;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.element.DeclarationPlateItem;
import com.water2h2o.elementalverse.element.ElementCarrier;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.menu.RitualAltarMenu;
import com.water2h2o.elementalverse.registry.EVItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 仪式台的方块实体。
 *
 * <p>**本期只做"定义"模式**：投入元素 + 空申明板 → 产出写好的申明板。
 * 界面里第 2、3 个投入口也在，但**不参与计算**（作者定：先把三口结构立住）。
 *
 * <p>产出格自动计算（像工作台），玩家从产出格拿走的瞬间才扣材料 ——
 * 这样不需要额外的按钮和网络包。
 */
public class RitualAltarBlockEntity extends BlockEntity implements Container, MenuProvider {

    /** 0/1/2 = 三个元素投入口，3 = 申明板，4 = 产出。 */
    public static final int SLOT_ELEMENT_A = 0;
    public static final int SLOT_ELEMENT_B = 1;
    public static final int SLOT_ELEMENT_C = 2;
    public static final int SLOT_PLATE = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SIZE = 5;

    /** 第 2、3 口本期不参与计算。 */
    public static final int ACTIVE_ELEMENT_SLOT = SLOT_ELEMENT_A;

    private final SimpleContainer items = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            RitualAltarBlockEntity.this.setChanged();
            RitualAltarBlockEntity.this.refreshResult();
        }
    };

    public RitualAltarBlockEntity(BlockPos pos, BlockState state) {
        super(com.water2h2o.elementalverse.registry.EVBlockEntities.RITUAL_ALTAR.get(), pos, state);
    }

    // ---------------------------------------------------------------- 产出计算

    /**
     * 重算产出格。
     *
     * <p>规则（本期）：空申明板 + 第 1 口里的元素 → 写好的申明板。
     * 档位由第 1 口的**数量**决定：1 / 3 / 9 → 档 1 / 2 / 3。
     */
    public void refreshResult() {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack output = computeResult();
        if (!ItemStack.matches(output, items.getItem(SLOT_OUTPUT))) {
            items.setItem(SLOT_OUTPUT, output);
        }
    }

    /** 按当前投入算出应该产出什么（不改动任何格子）。 */
    public ItemStack computeResult() {
        ItemStack plate = items.getItem(SLOT_PLATE);
        if (plate.isEmpty() || !plate.is(EVItems.DECLARATION_PLATE.get())) {
            return ItemStack.EMPTY;
        }
        // 已经是写好的板子 → 不能再写一遍
        if (DeclarationPlateItem.getDeclaration(plate).isDefined()) {
            return ItemStack.EMPTY;
        }

        ItemStack elements = items.getItem(ACTIVE_ELEMENT_SLOT);
        if (elements.isEmpty() || !ElementCarrier.isCarrier(elements)) {
            return ItemStack.EMPTY;
        }
        ElementType element = ElementVialItem.getElement(elements);
        if (element.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int tier = Declaration.tierOf(elements.getCount());
        if (tier <= 0) {
            return ItemStack.EMPTY;
        }

        // 第三个口决定「态」；第 2 口（辅料）本期还不参与
        ElementType stateElement = ElementType.EMPTY;
        int stateTier = 0;
        ItemStack stateStack = items.getItem(SLOT_ELEMENT_C);
        if (!stateStack.isEmpty() && ElementCarrier.isCarrier(stateStack)) {
            ElementType candidate = ElementVialItem.getElement(stateStack);
            int candidateTier = Declaration.tierOf(stateStack.getCount());
            if (!candidate.isEmpty() && candidateTier > 0) {
                stateElement = candidate;
                stateTier = candidateTier;
            }
        }

        // 第二个口 = 辅料（与主料一起构成反应的输入向量）
        ElementType reagentElement = ElementType.EMPTY;
        int reagentTier = 0;
        ItemStack reagentStack = items.getItem(SLOT_ELEMENT_B);
        if (!reagentStack.isEmpty() && ElementCarrier.isCarrier(reagentStack)) {
            ElementType candidate = ElementVialItem.getElement(reagentStack);
            int candidateTier = Declaration.tierOf(reagentStack.getCount());
            if (!candidate.isEmpty() && candidateTier > 0) {
                reagentElement = candidate;
                reagentTier = candidateTier;
            }
        }

        // 写入时就把"这是什么魔法"算出来存好 —— 客户端没有反应表，
        // 不能指望它在运行时再匹配一次。
        String magicName = "";
        var preview = DeclarationPlateItem.written(element, tier, stateElement, stateTier,
                reagentElement, reagentTier);
        var declaration = DeclarationPlateItem.getDeclaration(preview);
        var matched = com.water2h2o.elementalverse.reaction.ReactionManager.match(declaration.inputValue());
        if (matched.isPresent()) {
            magicName = matched.get().name();
        }

        return DeclarationPlateItem.written(element, tier, stateElement, stateTier,
                reagentElement, reagentTier, magicName);
    }

    /** 玩家把产出拿走时，按档位扣材料。 */
    public void consumeInputs() {
        ItemStack plate = items.getItem(SLOT_PLATE);
        ItemStack elements = items.getItem(ACTIVE_ELEMENT_SLOT);
        if (plate.isEmpty() || elements.isEmpty()) {
            return;
        }

        int tier = Declaration.tierOf(elements.getCount());
        int cost = switch (tier) {
            case 3 -> 9;
            case 2 -> 3;
            case 1 -> 1;
            default -> 0;
        };
        if (cost <= 0) {
            return;
        }

        plate.shrink(1);
        elements.shrink(Math.min(cost, elements.getCount()));
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
        // 产出格被拿走 → 扣材料
        boolean takingOutput = slot == SLOT_OUTPUT;
        ItemStack result = items.removeItem(slot, amount);
        if (takingOutput && !result.isEmpty()) {
            consumeInputs();
            refreshResult();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return items.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setItem(slot, stack);
        if (slot != SLOT_OUTPUT) {
            setChanged();
            refreshResult();
        }
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_ELEMENT_A, SLOT_ELEMENT_B, SLOT_ELEMENT_C -> ElementCarrier.isCarrier(stack)
                    && !ElementVialItem.getElement(stack).isEmpty();
            case SLOT_PLATE -> stack.is(EVItems.DECLARATION_PLATE.get());
            default -> false;
        };
    }

    // ---------------------------------------------------------------- MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.translatable(com.water2h2o.elementalverse.util.EVLang.BLOCK_RITUAL_ALTAR);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RitualAltarMenu(containerId, inventory, this, new ContainerData() {
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
        });
    }

    // ---------------------------------------------------------------- 存档

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
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

    /** 破坏时把东西丢出来（由方块调用）。 */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = items.getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }
}
