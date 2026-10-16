package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.element.ElementVialItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册。
 *
 * <p>元素不是物品 —— 17 种元素形态共用一个「元素瓶」物品，靠数据组件区分。
 * 所以这里只有 4 个物品，加一个死寂物质的方块物品。
 */
public final class EVItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems("elemental_verse");

    /**
     * 元素瓶。**整个模组的入口与核心载体。**
     *
     * <p>空着是空瓶；装了东西靠 {@code elemental_verse:element} 组件区分。
     * 堆叠上限刻意给 16：元素会各自变质，允许 64 个混在一起只会让玩家更困惑。
     */
    public static final DeferredItem<ElementVialItem> ELEMENT_VIAL = ITEMS.register(
            "element_vial",
            () -> new ElementVialItem(new Item.Properties().stacksTo(16)));

    /** 折射棱镜：光的专用采集容器。阶段 01 只注册与展示，功能留到采集系统那一轮。 */
    public static final DeferredItem<Item> REFRACTION_PRISM = ITEMS.register(
            "refraction_prism",
            () -> new Item(new Item.Properties().stacksTo(1)));

    /** 聚影瓶：暗的专用采集容器。线索见总纲 11.2（遮光玻璃）。 */
    public static final DeferredItem<Item> SHADOW_BOTTLE = ITEMS.register(
            "shadow_bottle",
            () -> new Item(new Item.Properties().stacksTo(1)));

    /** 死寂物质的物品形态，用于合成与配方。 */
    public static final DeferredItem<BlockItem> DEAD_MATTER = ITEMS.register(
            "dead_matter",
            () -> new BlockItem(EVBlocks.DEAD_MATTER.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private EVItems() {
    }
}
