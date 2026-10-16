package com.water2h2o.elementalverse.element;

import com.water2h2o.elementalverse.config.EVConfig;
import com.water2h2o.elementalverse.registry.EVItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 把衰变挂到游戏循环上。
 *
 * <p>为什么用 {@link EntityTickEvent.Post} 而不是 {@code Item#inventoryTick}：
 * 后者只覆盖「容器里」的物品，**掉在地上的不算**。而掉落物本身是实体，
 * 用实体 tick 事件能一次覆盖两种情况，也不用担心玩家背包被计两次。
 *
 * <p>频率：每 {@code decay.tickInterval}（默认 20）tick 走一次，用
 * {@code level.getGameTime()} 取模触发 —— 所以它是**离散的**，区块没加载就没有 tick，
 * 「世界停下，东西就停下」因此天然成立。
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class VialDecayEvents {

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        // 用 getCommandSenderWorld() 而不是 level()：前者在多个版本间都稳定
        Level level = event.getEntity().getCommandSenderWorld();
        if (level.isClientSide()) {
            return;
        }

        int interval = EVConfig.getTickInterval();
        if (level.getGameTime() % interval != 0L) {
            return;
        }
        if (!EVConfig.isDecayEnabled()) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            tickPlayerInventory(player, interval);
        } else if (event.getEntity() instanceof ItemEntity itemEntity) {
            tickItemEntity(itemEntity, interval);
        }
    }

    /** 玩家背包：约 40 格，每秒一次，开销可以忽略。 */
    private static void tickPlayerInventory(Player player, int interval) {
        BlockPos pos = player.blockPosition();
        Level level = player.getCommandSenderWorld();

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.is(EVItems.ELEMENT_VIAL.get())) {
                continue;
            }
            ItemStack replacement = advance(stack, level, pos, interval);
            if (replacement != null) {
                player.getInventory().setItem(slot, replacement);
            }
        }
    }

    /** 掉在地上的元素瓶：区块卸载就不再 tick，正好符合"世界停下就停下"。 */
    private static void tickItemEntity(ItemEntity itemEntity, int interval) {
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty() || !stack.is(EVItems.ELEMENT_VIAL.get())) {
            return;
        }

        ItemStack replacement = advance(stack, itemEntity.getCommandSenderWorld(), itemEntity.blockPosition(), interval);
        if (replacement != null) {
            itemEntity.setItem(replacement);
        }
    }

    // ---------------------------------------------------------------- 换形态

    /**
     * 推进一个堆叠。越过阈值就把它换成下一个形态。
     *
     * @return 换过之后的堆叠；不需要换就返回 null（调用方什么也不做）
     */
    private static ItemStack advance(ItemStack stack, Level level, BlockPos pos, int elapsedTicks) {
        if (!DecayEngine.tickStack(stack, level, pos, elapsedTicks)) {
            return null;
        }

        ElementType current = ElementVialItem.getElement(stack);
        if (current.isEmpty() || !DecayEngine.decays(current) || !DecayEngine.getStage(stack).canDecay()) {
            return null;
        }

        ElementType next = current.stage1();
        int count = stack.getCount();

        if (next == null) {
            // 走到尽头：瓶子消失，原地留下一块死寂物质
            return new ItemStack(EVItems.DEAD_MATTER.get(), count);
        }

        ItemStack replacement = ElementVialItem.createStack(next);
        replacement.setCount(count);
        return replacement;
    }

    private VialDecayEvents() {
    }
}
