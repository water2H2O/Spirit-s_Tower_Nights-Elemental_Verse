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
                          ElementType reagentElement, int reagentTier) {

    public static final Declaration EMPTY = new Declaration(
            ElementType.EMPTY, 0, "release", "projectile", ElementType.EMPTY, 0,
            ElementType.EMPTY, 0);

    /** 兼容旧写法（没有态）。 */
    public Declaration(ElementType element, int tier, String operation, String form) {
        this(element, tier, operation, form, ElementType.EMPTY, 0, ElementType.EMPTY, 0);
    }

    /** 兼容"没辅料"的写法。 */
    public Declaration(ElementType element, int tier, String operation, String form,
                       ElementType stateElement, int stateTier) {
        this(element, tier, operation, form, stateElement, stateTier, ElementType.EMPTY, 0);
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

    public static final Codec<Declaration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ElementType.CODEC.fieldOf("element").forGetter(Declaration::element),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(Declaration::tier),
            Codec.STRING.optionalFieldOf("operation", "release").forGetter(Declaration::operation),
            Codec.STRING.optionalFieldOf("form", "projectile").forGetter(Declaration::form),
            ElementType.CODEC.optionalFieldOf("state_element", ElementType.EMPTY)
                    .forGetter(Declaration::stateElement),
            Codec.INT.optionalFieldOf("state_tier", 0).forGetter(Declaration::stateTier),
            ElementType.CODEC.optionalFieldOf("reagent_element", ElementType.EMPTY)
                    .forGetter(Declaration::reagentElement),
            Codec.INT.optionalFieldOf("reagent_tier", 0).forGetter(Declaration::reagentTier)
    ).apply(instance, Declaration::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, Declaration> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    /** 写过了没有。 */
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
