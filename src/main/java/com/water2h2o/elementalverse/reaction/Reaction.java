package com.water2h2o.elementalverse.reaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.water2h2o.elementalverse.element.ElementValue;

/**
 * 一条反应式 = 这个模组的"魔法"。
 *
 * <p>作者的第一个例子：
 * <pre>
 *   消耗：1 草 + 1 火 → 无条件 → 造成大量持续的火元素伤害 → 返还：无
 * </pre>
 *
 * @param consume   消耗的元素值（玩家投入的向量要等于它）
 * @param phenomenon 产生的现象
 * @param refund    返还的元素值（多数反应是空的）
 */
public record Reaction(ElementValue consume, Phenomenon phenomenon, ElementValue refund,
                       String name, ElementValue max) {

    public static final Codec<Reaction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ElementValue.CODEC.fieldOf("consume").forGetter(Reaction::consume),
            Phenomenon.CODEC.fieldOf("phenomenon").forGetter(Reaction::phenomenon),
            ElementValue.CODEC.optionalFieldOf("refund", ElementValue.ZERO).forGetter(Reaction::refund),
            Codec.STRING.optionalFieldOf("name", "").forGetter(Reaction::name),
            // "最多这么多"：超过就不匹配。冻结要求 火 ≤ 2 就靠这个
            ElementValue.CODEC.optionalFieldOf("max", ElementValue.ZERO).forGetter(Reaction::max)
    ).apply(instance, Reaction::new));

    /** 兼容旧写法（没有 max）。 */
    public Reaction(ElementValue consume, Phenomenon phenomenon, ElementValue refund, String name) {
        this(consume, phenomenon, refund, name, ElementValue.ZERO);
    }

    /**
     * 守恒三律之一：现象的**元素值当量**不能超过投入的强度。
     *
     * <p>比的是 {@code cost} 而不是伤害数值 —— 伤害是玩法参数，元素值才是守恒的东西。
     */
    /**
     * 条件匹配：投入的向量是否**至少**包含这条反应要求的分量。
     *
     * <p>不是等值匹配 —— 这样 `1草+1火`、`2草+1火`、`1草+3火` 会命中同一条，
     * 只是强度不同。**一条配方覆盖一整类组合**，这是数量不爆炸的关键。
     */
    public boolean accepts(ElementValue input) {
        // 空要求 = 只匹配"净归零"的输入（光 + 暗 = 湮灭就靠这条）
        if (consume.asMap().isEmpty()) {
            return input.isZero();
        }
        // 上限：任一轴超过就否掉
        for (var entry = max.asMap().entrySet().iterator(); entry.hasNext();) {
            var limit = entry.next();
            if (input.get(limit.getKey()) > limit.getValue() + ElementValue.EPSILON) {
                return false;
            }
        }
        for (var entry : consume.asMap().entrySet()) {
            double required = entry.getValue();
            double actual = input.get(entry.getKey());
            // 负要求表示"必须带这么多负分量"（例如暗 = 光 -1）
            if (required >= 0) {
                if (actual + ElementValue.EPSILON < required) {
                    return false;
                }
            } else if (actual > required + ElementValue.EPSILON) {
                return false;
            }
        }
        return true;
    }

    /** 只要求一条轴 = 单元素魔法（要在双元素之后判定）。 */
    public boolean isSingleElement() {
        return consume.asMap().size() == 1;
    }

    public boolean isBalanced() {
        return phenomenon.cost() <= consume.magnitude() + ElementValue.EPSILON;
    }
}
