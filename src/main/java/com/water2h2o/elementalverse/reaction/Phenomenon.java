package com.water2h2o.elementalverse.reaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.water2h2o.elementalverse.element.ElementAxis;
import com.water2h2o.elementalverse.element.ElementValue;

/**
 * 现象：反应产生的东西。
 *
 * <p>用元素值来描述强弱 —— 比如"持续火伤害"就是**往目标身上灌火元素值**，
 * 每秒灌多少、灌多久，都写在这里。
 *
 * <p><b>amount/duration 是玩法数值，cost 是元素值当量，两者不能混为一谈。</b>
 * 2 份材料可以换出 6 点伤害 —— 魔法本来就是"拿材料里的势能换效果"，
 * 所以守恒约束的是 <b>cost</b>（≤ 投入强度），不是伤害数字。
 *
 * @param type     现象类型（决定怎么作用）
 * @param element  灌哪条轴
 * @param amount   每秒多少（伤害/半径等玩法数值）
 * @param duration 持续多少 tick
 * @param cost     这次现象消耗的**元素值当量**，必须 ≤ 投入强度（守恒三律）
 */
public record Phenomenon(String type, ElementAxis element, double amount, int duration, double cost) {

    public static final Codec<Phenomenon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(Phenomenon::type),
            ElementAxis.CODEC.optionalFieldOf("element", ElementAxis.FIRE).forGetter(Phenomenon::element),
            Codec.DOUBLE.optionalFieldOf("amount", 3.0D).forGetter(Phenomenon::amount),
            Codec.INT.optionalFieldOf("duration", 100).forGetter(Phenomenon::duration),
            Codec.DOUBLE.optionalFieldOf("cost", 0.0D).forGetter(Phenomenon::cost)
    ).apply(instance, Phenomenon::new));

    /** 现象折算成元素值（用 cost，不是用伤害数字）。 */
    public ElementValue asValue() {
        return ElementValue.of(element, cost);
    }
}
