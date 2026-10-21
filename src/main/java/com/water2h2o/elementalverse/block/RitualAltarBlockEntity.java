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
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
 * <p>两种模式共用一套槽位：
 * <ul>
 *   <li><b>写入</b>：主料 + 辅料 + 态 + 空申明板 → 写好的申明板</li>
 *   <li><b>刻印</b>：已写好的申明板 + 剑 → 带申明的剑（板是模板，不消耗；剑会被消耗）</li>
 * </ul>
 *
 * <p>产出格自动计算（像工作台），玩家从产出格拿走的瞬间才扣材料 ——
 * 这样不需要额外的按钮和网络包。**破坏方块不会掉产出格**，否则可以靠拆台子白拿。
 */
public class RitualAltarBlockEntity extends BlockEntity implements Container, MenuProvider {

    /** 0/1/2 = 三个元素投入口，3 = 申明板，4 = 产出。 */
    public static final int SLOT_ELEMENT_A = 0;
    public static final int SLOT_ELEMENT_B = 1;
    public static final int SLOT_ELEMENT_C = 2;
    public static final int SLOT_PLATE = 3;
    public static final int SLOT_OUTPUT = 4;

    public static final int SIZE = 5;

    /** 主料口；辅料口/态口分别在 B / C。 */
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

    /** 相邻容器里的元素来源。owner 为 null 表示仪式台自己的第 1 口。 */
    private record ElementSource(ItemStack stack, Container owner, int slot) {
    }

    /** 按当前投入算出应该产出什么（不改动任何格子）。 */
    private static boolean isWrittenPlate(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(EVItems.DECLARATION_PLATE.get())
                && DeclarationPlateItem.getDeclaration(stack).isDefined();
    }

    /** 拼贴结果是不是"过于复杂"（超过 3 次修饰）。 */
    public boolean isTooComplex() {
        ItemStack a = items.getItem(SLOT_ELEMENT_A);
        ItemStack b = items.getItem(SLOT_ELEMENT_B);
        if (!isWrittenPlate(a) || !isWrittenPlate(b)) {
            return false;
        }
        return Declaration.chainCount(DeclarationPlateItem.getDeclaration(a),
                DeclarationPlateItem.getDeclaration(b)) > Declaration.MAX_MODIFIED;
    }

    public ItemStack computeResult() {
        // ---- 拼贴模式：料位与辅料位都放"写好的板" ----
        ItemStack firstPlate = items.getItem(SLOT_ELEMENT_A);
        ItemStack secondPlate = items.getItem(SLOT_ELEMENT_B);
        if (isWrittenPlate(firstPlate) && isWrittenPlate(secondPlate)) {
            Declaration first = DeclarationPlateItem.getDeclaration(firstPlate);
            Declaration second = DeclarationPlateItem.getDeclaration(secondPlate);
            int count = Declaration.chainCount(first, second);
            if (count > Declaration.MAX_MODIFIED) {
                return ItemStack.EMPTY;
            }
            String modifier = com.water2h2o.elementalverse.element.Modifiers
                    .of(items.getItem(SLOT_PLATE));
            if (modifier == null) {
                modifier = com.water2h2o.elementalverse.element.Modifiers.NONE;
            }
            // 名字各取一半：A 名 + B 名
            String magic = (first.magic() + " " + second.magic()).trim();
            Declaration chained = new Declaration(first.element(), first.tier(), first.operation(),
                    first.form(), first.stateElement(), first.stateTier(),
                    first.reagentElement(), first.reagentTier(), magic,
                    second, modifier, count);
            ItemStack out = new ItemStack(EVItems.DECLARATION_PLATE.get());
            DeclarationPlateItem.setDeclaration(out, chained);
            applyNameFromSlot(out);
            return out;
        }

        ItemStack plate = items.getItem(SLOT_PLATE);
        if (plate.isEmpty() || !plate.is(EVItems.DECLARATION_PLATE.get())) {
            return ItemStack.EMPTY;
        }
        // 已经写好的板子 → 不再重复写
        if (DeclarationPlateItem.getDeclaration(plate).isDefined()) {
            return ItemStack.EMPTY;
        }

        ElementSource source = findElementSource();
        if (source == null) {
            return ItemStack.EMPTY;
        }
        ItemStack elements = source.stack();
        ElementType element = ElementVialItem.getElement(elements);
        if (element.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int tier = Declaration.tierOf(elements.getCount());
        if (tier <= 0) {
            return ItemStack.EMPTY;
        }

        // 第三个口决定「态」
        ElementType stateElement = ElementType.EMPTY;
        int stateTier = 0;
        ItemStack stateStack = items.getItem(SLOT_ELEMENT_C);
        if (isElementStack(stateStack)) {
            stateElement = ElementVialItem.getElement(stateStack);
            stateTier = Declaration.tierOf(stateStack.getCount());
        }

        // 第二个口 = 辅料（与主料一起构成反应的输入向量）
        ElementType reagentElement = ElementType.EMPTY;
        int reagentTier = 0;
        ItemStack reagentStack = items.getItem(SLOT_ELEMENT_B);
        if (isElementStack(reagentStack)) {
            reagentElement = ElementVialItem.getElement(reagentStack);
            reagentTier = Declaration.tierOf(reagentStack.getCount());
        }

        // 写入时就把"这是什么魔法"算出来存好 —— 客户端没有反应表，
        // 不能指望它在运行时再匹配一次。
        // 注意：必须和释放时用同一套 bothSlotsSame 规则，否则会出现
        // "板子上写着洪流，但右键放不出来"的错位。
        boolean bothSlotsSame = reagentElement != null && !reagentElement.isEmpty()
                && reagentElement == element;
        String magicName = "";
        var preview = DeclarationPlateItem.written(element, tier, stateElement, stateTier,
                reagentElement, reagentTier);
        var declaration = DeclarationPlateItem.getDeclaration(preview);
        var matched = com.water2h2o.elementalverse.reaction.ReactionManager
                .match(declaration.inputValue(), bothSlotsSame);
        if (matched.isPresent()) {
            magicName = matched.get().name();
        }

        ItemStack result = DeclarationPlateItem.written(element, tier, stateElement, stateTier,
                reagentElement, reagentTier, magicName);
        applyNameFromSlot(result);
        return result;
    }

    /** 主料优先从第 1 口取；没有就按水平方向找相邻容器（最多取第一个可用）。 */
    private ElementSource findElementSource() {
        ItemStack main = items.getItem(ACTIVE_ELEMENT_SLOT);
        if (isElementStack(main)) {
            return new ElementSource(main, null, ACTIVE_ELEMENT_SLOT);
        }
        if (level == null) {
            return null;
        }
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (direction.getAxis().isVertical()) {
                continue;
            }
            var neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (!(neighbour instanceof Container container)) {
                continue;
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (isElementStack(stack)) {
                    return new ElementSource(stack, container, slot);
                }
            }
        }
        return null;
    }

    /** 真正能作为投入的元素载体：必须装着非空元素，空瓶不算。 */
    private static boolean isElementStack(ItemStack stack) {
        return !stack.isEmpty() && ElementCarrier.isCarrier(stack)
                && !ElementVialItem.getElement(stack).isEmpty();
    }

    /**
     * 拿走产出时扣材料。
     *
     * <p>写入模式扣：主料 1/3/9 + 辅料 1/3/9 + 态 1/3/9 + 一张空板。
     * 刻印模式扣工具槽里的剑；申明板是模板，**不消耗**。
     *
     * @return 扣成功返回 true；输入在产出计算之后变了返回 false，调用方不要把产出交出去
     */
    public boolean consumeInputs() {
        // 拼贴：吃两块板 + 一份修饰材料
        ItemStack firstPlate = items.getItem(SLOT_ELEMENT_A);
        ItemStack secondPlate = items.getItem(SLOT_ELEMENT_B);
        if (isWrittenPlate(firstPlate) && isWrittenPlate(secondPlate)) {
            if (Declaration.chainCount(DeclarationPlateItem.getDeclaration(firstPlate),
                    DeclarationPlateItem.getDeclaration(secondPlate)) > Declaration.MAX_MODIFIED) {
                return false;
            }
            firstPlate.shrink(1);
            secondPlate.shrink(1);
            shrinkSlot(SLOT_PLATE, 1);
            items.setChanged();
            return true;
        }

        ItemStack plate = items.getItem(SLOT_PLATE);
        if (plate.isEmpty()) {
            return false;
        }

        ElementSource source = findElementSource();
        if (source == null) {
            return false;
        }
        int mainCost = Declaration.costOfTier(Declaration.tierOf(source.stack().getCount()));
        if (mainCost <= 0) {
            return false;
        }
        int reagentCost = costOfSlot(SLOT_ELEMENT_B);
        int stateCost = costOfSlot(SLOT_ELEMENT_C);

        plate.shrink(1);
        source.stack().shrink(Math.min(mainCost, source.stack().getCount()));
        shrinkSlot(SLOT_ELEMENT_B, reagentCost);
        shrinkSlot(SLOT_ELEMENT_C, stateCost);


        if (source.owner() != null) {
            source.owner().setChanged();
        }
        items.setChanged();
        return true;
    }

    /** 界面上那个命名框里写的名字（空 = 不给名字）。 */
    private String customName = "";

    public void setCustomName(String name) {
        this.customName = name == null ? "" : name.trim();
        refreshResult();
    }

    public String getCustomName() {
        return customName;
    }

    /** 把命名框里的文字套到产物上。 */
    private void applyNameFromSlot(ItemStack result) {
        if (customName.isEmpty()) {
            return;
        }
        result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal(customName)
                        .withStyle(net.minecraft.ChatFormatting.WHITE));
    }

    /** 某个投入口当前档位需要扣的数量；空口返回 0。 */
    private int costOfSlot(int slot) {
        ItemStack stack = items.getItem(slot);
        if (!isElementStack(stack)) {
            return 0;
        }
        return Declaration.costOfTier(Declaration.tierOf(stack.getCount()));
    }

    private void shrinkSlot(int slot, int cost) {
        if (cost <= 0) {
            return;
        }
        ItemStack stack = items.getItem(slot);
        if (!stack.isEmpty()) {
            stack.shrink(Math.min(cost, stack.getCount()));
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
        if (slot != SLOT_OUTPUT) {
            return items.removeItem(slot, amount);
        }

        ItemStack preview = items.getItem(SLOT_OUTPUT);
        if (preview.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        // 相邻容器可能在没有 setChanged 的情况下变了；产出过期就重算并拒绝这一次。
        if (!ItemStack.matches(preview, computeResult())) {
            refreshResult();
            return ItemStack.EMPTY;
        }

        ItemStack result = preview.copy();
        if (result.getCount() > amount) {
            result.setCount(amount);
        }

        // 先扣材料，再让 refreshResult 算“下一份”的预览。若材料已不够，预览会清空。
        if (!consumeInputs()) {
            return ItemStack.EMPTY;
        }
        refreshResult();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        // 产出格不能走“无消耗”的旁路，否则漏斗可以白拿。
        if (slot == SLOT_OUTPUT) {
            return removeItem(slot, Integer.MAX_VALUE);
        }
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
            case SLOT_ELEMENT_A, SLOT_ELEMENT_B ->
                    isElementStack(stack) || stack.is(EVItems.DECLARATION_PLATE.get());
            case SLOT_ELEMENT_C -> isElementStack(stack);
            case SLOT_PLATE -> stack.is(EVItems.DECLARATION_PLATE.get())
                    || com.water2h2o.elementalverse.element.Modifiers.isModifier(stack);
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
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            list.set(i, items.getItem(i));
        }
        // 用 vanilla 的扁平 ItemStack 格式，保证 /data、结构方块与其它 mod 能读懂。
        ContainerHelper.saveAllItems(tag, list, registries);
        if (!customName.isEmpty()) {
            tag.putString("CustomName", customName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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
        customName = tag.getString("CustomName");
        refreshResult();
    }

    /**
     * 破坏时把东西丢出来（由方块调用）。
     *
     * <p>**不丢产出格**：产出还只是预览，没有扣材料；如果连它一起掉，
     * 玩家就能靠“放好材料 → 砸掉仪式台”白拿产物。
     */
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SIZE; i++) {
            if (i == SLOT_OUTPUT) {
                continue;
            }
            ItemStack stack = items.getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }
}
