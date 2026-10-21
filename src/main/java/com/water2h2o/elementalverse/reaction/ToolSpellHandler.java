package com.water2h2o.elementalverse.reaction;

import com.water2h2o.elementalverse.element.DeclarationPlateItem;
import com.water2h2o.elementalverse.registry.EVDataComponents;

import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 武器触发：刻了申明的剑，攻击时释放对应的魔法。
 *
 * <p>作者定的规则：申明板与武器都是**触摸触发** —— 所以这里用"打中哪个就作用在哪个身上"。
 * 材料不够就不放，也不消耗。
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class ToolSpellHandler {

    private ToolSpellHandler() {
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        var player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        // 只有**剑**才是攻击触发；板/书带同一个组件，但它们是右键触发，
        // 否则拿着板子左键砍怪也会放法术（issue #1）
        if (!stack.is(net.minecraft.tags.ItemTags.SWORDS)) {
            return;
        }
        var declaration = stack.get(EVDataComponents.DECLARATION.get());
        if (declaration == null || !declaration.isDefined()) {
            return;
        }
        SpellCaster.castAtEntity(player.level(), player, declaration, event.getTarget());
    }
}
