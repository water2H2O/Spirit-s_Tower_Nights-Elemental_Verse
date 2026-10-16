package com.water2h2o.elementalverse.element;

/**
 * 一个元素瓶走到了哪一段。
 *
 * <p>对应总纲 2.1 的「稳定元素 → 衍生元素 → 死寂物质」三段，
 * 外加一个「空瓶」和「已经不可再变」。
 */
public enum DecayStage {

    /** 空瓶子，不参与衰变。 */
    EMPTY("empty"),

    /** 稳定元素：刚从世界采集到的样子。 */
    STABLE("stable"),

    /** 衍生元素：用途已经整个换掉。 */
    DERIVED("derived"),

    /** 已经到尽头（死寂物质不是元素，这里是兜底态）。 */
    DEAD("dead");

    private final String id;

    DecayStage(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** tooltip 用的一句模糊描述 —— 刻意不给数字。 */
    public String descriptionKey() {
        return "tooltip.elemental_verse.stage." + id;
    }

    /** 还能不能继续变。 */
    public boolean canDecay() {
        return this == STABLE || this == DERIVED;
    }
}
