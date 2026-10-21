package com.water2h2o.elementalverse.element;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 修饰材料：贴在申明板上的一层附加物。
 *
 * <p>作者 2026-10-21 定：
 * <ul>
 *   <li>粘液球 = 默认，两段法术同时生效</li>
 *   <li>红石粉 = 两段之间延迟 0.5 秒</li>
 *   <li>末影珍珠 = 弹射物形态改为"指哪打哪"，没有飞行时间</li>
 * </ul>
 */
public final class Modifiers {

    public static final String NONE = "";
    public static final String DELAY = "delay";
    public static final String INSTANT = "instant";

    /** 延迟多久（tick）。 */
    public static final int DELAY_TICKS = 10;

    /** 不是修饰材料返回 null。 */
    public static String of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(Items.SLIME_BALL)) {
            return NONE;
        }
        if (stack.is(Items.REDSTONE)) {
            return DELAY;
        }
        if (stack.is(Items.ENDER_PEARL)) {
            return INSTANT;
        }
        return null;
    }

    public static boolean isModifier(ItemStack stack) {
        return of(stack) != null;
    }

    private Modifiers() {
    }
}
