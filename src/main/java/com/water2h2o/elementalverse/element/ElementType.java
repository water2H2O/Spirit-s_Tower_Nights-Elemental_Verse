package com.water2h2o.elementalverse.element;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * 元素的全部形态：7 种稳定元素、10 种衍生元素，外加一个「空」。
 *
 * <p>元素不是一个个物品 —— 它们是同一个「元素瓶」物品上的数据组件值。
 * 所以这里只描述「是什么」以及「会变成什么」，不碰任何注册。
 *
 * <p>衰变链全部汇到同一个终点（死寂物质）。{@code stage2 == null} 表示已经走到尽头。
 */
public enum ElementType implements StringRepresentable {

    // ---- 空 ----
    /** 没装东西的瓶子。不参与衰变。 */
    EMPTY("empty", null, null, false),

    // ---- 七种稳定元素 ----
    EARTH("earth", "sand", null, true),
    WATER("water", "ice", null, true),
    WIND("wind", "lightning", null, true),
    FIRE("fire", "ember", null, true),
    GRASS("grass", "wood", null, true),
    LIGHT("light", "dawn", null, true),
    SHADOW("shadow", "shadow_essence", null, true),

    // ---- 十种衍生元素 ----
    /** 水 → 冰，独占「延缓衰变（冷链）」。 */
    ICE("ice", "frost", null, true),
    /** 水元素的低温支线，阶段 01 只注册、不参与衰变。 */
    FROST("frost", null, null, false),
    /** 风 → 电，独占「瞬时爆发」。 */
    LIGHTNING("lightning", null, null, true),
    /** 草 → 木，独占「再生建材 / 催熟」。 */
    WOOD("wood", null, null, true),
    /** 地 → 砂，独占「玻璃 / 中和灼喉砂」。 */
    SAND("sand", null, null, true),
    /** 火 → 烬，独占「余温 / 延时」。 */
    EMBER("ember", null, null, true),
    /** 地/金 支线，阶段 01 只注册、不参与衰变。 */
    METAL("metal", null, null, false),
    /** 草的高纯支线，阶段 01 只注册、不参与衰变。 */
    BUD("bud", null, null, false),
    /** 光 → 晨，独占「治疗 / 净化 / 破雾」。 */
    DAWN("dawn", null, null, true),
    /** 暗 → 影，独占「隐匿 / 与晶裔沟通」。 */
    SHADOW_ESSENCE("shadow_essence", null, null, true);

    /** 持久化与网络同步用的编解码器。名字是稳定契约，不要随手改字符串。 */
    public static final Codec<ElementType> CODEC = StringRepresentable.fromEnum(ElementType::values);

    private final String id;
    private final ElementType stage1;
    private final ElementType stage2;
    private final boolean decays;

    ElementType(String id, String stage1Id, String stage2Id, boolean decays) {
        this.id = id;
        this.stage1 = stage1Id == null ? null : resolve(stage1Id);
        this.stage2 = stage2Id == null ? null : resolve(stage2Id);
        this.decays = decays;
    }

    /** 编译期常量池里 enum 还没建好，所以用名字二次解析。只在类初始化时跑一次。 */
    private static ElementType resolve(String id) {
        for (ElementType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalStateException("衰变链指向了不存在的元素 id: " + id);
    }

    /** 数据组件里存的字符串。 */
    @Override
    public String getSerializedName() {
        return id;
    }

    /** 是不是空的瓶子。 */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /** 这个形态是否会随时间衰变。*/
    public boolean decays() {
        return decays;
    }

    /** 阶段①的产物；null 表示不会再变。 */
    public ElementType stage1() {
        return stage1;
    }

    /** 阶段②的产物；null 表示走到阶段①就到头了。 */
    public ElementType stage2() {
        return stage2;
    }

    /** 完整衰变链的终点是不是死寂物质（只剩一个阶段可走）。 */
    public boolean endsInDeadMatter() {
        return stage1 != null && stage2 == null;
    }

    /** 这个形态在总纲里属于哪一段（稳定 / 衍生 / 已死寂）。 */
    public DecayStage stage() {
        if (this == EMPTY) {
            return DecayStage.EMPTY;
        }
        if (stage1 == null && stage2 == null) {
            // 走到头了：已经是衍生元素的下一个形态（死寂物质不是元素，这里只兜底）
            return DecayStage.DEAD;
        }
        if (isBaseElement()) {
            return DecayStage.STABLE;
        }
        return DecayStage.DERIVED;
    }

    private boolean isBaseElement() {
        return switch (this) {
            case EARTH, WATER, WIND, FIRE, GRASS, LIGHT, SHADOW -> true;
            default -> false;
        };
    }

    /** 「土元素瓶」里的「土元素」部分。 */
    public Component displayName() {
        return Component.translatable(EVLang.element(this));
    }

    public String id() {
        return id;
    }

    /** 供 lang 文件与调试输出用的规范化命名。 */
    public String translationKey() {
        return EVLang.element(this);
    }

    @Override
    public String toString() {
        return "ElementType[" + id + ']';
    }

    /** 给命令补全 / 调试用的名字集合。 */
    public static String[] ids() {
        ElementType[] values = values();
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i].id;
        }
        return out;
    }

    /** 按字符串找元素（大小写不敏感），找不到返回 EMPTY。 */
    public static ElementType byId(String id) {
        String needle = id.toLowerCase(Locale.ROOT);
        for (ElementType type : values()) {
            if (type.id.equals(needle)) {
                return type;
            }
        }
        return EMPTY;
    }
}
