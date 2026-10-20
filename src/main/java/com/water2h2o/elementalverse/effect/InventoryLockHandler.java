package com.water2h2o.elementalverse.effect;

import com.water2h2o.elementalverse.registry.EVMobEffects;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 物品栏封锁的具体限制：不能用物品、不能丢东西。 */
@EventBusSubscriber(modid = "elemental_verse")
public final class InventoryLockHandler {

    private InventoryLockHandler() {
    }

    @SubscribeEvent
    static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().hasEffect(EVMobEffects.INVENTORY_LOCK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onToss(ItemTossEvent event) {
        if (!event.getPlayer().hasEffect(EVMobEffects.INVENTORY_LOCK)) {
            return;
        }
        event.setCanceled(true);
        // 取消事件时物品已经从格子里拿出来了，必须还回去，否则等于凭空吞掉
        event.getPlayer().getInventory().add(event.getEntity().getItem());
    }
}
