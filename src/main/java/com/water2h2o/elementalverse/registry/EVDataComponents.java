package com.water2h2o.elementalverse.registry;

import com.mojang.serialization.Codec;
import com.water2h2o.elementalverse.element.ElementType;

import net.minecraft.core.component.DataComponentType;
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

    /**
     * 数据组件的注册器。
     *
     * <p>用 {@code createDataComponents} 而不是 {@code create(Registries.DATA_COMPONENT_TYPE, ...)}：
     * 后者返回泛型 {@code DeferredRegister<T>}，拿不到 {@code DataComponents} 这个专用子类
     * （它才有 {@code registerComponentType}）。前者虽被标记弃用，但仍是这里的正解。
     */
    @SuppressWarnings("removal")
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents("elemental_verse");

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
                            .networkSynchronized(ByteBufCodecs.idMapper(ElementType::fromOrdinal, ElementType::ordinal)));

    /**
     * 已经过的 tick 数。由统一的慢速 tick 累加，不依赖真实时间。
     *
     * <p>**刻意不做网络同步。**
     * 这个组件每秒都会变一次；一旦同步，客户端每一秒都会收到"这个物品变了"，
     * 于是重播手持/拾取动画 —— 玩家看到的就是元素瓶在手上闪。
     * 而客户端其实**不需要**它：显示用的名字与图标来自 {@link #ELEMENT}，
     * 计时器只在服务端参与判定。省掉同步，既没闪动也省带宽。
     *
     * <p>如果以后要在客户端显示倒计时（例如"特殊工具"），再打开同步即可。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DECAY_TIMER =
            DATA_COMPONENTS.registerComponentType(
                    "decay_timer",
                    builder -> builder.persistent(Codec.INT));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    private EVDataComponents() {
    }
}
