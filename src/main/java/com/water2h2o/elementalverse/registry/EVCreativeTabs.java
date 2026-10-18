package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.element.ElementCarrier;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式物品栏。
 *
 * <p>按**三种载体**分组（作者 2026-10-17 定的排布）：
 * 空元素瓶 + 13 种瓶装元素 / 折射棱镜 + 光·晨 / 聚影瓶 + 暗·影，
 * 最后是死寂尘与死寂物质方块。
 *
 * <p>分组完全由 {@link ElementType#carrier()} 驱动 —— 以后挪动某个元素的载体，
 * 这里不用改。
 */
public final class EVCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "elemental_verse");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(EVLang.CREATIVE_TAB))
                    .icon(() -> ElementVialItem.createStack(ElementType.WATER))
                    .displayItems((parameters, output) -> {
                        // ① 元素瓶
                        output.accept(ElementVialItem.emptyVial());
                        acceptCarrier(output, ElementCarrier.ELEMENT_VIAL);

                        // ② 折射棱镜（光 · 晨）
                        output.accept(new ItemStack(EVItems.REFRACTION_PRISM.get()));
                        acceptCarrier(output, ElementCarrier.REFRACTION_PRISM);

                        // ③ 聚影瓶（暗 · 影）
                        output.accept(new ItemStack(EVItems.SHADOW_BOTTLE.get()));
                        acceptCarrier(output, ElementCarrier.SHADOW_BOTTLE);

                        // ③.5 申明板与仪式台
                        output.accept(new ItemStack(EVItems.DECLARATION_PLATE.get()));
                        output.accept(new ItemStack(EVItems.RITUAL_ALTAR.get()));

                        // ④ 仓储方块（作者反馈：创造栏里找不到密封仓）
                        output.accept(new ItemStack(EVItems.SEALED_VAULT.get()));
                        output.accept(new ItemStack(EVItems.RUNE_CABINET.get()));

                        // ⑤ 便携容器
                        output.accept(new ItemStack(EVItems.ELEMENT_POUCH.get()));

                        // ⑤ 衰变的终点
                        output.accept(new ItemStack(EVItems.DEAD_DUST.get()));
                        output.accept(new ItemStack(EVItems.DEAD_MATTER.get()));
                    })
                    .build());

    /** 按载体过滤元素形态，保持枚举顺序。 */
    private static void acceptCarrier(net.minecraft.world.item.CreativeModeTab.Output output,
                                      ElementCarrier carrier) {
        for (ElementType type : ElementType.values()) {
            if (type.isEmpty() || type.carrier() != carrier) {
                continue;
            }
            output.accept(ElementVialItem.createStack(type));
        }
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private EVCreativeTabs() {
    }
}
