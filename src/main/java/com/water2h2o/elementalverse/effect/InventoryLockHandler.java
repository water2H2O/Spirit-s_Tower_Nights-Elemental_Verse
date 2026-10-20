package com.water2h2o.elementalverse.effect;

import com.water2h2o.elementalverse.registry.EVMobEffects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 物品栏封锁的具体限制：不能用物品、不能攻击、不能丢东西。
 *
 * <p>**对生物同样生效** —— 骷髅拉不开弓、女巫灌不了药、僵尸拍不出伤害。
 * 玩家走的是交互事件；生物没有"右键"，所以走的是更底层的那几条：
 * <ul>
 *   <li>用物品：{@link LivingEntityUseItemEvent.Start}（弓、药水、食物、盾牌都从这里过）</li>
 *   <li>攻击：{@link LivingIncomingDamageEvent} 里取消"由被封锁者造成"的伤害</li>
 *   <li>远程：拦掉它刚生成、主人正被封锁的弹射物</li>
 * </ul>
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class InventoryLockHandler {

    private InventoryLockHandler() {
    }

    // ---------------------------------------------------------------- 玩家

    @SubscribeEvent
    static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().hasEffect(EVMobEffects.INVENTORY_LOCK)) {
            event.setCanceled(true);
        }
    }

    /** 被洪流糊住的时候连手都抬不起来 —— 攻击一律取消。 */
    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().hasEffect(EVMobEffects.INVENTORY_LOCK)) {
            event.setCanceled(true);
        }
    }

    /** 放方块也算"使用物品"，同样锁住。 */
    @SubscribeEvent
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().hasEffect(EVMobEffects.INVENTORY_LOCK)
                && !event.getItemStack().isEmpty()) {
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

    // ---------------------------------------------------------------- 所有生物

    /**
     * 用物品：拉弓、喝药、吃食物、举盾都从这里过。
     *
     * <p>骷髅的弓、女巫的药水因此都放不出来。
     */
    @SubscribeEvent
    static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().hasEffect(EVMobEffects.INVENTORY_LOCK)) {
            event.setCanceled(true);
        }
    }

    /**
     * 攻击：把"由被封锁者造成"的伤害直接取消。
     *
     * <p>玩家已经有 {@link AttackEntityEvent} 拦在前面；这一条是为了生物 ——
     * 僵尸、铁傀儡、凋灵骷髅的近战都算在这里。
     */
    @SubscribeEvent
    static void onDealDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living
                && living.hasEffect(EVMobEffects.INVENTORY_LOCK)) {
            event.setCanceled(true);
        }
    }

    /**
     * 远程：拦掉"主人正被封锁"的弹射物。
     *
     * <p>兜底用 —— 有些生物不走 useItem（比如烈焰人的火球、恶魂的火球）。
     */
    @SubscribeEvent
    static void onProjectile(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity living
                && living.hasEffect(EVMobEffects.INVENTORY_LOCK)) {
            event.setCanceled(true);
        }
    }
}
