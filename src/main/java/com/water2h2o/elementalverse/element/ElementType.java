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
 *
 * <h2>⚠️ 一个必须守住的约束</h2>
 * 枚举构造器里**绝对不能**调用 {@code values()} / {@code valueOf()}，也不能构造
 * {@code EnumMap}。原因：{@code $VALUES} 要等所有常量都构造完才会被赋值，
 * 构造期间调 {@code values()} 会读到 null 并抛
 * {@code NullPointerException: ... $VALUES is null}，表现为
 * {@code ExceptionInInitializerError}，整个模组加载失败。
 *
 * <p>所以这里构造器只保存**字符串 id**，真正的解析推迟到 {@link #stage1()} /
 * {@link #stage2()} 被调用时（那时类已经初始化完毕）。这曾经是一个真实的线上崩溃。
 */
public enum ElementType implements StringRepresentable {

    // ---- 空 ----
    /** 没装东西的瓶子。不参与衰变。 */
    EMPTY("empty", null, null, false),

    // ---- 七种稳定元素 ----
    EARTH("earth", "sand", null, true),
    /**
     * 水 → **霜**（不是冰）。
     *
     * <p>作者 2026-10-17 改的口径：普通水元素演变成霜，**冰要单独获取**，
     * 这样"冰"才有稀缺性（它以后是小包里的高级冷却剂）。
     */
    WATER("water", "frost", null, true),
    WIND("wind", "lightning", null, true),
    FIRE("fire", "ember", null, true),
    GRASS("grass", "bud", null, true),
    LIGHT("light", "dawn", null, true),
    SHADOW("shadow", "shadow_essence", null, true),

    // ---- 十种衍生元素 ----
    /**
     * 冰：**不由衰变产生**，要从冰方块采（见采集系统）。
     *
     * <p>作者 2026-10-17：**冰作为元素态也会衰变**，所以 {@code decays = true}，
     * 时长在 {@code EVConfig} 的预设表里单独给（它没有父元素，不能靠继承算）。
     */
    ICE("ice", null, null, true),
    /** 霜：水的衍生形态，也是小包里的基础冷却剂。会继续衰变成死寂尘。 */
    FROST("frost", null, null, true),
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
    BUD("bud", "wood", null, true),
    /** 光 → 晨，独占「治疗 / 净化 / 破雾」。 */
    DAWN("dawn", null, null, true),
    /** 暗 → 影，独占「隐匿 / 与晶裔沟通」。 */
    SHADOW_ESSENCE("shadow_essence", null, null, true);

    /**
     * 持久化与网络同步用的编解码器。
     *
     * <p>声明在常量之后是**必须的**：此时 {@code $VALUES} 已经赋值，
     * {@code fromEnum} 立刻建立名字索引也不会踩到空数组。
     */
    public static final Codec<ElementType> CODEC = StringRepresentable.fromEnum(ElementType::values);

    private final String id;
    /** 存 id 而不是枚举引用 —— 构造期间拿不到其他常量。 */
    private final String stage1Id;
    private final String stage2Id;
    private final boolean decays;

    ElementType(String id, String stage1Id, String stage2Id, boolean decays) {
        this.id = id;
        this.stage1Id = stage1Id;
        this.stage2Id = stage2Id;
        this.decays = decays;
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

    /** 这个形态是否会随时间衰变。 */
    public boolean decays() {
        return decays;
    }

    /** 阶段①的产物；null 表示不会再变。惰性解析，见类注释。 */
    public ElementType stage1() {
        return resolve(stage1Id);
    }

    /** 阶段②的产物；null 表示走到阶段①就到头了。 */
    public ElementType stage2() {
        return resolve(stage2Id);
    }

    /** 完整衰变链的终点是不是死寂物质（只剩一个阶段可走）。 */
    public boolean endsInDeadMatter() {
        return resolve(stage1Id) != null && resolve(stage2Id) == null;
    }

    /** 这个形态在总纲里属于哪一段（稳定 / 衍生 / 已死寂）。 */
    public DecayStage stage() {
        if (this == EMPTY) {
            return DecayStage.EMPTY;
        }
        if (resolve(stage1Id) == null && resolve(stage2Id) == null) {
            return DecayStage.DEAD;
        }
        if (isBaseElement()) {
            return DecayStage.STABLE;
        }
        return DecayStage.DERIVED;
    }

    /** 是不是七种基础元素之一（衰变链的第一段）。 */
    public boolean isBaseElement() {
        return switch (this) {
            case EARTH, WATER, WIND, FIRE, GRASS, LIGHT, SHADOW -> true;
            default -> false;
        };
    }

    /**
     * 创造物品栏与模型分发用的序号。
     *
     * <p>{@code +1} 是为了给「空」留出 0：0 对应基础模型，1..N 对应各元素。
     * 这个值会写进 {@code custom_model_data} 组件，由物品模型的 overrides 读取。
     */
    public int modelIndex() {
        return ordinal() + 1;
    }

    /**
     * 这个元素住在**哪个载体**里。
     *
     * <p>作者 2026-10-17 决定：光/晨用折射棱镜，暗/影用聚影瓶，其余用元素瓶。
     * 三条载体共用同一套数据组件与衰变逻辑，区别只在物品本身。
     */
    public ElementCarrier carrier() {
        return switch (this) {
            case LIGHT, DAWN -> ElementCarrier.REFRACTION_PRISM;
            case SHADOW, SHADOW_ESSENCE -> ElementCarrier.SHADOW_BOTTLE;
            default -> ElementCarrier.ELEMENT_VIAL;
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
        ElementType[] all = values();
        String[] out = new String[all.length];
        for (int i = 0; i < all.length; i++) {
            out[i] = all[i].id;
        }
        return out;
    }

    /** 按字符串找元素（大小写不敏感），找不到返回 EMPTY。 */
    public static ElementType byId(String id) {
        if (id == null) {
            return EMPTY;
        }
        String needle = id.toLowerCase(Locale.ROOT);
        for (ElementType type : values()) {
            if (type.id.equals(needle)) {
                return type;
            }
        }
        return EMPTY;
    }

    /**
     * 按序号找元素 —— 网络同步用。
     *
     * <p>越界时返回 {@link #EMPTY} 而不是抛异常：万一以后有人往枚举中间插值，
     * 老客户端收到未知序号只会显示成空瓶，而不是把连接打断。
     */
    public static ElementType fromOrdinal(int ordinal) {
        ElementType[] all = values();
        if (ordinal < 0 || ordinal >= all.length) {
            return EMPTY;
        }
        return all[ordinal];
    }

    /** 惰性解析 id → 枚举；null 原样返回 null。只在类初始化完成后调用。 */
    private static ElementType resolve(String id) {
        if (id == null) {
            return null;
        }
        for (ElementType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalStateException("衰变链指向了不存在的元素 id: " + id);
    }

    // ---------------------------------------------------------------- 元素值

    /**
     * 每种元素对应的元素值（六维向量）。
     *
     * <p>用**静态表按 id 查**而不是加枚举参数，原因有两个：
     * <ul>
     *   <li>枚举构造时不能调用 {@code values()}（踩过静态初始化崩溃的坑）</li>
     *   <li>二级元素的式子以后还会调，改数据不动构造函数签名</li>
     * </ul>
     *
     * <p>数值口径见 `docs/魔法体系原理.md` 第四节。
     */
    private static final java.util.Map<String, ElementValue> VALUES = new java.util.HashMap<>();

    static {
        VALUES.put("fire", ElementValue.of(ElementAxis.FIRE, 1));
        VALUES.put("water", ElementValue.of(ElementAxis.WATER, 1));
        VALUES.put("earth", ElementValue.of(ElementAxis.EARTH, 1));
        VALUES.put("wind", ElementValue.of(ElementAxis.WIND, 1));
        VALUES.put("grass", ElementValue.of(ElementAxis.GRASS, 1));
        VALUES.put("light", ElementValue.of(ElementAxis.LIGHT, 1));

        // 暗 = 负的光（不是第七条第轴）
        VALUES.put("shadow", ElementValue.of(ElementAxis.LIGHT, -1));

        // 二级元素 = **纯克制剂**（作者 2026-10-20）：
        // 只有一条轴、而且是负值 —— 它们不提供任何正面分量，只用来抵消。
        //   ？克火 → 霜/冰      水克？(砂/金 抵消水)
        // 因此"火 + 冰"会互相抵消，净值为零。
        VALUES.put("frost", ElementValue.of(ElementAxis.FIRE, -0.5));
        VALUES.put("ice", ElementValue.of(ElementAxis.FIRE, -1.0));
        VALUES.put("sand", ElementValue.of(ElementAxis.WATER, -0.5));
        VALUES.put("metal", ElementValue.of(ElementAxis.WATER, -1.0));
        VALUES.put("bud", ElementValue.of(ElementAxis.EARTH, -0.5));
        VALUES.put("wood", ElementValue.of(ElementAxis.EARTH, -1.0));
        VALUES.put("lightning", ElementValue.of(ElementAxis.GRASS, -0.5));
        VALUES.put("ember", ElementValue.of(ElementAxis.LIGHT, -0.5));

        // 光系例外：光是"没有天敌"的轴，所以它只有强弱之分
        VALUES.put("dawn", ElementValue.of(ElementAxis.LIGHT, 2.0));
        VALUES.put("shadow_essence", ElementValue.of(ElementAxis.LIGHT, -1.5));
    }

    /** 这种元素的元素值（空元素返回零向量）。 */
    public ElementValue value() {
        ElementValue value = VALUES.get(id);
        return value == null ? ElementValue.ZERO : value;
    }

    /** 按 id 找形态（命令与调试用）。 */
    public static ElementType byName(String name) {
        for (ElementType type : values()) {
            if (type.id.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
