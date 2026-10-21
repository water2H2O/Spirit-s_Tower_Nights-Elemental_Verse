package com.water2h2o.elementalverse.element;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 一条"申明"：写在申明板上的东西。
 *
 * <p>依据 `docs/魔法体系原理.md`：**料 + 形**。
 * <ul>
 *   <li>料：{@code element} + {@code tier}（档位 1/2/3 对应投入 1/3/9 个）</li>
 *   <li>形：{@code operation}（操作）+ {@code form}（形态）</li>
 * </ul>
 *
 * <p>本期形只有一个取值：释放 + 发射。其余保留字段，以后靠知识解锁。
 *
 * @param element   料是哪种元素（空 = 还没写）
 * @param tier      档位 1 / 2 / 3
 * @param operation 操作：release（释放）/ gather（聚集）/ shape（塑形）
 * @param form      形态：projectile（发射）/ self（自身）/ touch（交互）
 */
public record Declaration(ElementType element, int tier, String operation, String form,
                          ElementType stateElement, int stateTier,
                          ElementType reagentElement, int reagentTier, String magic,
                          Declaration second, String modifier, int modified, long readyAt) {

    /** 一条法术链最多叠这么多次修饰。 */
    public static final int MAX_MODIFIED = 3;

    /**
     * "没有第二段"的占位。
     *
     * <p>不能直接用 null：{@code DataResult.Success.result()} 内部是
     * {@code Optional.of(value)}，装 null 会直接 NPE —— 旧存档里没有 second 字段，
     * 一读玩家背包就崩。
     */
    private static final Declaration NONE = new Declaration(
            ElementType.EMPTY, 0, "release", "projectile", ElementType.EMPTY, 0,
            ElementType.EMPTY, 0, "", null, "", 0, 0L);

    public static final Declaration EMPTY = NONE;

    /** 有没有拼贴出来的第二段。 */
    public boolean hasSecond() {
        return second != null && second.isDefined();
    }

    /** 拼接后这条链算被修饰了几次：取两段里更大的 + 1。 */
    public static int chainCount(Declaration first, Declaration second) {
        int a = first == null ? 0 : first.modified();
        int b = second == null ? 0 : second.modified();
        return Math.max(a, b) + 1;
    }

    /** 兼容旧写法（没有态）。 */
    public Declaration(ElementType element, int tier, String operation, String form) {
        this(element, tier, operation, form, ElementType.EMPTY, 0, ElementType.EMPTY, 0,
                "", NONE, "", 0, 0L);
    }

    /** 兼容"没辅料"的写法。 */
    public Declaration(ElementType element, int tier, String operation, String form,
                       ElementType stateElement, int stateTier) {
        this(element, tier, operation, form, stateElement, stateTier, ElementType.EMPTY, 0,
                "", NONE, "", 0, 0L);
    }

    /** 兼容"带魔法名、但没有拼贴"的写法。 */
    public Declaration(ElementType element, int tier, String operation, String form,
                       ElementType stateElement, int stateTier,
                       ElementType reagentElement, int reagentTier, String magic) {
        this(element, tier, operation, form, stateElement, stateTier,
                reagentElement, reagentTier, magic, null, "", 0, 0L);
    }

    /** 兼容"没辅料也没魔法名"的写法。 */
    public Declaration(ElementType element, int tier, String operation, String form,
                       ElementType stateElement, int stateTier,
                       ElementType reagentElement, int reagentTier) {
        this(element, tier, operation, form, stateElement, stateTier,
                reagentElement, reagentTier, "", NONE, "", 0, 0L);
    }

    /**
     * 投入的元素值 = 主料 + 辅料（态不参与，它只定参数）。
     *
     * <p>这是"化学"的核心：反应只看**加起来的结果**，不看哪个格子放了什么。
     */
    public ElementValue inputValue() {
        ElementValue value = value();
        if (reagentElement != null && !reagentElement.isEmpty() && reagentTier > 0) {
            value = value.plus(reagentElement.value().scale(reagentTier));
        }
        return value;
    }

    /**
     * 递归编解码器：{@code second} 里还是一整条申明，所以要用 {@link Codec#recursive}。
     * 直接在自己的初始化式里引用 {@code CODEC} 会读到 null。
     */
    public static final Codec<Declaration> CODEC = Codec.recursive("elemental_verse:declaration",
            self -> RecordCodecBuilder.create(instance -> instance.group(
                    ElementType.CODEC.fieldOf("element").forGetter(Declaration::element),
                    Codec.INT.optionalFieldOf("tier", 1).forGetter(Declaration::tier),
                    Codec.STRING.optionalFieldOf("operation", "release")
                            .forGetter(Declaration::operation),
                    Codec.STRING.optionalFieldOf("form", "projectile").forGetter(Declaration::form),
                    ElementType.CODEC.optionalFieldOf("state_element", ElementType.EMPTY)
                            .forGetter(Declaration::stateElement),
                    Codec.INT.optionalFieldOf("state_tier", 0).forGetter(Declaration::stateTier),
                    ElementType.CODEC.optionalFieldOf("reagent_element", ElementType.EMPTY)
                            .forGetter(Declaration::reagentElement),
                    Codec.INT.optionalFieldOf("reagent_tier", 0).forGetter(Declaration::reagentTier),
                    Codec.STRING.optionalFieldOf("magic", "").forGetter(Declaration::magic),
                    // 编解码都要能处理 null：
                    //   解码缺省 → NONE（不能是 null，DataResult 装不了 null）
                    //   编码 null → 字段直接省略（否则 optionalFieldOf 会去 encode null）
                    self.optionalFieldOf("second")
                            .xmap(o -> o.orElse(NONE), java.util.Optional::ofNullable)
                            .forGetter(Declaration::second),
                    Codec.STRING.optionalFieldOf("modifier", "").forGetter(Declaration::modifier),
                    Codec.INT.optionalFieldOf("modified", 0).forGetter(Declaration::modified),
                    Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(Declaration::readyAt)
            ).apply(instance, Declaration::new)));

    public static final StreamCodec<io.netty.buffer.ByteBuf, Declaration> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    /** 这条申明算出来的魔法名（写板子时定好，客户端直接显示）。 */
    public boolean hasMagic() {
        return magic != null && !magic.isEmpty();
    }

    /** 写过了没有。 */
    /** 换一个形态（板 = 自身 / 剑 = 接触 / 书 = 发射）。 */
    public Declaration withForm(String newForm) {
        return new Declaration(element, tier, operation, newForm, stateElement, stateTier,
                reagentElement, reagentTier, magic, second, modifier, modified, readyAt);
    }

    /** 记下"什么时候可以再放"。 */
    public Declaration withReadyAt(long newReadyAt) {
        return new Declaration(element, tier, operation, form, stateElement, stateTier,
                reagentElement, reagentTier, magic, second, modifier, modified, newReadyAt);
    }

    /** 挂上修饰材料与新的修饰次数。 */
    public Declaration withModifier(String newModifier, int newModified) {
        return new Declaration(element, tier, operation, form, stateElement, stateTier,
                reagentElement, reagentTier, magic, second, newModifier, newModified, readyAt);
    }

    /** 拼上第二段。 */
    public Declaration withSecond(Declaration newSecond, String newModifier, int newModified) {
        return new Declaration(element, tier, operation, form, stateElement, stateTier,
                reagentElement, reagentTier, magic, newSecond, newModifier, newModified, readyAt);
    }

    public boolean isDefined() {
        return element != null && !element.isEmpty() && tier > 0;
    }

    /**
     * 这条申明的"料"：元素值 × 档位。
     *
     * <p>档位 1/2/3 = 投入 1/3/9 个元素瓶，所以火·档 3 就是 {火:3}。
     */
    public ElementValue value() {
        return isDefined() ? element.value().scale(tier) : ElementValue.ZERO;
    }

    /** 给玩家看的强度（不会显示具体数字以外的东西）。 */
    public int investCount() {
        return costOfTier(tier);
    }

    /**
     * **释放**这条申明时要扣几个元素。
     *
     * <p>和写入不同：写入是 1 / 3 / 9（把元素烧进板子里，贵），
     * 释放只是再掏一次材料把法术放出去，所以按档位 1 / 2 / 3 收。
     */
    public int castCount() {
        return Math.max(0, Math.min(3, tier));
    }

    /** 主料（写入时）要扣几个元素。 */
    public int stateCost() {
        return stateElement != null && !stateElement.isEmpty() ? costOfTier(stateTier) : 0;
    }

    /** 辅料（写入时 / 施法时）要扣几个元素。 */
    public int reagentCost() {
        return reagentElement != null && !reagentElement.isEmpty() ? costOfTier(reagentTier) : 0;
    }

    /** 档位 1 / 2 / 3 对应的投入个数；不合法返回 0。 */
    public static int costOfTier(int tier) {
        return switch (tier) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 9;
            default -> 0;
        };
    }

    /** 投入 n 个元素对应的档位；不合法返回 0。 */
    public static int tierOf(int count) {
        if (count >= 9) {
            return 3;
        }
        if (count >= 3) {
            return 2;
        }
        return count >= 1 ? 1 : 0;
    }
}
