package com.water2h2o.elementalverse.element;

import com.water2h2o.elementalverse.config.EVConfig;
import com.water2h2o.elementalverse.registry.EVItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
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

            // 元素小包：里面的东西用自己的冷却规则推进；界面打开期间暂停
            if (stack.is(EVItems.ELEMENT_POUCH.get())) {
                if (!com.water2h2o.elementalverse.menu.PouchMenu.isOpen(stack)) {
                    PouchDecay.tick(stack, level, pos, interval);
                }
                continue;
            }

            if (!ElementCarrier.isCarrier(stack)) {
                continue;
            }
            ItemStack replacement = advance(stack, level, pos, interval);
            if (replacement != null) {
                player.getInventory().setItem(slot, replacement);
            }
        }

        // 鼠标拿着的那一叠不在任何槽位里，必须单独推进（issue #7）
        tickCarried(player, interval);
    }

    /**
     * 鼠标拿着的那一叠（容器界面里的 carried stack）也要老。
     *
     * <p>issue #7：从一叠里拆一半拿在手上时，它既不在背包槽位、也不在地面，
     * 原来的扫描完全覆盖不到 —— 拿着的那半永远停在拆分那一刻，
     * 放回去就和原叠"不是同一个物品"（原版按数据组件判等），合并不上，
     * 右键分堆的表现因此很怪。这里让它和背包里的兄弟同步老化。
     */
    private static void tickCarried(Player player, int interval) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return;
        }
        ItemStack carried = menu.getCarried();
        if (!ElementCarrier.isCarrier(carried)) {
            return;
        }
        ItemStack replacement = advance(carried, player.getCommandSenderWorld(),
                player.blockPosition(), interval);
        if (replacement != null) {
            menu.setCarried(replacement);
        }
    }

    /**
     * 捡起地上的元素时，把两边的时间拉到**加权平均**。
     *
     * <p>issue #7：原版按"物品 + 全部数据组件"判断能不能合并，而 {@code decay_timer}
     * 是每叠各存一份的 —— 两边时间差一个 tick 就合并不上，只能另占一格。
     * 这里在真正放进背包**之前**把两边改成同一个值，原版照常合并。
     * 时间取加权平均，和采集时的并入（{@link ElementStackMerger}）同一口径。
     */
    @SubscribeEvent
    static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (!EVConfig.isDecayEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack picked = event.getItemEntity().getItem();
        if (!ElementCarrier.isCarrier(picked)
                || !DecayEngine.decays(ElementVialItem.getElement(picked))) {
            return;
        }

        ElementType type = ElementVialItem.getElement(picked);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty() || existing.getItem() != picked.getItem()
                    || ElementVialItem.getElement(existing) != type) {
                continue;
            }
            int space = existing.getMaxStackSize() - existing.getCount();
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, picked.getCount());
            long weighted = (long) ElementVialItem.getTimer(existing) * existing.getCount()
                    + (long) ElementVialItem.getTimer(picked) * moved;
            int total = existing.getCount() + moved;
            int averaged = total == 0 ? 0 : (int) (weighted / total);

            if (averaged != ElementVialItem.getTimer(existing)) {
                ElementVialItem.setTimer(existing, averaged);
                ElementVialItem.refreshProgress(existing);
            }
            if (averaged != ElementVialItem.getTimer(picked)) {
                ElementVialItem.setTimer(picked, averaged);
                ElementVialItem.refreshProgress(picked);
            }
            inventory.setChanged();
            // 只对齐一个目标：原版的 add 会继续处理剩下的数量
            return;
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
