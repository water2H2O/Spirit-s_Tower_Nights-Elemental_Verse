package com.water2h2o.elementalverse.util;

import com.water2h2o.elementalverse.element.ElementType;

/**
 * 翻译键常量。
 *
 * <p>集中在一处，避免把一个键在三个文件里拼出三种写法 —— 那种错误在游戏里只表现为
 * 「显示成一串 raw key」，排查起来很费时间。
 */
public final class EVLang {

    public static final String CREATIVE_TAB = "itemGroup.elemental_verse.main";

    // 物品
    public static final String ITEM_VIAL = "item.elemental_verse.element_vial";
    public static final String ITEM_REFRACTION_PRISM = "item.elemental_verse.refraction_prism";
    public static final String ITEM_SHADOW_BOTTLE = "item.elemental_verse.shadow_bottle";
    public static final String BLOCK_DEAD_MATTER = "block.elemental_verse.dead_matter";

    // tooltip
    public static final String TOOLTIP_EMPTY_VIAL = "tooltip.elemental_verse.empty_vial";
    public static final String TOOLTIP_STAGE_PREFIX = "tooltip.elemental_verse.stage.";
    public static final String TOOLTIP_EXACT_SECONDS = "tooltip.elemental_verse.exact_seconds";
    public static final String TOOLTIP_NO_DECAY = "tooltip.elemental_verse.no_decay";
    public static final String TOOLTIP_DEBUG_DISABLED = "tooltip.elemental_verse.decay_disabled";

    // 配置界面
    public static final String CONFIG_TITLE = "elemental_verse.configuration.title";

    /** 「土元素」这类单个元素形态的名字。 */
    public static String element(ElementType type) {
        return "element.elemental_verse." + type.id();
    }

    /** 元素瓶的完整显示名，例如「水元素瓶」。 */
    public static String vial(ElementType type) {
        return "item.elemental_verse.element_vial." + type.id();
    }

    public static String config(String option) {
        return "elemental_verse.configuration." + option;
    }

    private EVLang() {
    }
}
