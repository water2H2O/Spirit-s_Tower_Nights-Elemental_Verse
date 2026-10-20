package com.water2h2o.elementalverse.element;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 元素值：六维向量。整套魔法系统的"化学式"。
 *
 * <p>分量可以是**正数、负数、小数**。负数不是"扣分"，而是这个体系的特色：
 * <ul>
 *   <li>暗 = 负光 —— 所以"光 + 暗 = 0"天然成立，不需要特判</li>
 *   <li>霜 = {火:-0.5} —— 纯克制剂；它能**吸热**，降温不是水的属性，是"负火"的属性</li>
 * </ul>
 *
 * <h2>运算</h2>
 * <ul>
 *   <li>{@link #plus}：混合两种物质</li>
 *   <li>{@link #minus} / {@link #clashTo}：**对抗**。作者举的例子：水灭火就是数值相减，
 *       最后较大的那个保留下来 —— 玩家只会感受到"哪一边更强"</li>
 *   <li>{@link #magnitude}：强度（各分量绝对值之和），用来缩放现象强弱</li>
 *   <li>{@link #dominant}：主成分（绝对值最大的那条轴），决定现象**类别**</li>
 * </ul>
 */
public final class ElementValue {

    /** 匹配容差：浮点比较不能直接用 ==。 */
    public static final double EPSILON = 1.0e-3D;

    public static final ElementValue ZERO = new ElementValue(new double[ElementAxis.values().length]);

    public static final Codec<ElementValue> CODEC = Codec.unboundedMap(ElementAxis.CODEC, Codec.DOUBLE)
            .xmap(ElementValue::of, ElementValue::asMap);

    public static final StreamCodec<io.netty.buffer.ByteBuf, ElementValue> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    private final double[] components;

    private ElementValue(double[] components) {
        this.components = components;
    }

    // ---------------------------------------------------------------- 构造

    public static ElementValue of(ElementAxis axis, double amount) {
        double[] values = new double[ElementAxis.values().length];
        values[axis.ordinal()] = amount;
        return new ElementValue(values);
    }

    public static ElementValue of(Map<ElementAxis, Double> map) {
        double[] values = new double[ElementAxis.values().length];
        map.forEach((axis, amount) -> values[axis.ordinal()] = amount);
        return new ElementValue(values);
    }

    // ---------------------------------------------------------------- 读取

    public double get(ElementAxis axis) {
        return components[axis.ordinal()];
    }

    /** 强度：各分量绝对值之和。 */
    public double magnitude() {
        double sum = 0;
        for (double value : components) {
            sum += Math.abs(value);
        }
        return sum;
    }

    /**
     * 主成分：绝对值最大的那条轴；并列时按枚举声明顺序（火 &gt; 水 &gt; 草 &gt; 地 &gt; 风 &gt; 光）裁决。
     *
     * @return 全零时返回 null（那叫**湮灭**，不产生现象）
     */
    public ElementAxis dominant() {
        ElementAxis best = null;
        double bestSize = EPSILON;
        for (ElementAxis axis : ElementAxis.values()) {
            double size = Math.abs(components[axis.ordinal()]);
            // 严格大于：并列时保留先出现的，从而天然按声明顺序裁决
            if (size > bestSize) {
                bestSize = size;
                best = axis;
            }
        }
        return best;
    }

    /** 主成分的方向（+1 / -1）。negative 就是"负火 = 吸热"这类效果的来源。 */
    public double dominantSign() {
        ElementAxis axis = dominant();
        return axis == null ? 0 : Math.signum(components[axis.ordinal()]);
    }

    public boolean isZero() {
        return magnitude() < EPSILON;
    }

    /** 有没有某条轴的负分量（例如"带负火 = 会吸热"）。 */
    public boolean hasNegative(ElementAxis axis) {
        return components[axis.ordinal()] < -EPSILON;
    }

    public Map<ElementAxis, Double> asMap() {
        Map<ElementAxis, Double> map = new LinkedHashMap<>();
        for (ElementAxis axis : ElementAxis.values()) {
            double value = components[axis.ordinal()];
            if (Math.abs(value) > EPSILON) {
                map.put(axis, value);
            }
        }
        return map;
    }

    // ---------------------------------------------------------------- 运算

    public ElementValue plus(ElementValue other) {
        double[] result = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            result[i] = components[i] + other.components[i];
        }
        return new ElementValue(result);
    }

    public ElementValue minus(ElementValue other) {
        double[] result = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            result[i] = components[i] - other.components[i];
        }
        return new ElementValue(result);
    }

    public ElementValue scale(double factor) {
        double[] result = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            result[i] = components[i] * factor;
        }
        return new ElementValue(result);
    }

    /**
     * 对抗：{@code this} 遇上 {@code other}，返回**相减之后剩下的**那部分。
     *
     * <p>这就是"水灭火"的算法 —— 不问是谁的属性，只做减法，最后剩下的那个说了算。
     * 玩家看到的是"水比火强/火比水强"，而不是一堆数值。
     */
    public ElementValue clashTo(ElementValue other) {
        return minus(other);
    }

    /** 两个数值在容差内相等。用于反应式的精确配方匹配。 */
    public boolean matches(ElementValue other) {
        for (int i = 0; i < components.length; i++) {
            if (Math.abs(components[i] - other.components[i]) > EPSILON) {
                return false;
            }
        }
        return true;
    }

    /** {@code this} 能不能"抵消掉" {@code other}（即相减后对方的分量都不剩）。 */
    public boolean overcomes(ElementValue other) {
        return magnitude() > other.magnitude() + EPSILON;
    }

    // ---------------------------------------------------------------- 显示

    @Override
    public String toString() {
        Map<ElementAxis, Double> map = asMap();
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        map.forEach((axis, amount) -> {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append(axis.getSerializedName()).append(':').append(trim(amount));
        });
        return builder.append('}').toString();
    }

    private static String trim(double value) {
        if (Math.abs(value - Math.rint(value)) < EPSILON) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.valueOf(Math.round(value * 1000) / 1000.0);
    }

    /** 方便给外部做一个"不可变空表"（避免重复分配）。 */
    public static Map<ElementAxis, Double> emptyMap() {
        return Collections.unmodifiableMap(new EnumMap<>(ElementAxis.class));
    }
}
