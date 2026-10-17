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
                       String name) {

    public static final Codec<Reaction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ElementValue.CODEC.fieldOf("consume").forGetter(Reaction::consume),
            Phenomenon.CODEC.fieldOf("phenomenon").forGetter(Reaction::phenomenon),
            ElementValue.CODEC.optionalFieldOf("refund", ElementValue.ZERO).forGetter(Reaction::refund),
            Codec.STRING.optionalFieldOf("name", "").forGetter(Reaction::name)
    ).apply(instance, Reaction::new));

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
        for (var entry : consume.asMap().entrySet()) {
            if (input.get(entry.getKey()) + ElementValue.EPSILON < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean isBalanced() {
        return phenomenon.cost() <= consume.magnitude() + ElementValue.EPSILON;
    }
}
