package com.water2h2o.elementalverse.element;

import com.water2h2o.elementalverse.registry.EVItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 元素的**载体**。
 *
 * <p>作者 2026-10-17 决定把光暗拆成独立物品：
 * <ul>
 *   <li>地/水/风/火/草 与它们的衍生 → {@link #ELEMENT_VIAL}</li>
 *   <li>光/晨 → {@link #REFRACTION_PRISM}</li>
 *   <li>暗/影 → {@link #SHADOW_BOTTLE}</li>
 * </ul>
 *
 * <p>三者共用同一套数据组件（{@code element} / {@code decay_timer}）与同一套衰变逻辑，
 * 区别只在物品本身与它的模型分发。这样"容器"成了数据的一等公民 ——
 * 以后要加神器能量池之类的载体，扩这个枚举即可。
 *
 * <p><b>注意：构造器里不能碰 {@link EVItems}。</b> 枚举常量会在模组注册之前构造，
 * 那时物品还是 null。所以物品一律通过 {@link #item()} 惰性取。
 */
public enum ElementCarrier {

    /** 元素瓶：最普通的载体。 */
    ELEMENT_VIAL("element_vial"),

    /** 折射棱镜：光的载体（也装晨）。 */
    REFRACTION_PRISM("refraction_prism"),

    /** 聚影瓶：暗的载体（也装影）。 */
    SHADOW_BOTTLE("shadow_bottle");

    private final String itemName;

    ElementCarrier(String itemName) {
        this.itemName = itemName;
    }

    /** 注册名，仅供日志与调试。 */
    public String itemName() {
        return itemName;
    }

    /** 这个载体对应的物品。**运行时才取**，见类注释。 */
    public Item item() {
        return switch (this) {
            case ELEMENT_VIAL -> EVItems.ELEMENT_VIAL.get();
            case REFRACTION_PRISM -> EVItems.REFRACTION_PRISM.get();
            case SHADOW_BOTTLE -> EVItems.SHADOW_BOTTLE.get();
        };
    }

    /** 这个物品是不是元素载体；不是就返回 null。 */
    public static ElementCarrier of(Item item) {
        if (item == null) {
            return null;
        }
        for (ElementCarrier carrier : values()) {
            // 注册表还没就绪时 item() 可能抛异常，这里保守跳过
            Item candidate;
            try {
                candidate = carrier.item();
            } catch (RuntimeException notReady) {
                continue;
            }
            if (candidate == item) {
                return carrier;
            }
        }
        return null;
    }

    /** 这个堆叠是不是元素载体。 */
    public static boolean isCarrier(ItemStack stack) {
        return !stack.isEmpty() && of(stack.getItem()) != null;
    }

    /** 这个堆叠属于哪个载体；不是载体返回 null。 */
    public static ElementCarrier of(ItemStack stack) {
        return stack.isEmpty() ? null : of(stack.getItem());
    }
}
