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
    public static final String ITEM_DEAD_DUST = "item.elemental_verse.dead_dust";
    public static final String BLOCK_DEAD_MATTER = "block.elemental_verse.dead_matter";
    public static final String ITEM_POUCH = "item.elemental_verse.element_pouch";
    public static final String TOOLTIP_POUCH_COUNT = "tooltip.elemental_verse.pouch_count";
    public static final String TOOLTIP_POUCH_COOLING = "tooltip.elemental_verse.pouch_cooling";
    public static final String TOOLTIP_POUCH_IDLE = "tooltip.elemental_verse.pouch_idle";

    // tooltip
    public static final String TOOLTIP_EMPTY_VIAL = "tooltip.elemental_verse.empty_vial";
    public static final String TOOLTIP_STAGE_PREFIX = "tooltip.elemental_verse.stage.";
    public static final String TOOLTIP_EXACT_SECONDS = "tooltip.elemental_verse.exact_seconds";
    public static final String TOOLTIP_NO_DECAY = "tooltip.elemental_verse.no_decay";
    public static final String TOOLTIP_DEBUG_DISABLED = "tooltip.elemental_verse.decay_disabled";

    // 采集失败的提示（只有"条件不满足"才提示）
    public static final String MSG_NO_LIGHT = "message.elemental_verse.no_light";
    public static final String MSG_TOO_BRIGHT = "message.elemental_verse.too_bright";
    public static final String MSG_NO_WIND = "message.elemental_verse.no_wind";
    public static final String MSG_WEAK_WIND = "message.elemental_verse.weak_wind";

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

    /** 申明板与仪式台。 */
    public static final String BLOCK_RITUAL_ALTAR = "block.elemental_verse.ritual_altar";
    public static final String MSG_NOT_ENOUGH = "message.elemental_verse.not_enough";
    public static final String PLATE_WRITTEN = "item.elemental_verse.declaration_plate.written";
    public static final String PLATE_BLANK_HINT = "tooltip.elemental_verse.plate.blank";
    public static final String PLATE_ELEMENT = "tooltip.elemental_verse.plate.element";
    public static final String PLATE_TIER = "tooltip.elemental_verse.plate.tier";
    public static final String PLATE_OPERATION = "tooltip.elemental_verse.plate.operation";

    public static String config(String option) {
        return "elemental_verse.configuration." + option;
    }

    private EVLang() {
    }
}
