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
    /** 龙息：命中点留下一滩魔法烟雾。 */
    public static final String BREATH = "breath";
    /** 金锭：冷却 -30%，最多叠两层。 */
    public static final String COOLDOWN = "cooldown";

    /** 金锭最多叠几层。 */
    public static final int COOLDOWN_MAX_STACK = 2;
    /** 每层减多少冷却。 */
    public static final double COOLDOWN_PER_STACK = 0.30D;

    /** 修饰是逗号分隔的集合，统计某个修饰出现了几次。 */
    public static int count(String modifiers, String id) {
        if (modifiers == null || modifiers.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (String part : modifiers.split(",")) {
            if (part.trim().equals(id)) {
                n++;
            }
        }
        return n;
    }

    /** 有没有某个修饰。 */
    public static boolean has(String modifiers, String id) {
        return count(modifiers, id) > 0;
    }

    /** 合并两组修饰（用于拼贴），最多保留 3 条。 */
    public static String merge(String a, String b) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String src : new String[] {a, b}) {
            if (src == null || src.isEmpty()) {
                continue;
            }
            for (String part : src.split(",")) {
                String id = part.trim();
                if (!id.isEmpty() && out.size() < 3) {
                    out.add(id);
                }
            }
        }
        return String.join(",", out);
    }

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
        if (stack.is(Items.GOLD_INGOT)) {
            return COOLDOWN;
        }
        return null;
    }

    public static boolean isModifier(ItemStack stack) {
        return of(stack) != null;
    }

    /**
     * 把修饰串翻译成一行可读文字（给悬浮框用）。
     *
     * @return 没有修饰时返回空组件
     */
    public static net.minecraft.network.chat.Component describe(String modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return net.minecraft.network.chat.Component.empty();
        }
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String part : modifiers.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                counts.merge(id, 1, Integer::sum);
            }
        }
        net.minecraft.network.chat.MutableComponent out = net.minecraft.network.chat.Component.empty();
        boolean first = true;
        for (var entry : counts.entrySet()) {
            if (!first) {
                out.append(" · ");
            }
            first = false;
            out.append(net.minecraft.network.chat.Component.translatable(
                    "modifier.elemental_verse." + entry.getKey()));
            if (entry.getValue() > 1) {
                out.append(" ×" + entry.getValue());
            }
        }
        return out;
    }

    private Modifiers() {
    }
}
