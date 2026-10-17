package com.water2h2o.elementalverse.registry;

import com.mojang.serialization.Codec;
import com.water2h2o.elementalverse.element.ElementType;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 挂在元素瓶上的两个数据组件。
 *
 * <p>关键取舍：**不存「还剩多久」，只存「已经走了多久」**。
 * 拆分/合并堆叠时组件原样复制，所以拆了也刷新不了计时。
 */
public final class EVDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "elemental_verse");

    /**
     * 瓶子里装的是什么。没这个组件的元素瓶就是空瓶。
     *
     * <p>用 {@link ElementType#CODEC}（一个按名字查表的 EnumCodec）持久化 ——
     * 名字是稳定契约，以后往枚举中间插新元素也不会让老存档错位。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ElementType>> ELEMENT =
            DATA_COMPONENTS.registerComponentType(
                    "element",
                    builder -> builder
                            .persistent(ElementType.CODEC)
                            .networkSynchronized(ByteBufCodecs.idMapper(ElementType::byId, ElementType::ordinal)));

    /** 已经过的 tick 数。由统一的慢速 tick 累加，不依赖真实时间。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DECAY_TIMER =
            DATA_COMPONENTS.registerComponentType(
                    "decay_timer",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    private EVDataComponents() {
    }
}
