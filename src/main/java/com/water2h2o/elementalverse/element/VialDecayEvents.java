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
            if (!ElementCarrier.isCarrier(stack)) {
                continue;
            }
            ItemStack replacement = advance(stack, level, pos, interval);
            if (replacement != null) {
                player.getInventory().setItem(slot, replacement);
            }
        }
    }

    /** 掉在地上的元素容器：区块卸载就不再 tick，正好符合"世界停下就停下"。 */
    private static void tickItemEntity(ItemEntity itemEntity, int interval) {
        ItemStack stack = itemEntity.getItem();
        if (!ElementCarrier.isCarrier(stack)) {
            return;
        }

        ItemStack replacement = advance(stack, itemEntity.getCommandSenderWorld(),
                itemEntity.blockPosition(), interval);
        if (replacement != null) {
            itemEntity.setItem(replacement);
        }
    }

    // ---------------------------------------------------------------- 换形态

    /**
     * 推进一个堆叠，到点就换形态。
     *
     * <h2>为什么要区分「就地改」和「替换」</h2>
     * 「稳定元素 → 衍生元素」只换数据组件，**物品本身没变**。这时如果也返回一个
     * 新的 {@link ItemStack} 让调用方替换，客户端会把整格物品当成换了个东西：
     * 手持时会看到物品**反复"丢掉再捡起"的动画**。
     *
     * <p>所以：只换组件的情况**就地改**（返回 null），只有物品类型真的变了
     * （衍生元素 → 死寂物质）才返回新堆叠。这是模组里处理"会变的物品"的常见做法。
     *
     * @return 需要替换成的新堆叠；已就地修改、或无需变化时返回 null
     */
    private static ItemStack advance(ItemStack stack, Level level, BlockPos pos, int elapsedTicks) {
        if (!DecayEngine.tickStack(stack, level, pos, elapsedTicks)) {
            return null;
        }

        ElementType current = ElementVialItem.getElement(stack);
        if (current.isEmpty() || !DecayEngine.decays(current)) {
            return null;
        }

        ElementType next = current.stage1();
        int count = stack.getCount();
        int timer = ElementVialItem.getTimer(stack);

        if (next == null) {
            // 走到尽头：容器消失，留下**死寂尘**（作者决定：先给尘，4 尘压 1 块）
            return new ItemStack(EVItems.DEAD_DUST.get(), count);
        }

        // 只换组件：就地改，保留计时器（绝对口径，见 DecayEngine 注释）
        ElementVialItem.setElement(stack, next, timer);
        return null;
    }

    private VialDecayEvents() {
    }
}
