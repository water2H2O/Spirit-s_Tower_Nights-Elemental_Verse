package com.water2h2o.elementalverse.element;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * 元素值（ElementValue）的六条基础轴。
 *
 * <p>设计依据见 `docs/魔法体系原理.md`：整套魔法被当成"化学"，
 * 这六条轴就是化学里的"元素"，暗不是第七条轴，而是**负的光**。
 *
 * <h2>为什么只能有六条</h2>
 * 轴越少，组合越少，玩家越容易建立直觉；轴越多，越接近"任意魔法"但越难预测。
 * 六条是"够用且能记住"的折中。真正的自由度靠<b>形</b>（形态/时序）来给，不是靠加轴。
 */
public enum ElementAxis implements StringRepresentable {

    FIRE("fire"),
    WATER("water"),
    GRASS("grass"),
    EARTH("earth"),
    WIND("wind"),
    LIGHT("light");

    /**
     * 主成分并列时的裁决顺序：火 &gt; 水 &gt; 草 &gt; 地 &gt; 风 &gt; 光。
     *
     * <p>顺序本身没有物理意义，只是为了**确定性** —— 同样的投入必须每次都出同样的结果，
     * 否则玩家无法通过试验学习。
     */
    public static final Codec<ElementAxis> CODEC = StringRepresentable.fromEnum(ElementAxis::values);

    private final String name;

    ElementAxis(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** 这条路是不是"负向"更常见的轴（用于给现象表生成提示，不参与计算）。 */
    public boolean isNegativeFriendly() {
        return this == FIRE || this == GRASS || this == LIGHT;
    }
}
