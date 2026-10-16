package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.element.ElementType;
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
 * <p>排布刻意按「载体 → 稳定元素 → 衍生元素 → 尽头」分组，
 * 这样测试衰变时可以从这里直接拖出任何一个形态，不必先解决采集。
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
                        // 载体
                        output.accept(ElementVialItem.emptyVial());
                        ItemStack prism = new ItemStack(EVItems.REFRACTION_PRISM.get());
                        output.accept(prism);
                        output.accept(new ItemStack(EVItems.SHADOW_BOTTLE.get()));

                        // 全部元素形态（跳过 EMPTY，它已经作为空瓶加过了）
                        for (ElementType type : ElementType.values()) {
                            if (type.isEmpty()) {
                                continue;
                            }
                            output.accept(ElementVialItem.createStack(type));
                        }

                        // 尽头
                        output.accept(new ItemStack(EVItems.DEAD_MATTER.get()));
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private EVCreativeTabs() {
    }
}
