package com.water2h2o.elementalverse.reaction;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.registry.EVDataComponents;
import com.water2h2o.elementalverse.registry.EVItems;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 刻了申明的**书 / 其它物品**：右键释放。
 *
 * <p>剑走 {@link ToolSpellHandler}（攻击触发），申明板自己走
 * {@code DeclarationPlateItem.use}，这里只兜住剩下的载体。
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class TomeCastHandler {

    private TomeCastHandler() {
    }

    @SubscribeEvent
    static void onUse(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()
                || stack.is(EVItems.DECLARATION_PLATE.get())
                || stack.is(ItemTags.SWORDS)) {
            return;
        }
        Declaration declaration = stack.get(EVDataComponents.DECLARATION.get());
        if (declaration == null || !declaration.isDefined()) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (SpellCaster.cast(event.getLevel(), event.getEntity(), stack)) {
            event.setCanceled(true);
        }
    }
}
