package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.element.PouchItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册。
 *
 * <p>元素不是一个个物品 —— 元素是**数据组件值**，装在三种载体里：
 * 元素瓶（13 种）、折射棱镜（光·晨）、聚影瓶（暗·影）。
 * 三者都是 {@link ElementVialItem}，共用同一套逻辑与组件。
 */
public final class EVItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems("elemental_verse");

    /**
     * 元素瓶。最普通的载体。
     *
     * <p>堆叠上限给 16：元素会各自变质，允许 64 个混在一起只会让玩家更困惑。
     */
    public static final DeferredItem<ElementVialItem> ELEMENT_VIAL = ITEMS.register(
            "element_vial",
            () -> new ElementVialItem(new Item.Properties().stacksTo(16)));

    /** 折射棱镜：光的载体（也装晨）。单件即一件法器，不叠。 */
    public static final DeferredItem<ElementVialItem> REFRACTION_PRISM = ITEMS.register(
            "refraction_prism",
            () -> new ElementVialItem(new Item.Properties().stacksTo(1)));

    /** 聚影瓶：暗的载体（也装影）。 */
    public static final DeferredItem<ElementVialItem> SHADOW_BOTTLE = ITEMS.register(
            "shadow_bottle",
            () -> new ElementVialItem(new Item.Properties().stacksTo(1)));

    /**
     * 死寂尘：元素走到尽头的产物。
     *
     * <p>作者 2026-10-17：衰变**先给尘**，4 个尘压成 1 个死寂物质方块。
     * 这样"还剩多少"看得见，也多了一级加工。
     */
    public static final DeferredItem<Item> DEAD_DUST = ITEMS.register(
            "dead_dust",
            () -> new Item(new Item.Properties()));

    /** 死寂物质方块（建材，只作建筑用）。 */
    public static final DeferredItem<BlockItem> DEAD_MATTER = ITEMS.register(
            "dead_matter",
            () -> new BlockItem(EVBlocks.DEAD_MATTER.get(), new Item.Properties()));

    /** 石制密封仓的方块物品（没有它，方块放不下来也拿不到）。 */
    public static final DeferredItem<BlockItem> SEALED_VAULT = ITEMS.register(
            "sealed_vault",
            () -> new BlockItem(EVBlocks.SEALED_VAULT.get(), new Item.Properties()));

    /**
     * 元素小包：9 格便携容器，放霜/冰可以减缓内容物的变质。
     *
     * <p>作者 2026-10-17：把几种元素装在一起随身带，方便现场做东西。
     */
    public static final DeferredItem<PouchItem> ELEMENT_POUCH = ITEMS.register(
            "element_pouch",
            () -> new PouchItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private EVItems() {
    }
}
