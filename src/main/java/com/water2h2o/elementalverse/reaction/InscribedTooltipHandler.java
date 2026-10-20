package com.water2h2o.elementalverse.reaction;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.registry.EVDataComponents;
import com.water2h2o.elementalverse.registry.EVItems;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 刻了申明的**非申明板**物品（剑 / 书）：悬浮时也显示申明内容。
 *
 * <p>作者要求：「申明书和申明剑要像申明板一样显示详细信息」。
 * 它们用的是原版物品，没法覆写 {@code appendHoverText}，所以走工具提示事件。
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class InscribedTooltipHandler {

    private InscribedTooltipHandler() {
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.is(EVItems.DECLARATION_PLATE.get())) {
            return;
        }
        Declaration declaration = stack.get(EVDataComponents.DECLARATION.get());
        if (declaration == null || !declaration.isDefined()) {
            return;
        }
        var tooltip = event.getToolTip();

        if (declaration.hasMagic()) {
            tooltip.add(Component.translatable(EVLang.MAGIC_NAME, declaration.magic())
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable(EVLang.PLATE_ELEMENT, declaration.element().displayName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(EVLang.PLATE_TIER, declaration.tier())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(formKey(declaration.form()))
                .withStyle(ChatFormatting.DARK_GRAY));

        if (declaration.stateElement() != null && !declaration.stateElement().isEmpty()) {
            tooltip.add(Component.translatable(EVLang.PLATE_STATE,
                            declaration.stateElement().displayName(), declaration.stateTier())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String formKey(String form) {
        return switch (form == null ? "" : form) {
            case "self" -> "tooltip.elemental_verse.form.self";
            case "touch" -> "tooltip.elemental_verse.form.touch";
            default -> "tooltip.elemental_verse.form.projectile";
        };
    }
}
