package com.water2h2o.elementalverse.registry;

import com.mojang.serialization.Codec;
import com.water2h2o.elementalverse.element.ElementType;

import java.util.List;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
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

    /**
     * 衰变进度 0..100（**要同步**，客户端要靠它画耐久条）。
     *
     * <p>刻意只存整数百分比：一条衰变链下来最多变 100 次，
     * 而不是每秒推送一次完整计时器 —— 省流量，也少触发客户端重绘。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DECAY_PROGRESS =
            DATA_COMPONENTS.registerComponentType(
                    "decay_progress",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** 申明板上写的东西（料 + 形）。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.water2h2o.elementalverse.element.Declaration>> DECLARATION =
            DATA_COMPONENTS.registerComponentType(
                    "declaration",
                    builder -> builder
                            .persistent(com.water2h2o.elementalverse.element.Declaration.CODEC)
                            .networkSynchronized(com.water2h2o.elementalverse.element.Declaration.STREAM_CODEC));

    /** 小包内容的编解码器（持久化与网络共用）。**必须声明在使用它的字段之前**。 */
    public static final Codec<List<ItemStack>> POUCH_CODEC = ItemStack.OPTIONAL_CODEC.listOf();

    /**
     * 元素小包里装的东西（9 格）。
     *
     * <p>**这个要同步**：不像计时器，小包的内容是玩家要看的（界面上就是那 9 格），
     * 不同步的话客户端界面会一片空白。
     *
     * <p>用 {@code List<ItemStack>} 而不是原版的 `container` 组件，是为了保持
     * **固定 9 格、位置有意义**（原版那个是"无序列表"，做成 3×3 界面会乱序）。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemStack>>> POUCH_CONTENTS =
            DATA_COMPONENTS.registerComponentType(
                    "pouch_contents",
                    builder -> builder
                            .persistent(POUCH_CODEC)
                            .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(POUCH_CODEC)));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    private EVDataComponents() {
    }
}
