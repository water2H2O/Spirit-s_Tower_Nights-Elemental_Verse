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
    /** 回响碎片：第二段在 2 秒后原地重放一次。 */
    public static final String ECHO = "echo";
    /** 糖：弹得又快又远，但会散。 */
    public static final String SWIFT = "swift";
    /** 紫颂果：穿墙，只打生物。 */
    public static final String PHASE = "phase";
    /** 火药：命中后再等 1.5 秒才结算。 */
    public static final String FUSE = "fuse";
    /** 荧石粉：命中目标发光。 */
    public static final String GLOW = "glow";
    /** 龙息：命中点留下 5 秒的元素雾。 */
    public static final String BREATH = "breath";

    /** 延迟多久（tick）。 */
    public static final int DELAY_TICKS = 10;
    /** 回响重放的间隔。 */
    public static final int ECHO_TICKS = 40;
    /** 火药引信。 */
    public static final int FUSE_TICKS = 30;

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
        if (stack.is(Items.ECHO_SHARD)) {
            return ECHO;
        }
        if (stack.is(Items.SUGAR)) {
            return SWIFT;
        }
        if (stack.is(Items.CHORUS_FRUIT)) {
            return PHASE;
        }
        if (stack.is(Items.GUNPOWDER)) {
            return FUSE;
        }
        if (stack.is(Items.GLOWSTONE_DUST)) {
            return GLOW;
        }
        if (stack.is(Items.DRAGON_BREATH)) {
            return BREATH;
        }
        return null;
    }

    public static boolean isModifier(ItemStack stack) {
        return of(stack) != null;
    }

    private Modifiers() {
    }
}
