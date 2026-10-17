package com.water2h2o.elementalverse.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.water2h2o.elementalverse.registry.EVDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * 元素小包的内容物逻辑（9 格 + 冷却剂）。
 *
 * <p>作者 2026-10-17 的设计：
 * <ul>
 *   <li>小包可以**存放多种元素材料**，方便随身带着做东西</li>
 *   <li>里面放着**霜**或**冰**时，可以**消耗它们来减缓其它东西的变质**
 *       —— 霜 ×0.8、冰 ×0.5</li>
 * </ul>
 *
 * <p>冷却剂不占用独立槽位：**包里只要有霜/冰就自动生效**，优先消耗霜（便宜的先用），
 * 霜用完了才动冰。这样不需要额外的界面，3×3 就够。
 */
public final class PouchContents {

    /** 容量。与密封仓、发射器同为 9 格。 */
    public static final int SIZE = 9;

    /** 冷却剂的消耗间隔（tick）。60 秒消耗一个。 */
    public static final int COOLANT_BURN_TICKS = 1200;

    private PouchContents() {
    }

    /** 读内容；没有组件就是空的（补齐到 9 格，保证位置稳定）。 */
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
        boolean empty = items.stream().allMatch(ItemStack::isEmpty);
        if (empty) {
            pouch.remove(EVDataComponents.POUCH_CONTENTS.get());
        } else {
            pouch.set(EVDataComponents.POUCH_CONTENTS.get(), new ArrayList<>(items));
        }
    }

    /**
     * 当前冷却系数：包里有没有霜/冰。
     *
     * @return 1.0 = 没有冷却剂；0.8 = 有霜；0.5 = 有冰（冰优先，取更好的那个）
     */
    public static double coolingMultiplier(List<ItemStack> items) {
        boolean frost = false;
        for (ItemStack stack : items) {
            ElementType type = ElementVialItem.getElement(stack);
            if (type == ElementType.ICE) {
                return 0.5D;
            }
            if (type == ElementType.FROST) {
                frost = true;
            }
        }
        return frost ? 0.8D : 1.0D;
    }

    /**
     * 消耗一个冷却剂：优先霜，其次冰。
     *
     * @return 真的消耗掉了返回 true
     */
    public static boolean consumeCoolant(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            if (ElementVialItem.getElement(items.get(i)) == ElementType.FROST) {
                return shrink(items, i);
            }
        }
        for (int i = 0; i < items.size(); i++) {
            if (ElementVialItem.getElement(items.get(i)) == ElementType.ICE) {
                return shrink(items, i);
            }
        }
        return false;
    }

    private static boolean shrink(List<ItemStack> items, int slot) {
        ItemStack stack = items.get(slot);
        stack.shrink(1);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        return true;
    }

    /** 这个堆叠是不是在充当冷却剂（冷却剂本身不参与衰变，它已经被"花掉"了）。 */
    public static boolean isCoolant(ItemStack stack) {
        ElementType type = ElementVialItem.getElement(stack);
        return type == ElementType.FROST || type == ElementType.ICE;
    }

    /** 里面还有多少个冷却剂（霜 + 冰）。 */
    public static int coolantCount(List<ItemStack> items) {
        int count = 0;
        for (ItemStack stack : items) {
            if (isCoolant(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * 里面有没有**需要被保护的东西**（冷却剂以外、且会衰变的元素）。
     *
     * <p>用来避免"空包也在烧冷却剂"这种纯浪费：没东西可保就不消耗。
     */
    public static boolean hasProtectedContents(List<ItemStack> items) {
        for (ItemStack stack : items) {
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
