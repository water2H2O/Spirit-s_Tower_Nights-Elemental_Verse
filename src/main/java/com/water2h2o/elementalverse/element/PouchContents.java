package com.water2h2o.elementalverse.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.water2h2o.elementalverse.registry.EVDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * 元素小包的内容物逻辑（9 格存放 + 1 格燃料）。
 *
 * <p>作者 2026-10-17 的设计：
 * <ul>
 *   <li>小包可以**存放多种元素材料**，方便随身带着做东西；
 *       存放格只收装着元素的载体（空瓶、工具、小包自己都放不进去）</li>
 *   <li>另有一个**独立的燃料格**（像熔炉/酿造台那样），只能放霜或冰；
 *       放进去后会被消耗，换取其它内容物更慢的变质速度</li>
 * </ul>
 *
 * <p>系数：霜 ×0.8、冰 ×0.5（冰更好，同时有优先用冰的系数）。
 * 消耗顺序反过来：**先耗霜、后耗冰**（便宜的先用）。
 */
public final class PouchContents {

    /** 存放格数。 */
    public static final int STORAGE = 9;

    /** 燃料格（第 10 格）。 */
    public static final int FUEL_SLOT = STORAGE;

    /** 总格数 = 9 存放 + 1 燃料。 */
    public static final int SIZE = STORAGE + 1;

    /** 霜的消耗间隔：30 秒一个（一个霜 = 半个冰）。 */
    public static final int FROST_BURN_TICKS = 600;

    /** 冰的消耗间隔：60 秒一个。 */
    public static final int ICE_BURN_TICKS = 1200;

    /** 燃料格里那种冷却剂的消耗间隔（tick）；没有冷却剂返回 0。 */
    public static int burnInterval(List<ItemStack> items) {
        ElementType type = ElementVialItem.getElement(items.get(FUEL_SLOT));
        if (type == ElementType.ICE) {
            return ICE_BURN_TICKS;
        }
        if (type == ElementType.FROST) {
            return FROST_BURN_TICKS;
        }
        return 0;
    }

    /**
     * 小包存放格只收**真正装着元素**的载体。
     *
     * <p>空瓶、工具、小包自己都不能放进存放格；燃料格仍然只收霜/冰。
     * 这样既符合“元素小包”的定位，也堵死了“把小包装进自己里面”的递归组件。
     */
    public static boolean isStorable(ItemStack stack) {
        return !stack.isEmpty() && ElementCarrier.isCarrier(stack)
                && !ElementVialItem.getElement(stack).isEmpty();
    }

    private PouchContents() {
    }

    /** 读内容；没有组件就是空的（补齐到 SIZE 格，保证位置稳定）。 */
    public static List<ItemStack> get(ItemStack pouch) {
        List<ItemStack> stored = pouch.get(EVDataComponents.POUCH_CONTENTS.get());
        List<ItemStack> out = new ArrayList<>(Collections.nCopies(SIZE, ItemStack.EMPTY));
        if (stored != null) {
            for (int i = 0; i < Math.min(SIZE, stored.size()); i++) {
                out.set(i, stored.get(i));
            }
        }
        return out;
    }

    /** 写回内容（全空就把组件摘掉，省存档体积）。 */
    public static void set(ItemStack pouch, List<ItemStack> items) {
        // 最后一道保险：界面已经禁止把小包装进自己，但旧存档 / 命令仍可能塞进来。
        // 组件里一旦包含自己，保存/同步时会递归序列化，直接 StackOverflow。
        List<ItemStack> safe = new ArrayList<>(items.size());
        for (ItemStack stack : items) {
            safe.add(stack == pouch ? ItemStack.EMPTY : stack);
        }
        boolean empty = safe.stream().allMatch(ItemStack::isEmpty);
        if (empty) {
            pouch.remove(EVDataComponents.POUCH_CONTENTS.get());
        } else {
            pouch.set(EVDataComponents.POUCH_CONTENTS.get(), safe);
        }
    }

    // ------------------------------------------------------------ 燃料格

    /** 这一格是不是燃料格。 */
    public static int fuelSlot() {
        return FUEL_SLOT;
    }

    /**
     * 当前冷却系数，只看**燃料格**。
     *
     * @return 1.0 = 没有冷却剂；0.8 = 霜；0.5 = 冰（同时有取更好的）
     */
    public static double coolingMultiplier(List<ItemStack> items) {
        ItemStack fuel = items.get(FUEL_SLOT);
        ElementType type = ElementVialItem.getElement(fuel);
        if (type == ElementType.ICE) {
            return 0.5D;
        }
        if (type == ElementType.FROST) {
            return 0.8D;
        }
        return 1.0D;
    }

    /** 燃料格里还有多少个冷却剂。 */
    public static int coolantCount(List<ItemStack> items) {
        ItemStack fuel = items.get(FUEL_SLOT);
        return isCoolant(fuel) ? fuel.getCount() : 0;
    }

    /**
     * 消耗一个冷却剂：优先霜，其次冰。（同一格内两种不能混装，所以实际只动这一格）
     *
     * @return 真的消耗掉了返回 true
     */
    public static boolean consumeCoolant(List<ItemStack> items) {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (!isCoolant(fuel)) {
            return false;
        }
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(FUEL_SLOT, ItemStack.EMPTY);
        }
        return true;
    }

    /** 这个堆叠能不能当燃料（霜或冰的容器）。 */
    public static boolean isCoolant(ItemStack stack) {
        ElementType type = ElementVialItem.getElement(stack);
        return type == ElementType.FROST || type == ElementType.ICE;
    }

    // ------------------------------------------------------------ 存放格

    /**
     * 存放格里有没有**需要被保护的东西**（会衰变的元素）。
     *
     * <p>用来避免"空包也在烧冷却剂"这种纯浪费：没东西可保就不消耗。
     */
    public static boolean hasProtectedContents(List<ItemStack> items) {
        for (int i = 0; i < STORAGE; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || isCoolant(stack)) {
                continue;
            }
            if (ElementCarrier.isCarrier(stack) && DecayEngine.decays(ElementVialItem.getElement(stack))) {
                return true;
            }
        }
        return false;
    }
}
