package com.water2h2o.elementalverse.element;

import java.util.List;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 把新装好的元素容器**并进已有的同元素堆叠**。
 *
 * <h2>为什么只在"写入的那一刻"合并</h2>
 * 曾经做过一个"每秒扫一遍背包主动合并"的版本，结果是玩家**故意分开放**的两叠
 * 过一会儿又被粘回去，很难受；连续采集时看起来也像"每五次就自动合并一次"。
 *
 * <p>所以合并只发生在**采集把新瓶子放进背包的那一瞬间**：
 * 这时合并不打扰任何人，而玩家手动分开的堆叠永远保持分开 —— 合并是采集行为的副产品，
 * 不是背景进程。
 *
 * <p>剩余时间按数量**加权平均**（作者定），数量上限取堆叠上限（64）。
 */
public final class ElementStackMerger {

    private ElementStackMerger() {
    }

    /**
     * 把一个刚装好的容器塞进背包，优先并入同元素、同载体的已有堆叠。
     *
     * @return 真的并入/放进去了返回 true
     */
    public static boolean add(Player player, ItemStack fresh) {
        if (fresh.isEmpty()) {
            return true;
        }

        Inventory inventory = player.getInventory();
        ElementType freshType = ElementVialItem.getElement(fresh);

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty() || existing.getItem() != fresh.getItem()) {
                continue;
            }
            if (ElementVialItem.getElement(existing) != freshType) {
                continue;
            }
            if (existing.getCount() >= existing.getMaxStackSize()) {
                continue;
            }

            mergeInto(existing, fresh);
            inventory.setChanged();
            if (fresh.isEmpty()) {
                return true;
            }
        }

        return inventory.add(fresh);
    }

    /**
     * 把 {@code from} 并进 {@code into}，剩余时间取加权平均。
     */
    private static void mergeInto(ItemStack into, ItemStack from) {
        int space = into.getMaxStackSize() - into.getCount();
        if (space <= 0) {
            return;
        }
        int moved = Math.min(space, from.getCount());

        int totalCount = into.getCount() + moved;
        long weighted = (long) ElementVialItem.getTimer(into) * into.getCount()
                + (long) ElementVialItem.getTimer(from) * moved;
        int averaged = totalCount == 0 ? 0 : (int) (weighted / totalCount);

        into.grow(moved);
        from.shrink(moved);

        // 只在时间真的不同时才写，省一次组件变更（也少一次客户端重绘）
        if (averaged != ElementVialItem.getTimer(into)) {
            ElementVialItem.setTimer(into, averaged);
            // 计时器变了，耐久条百分比要立刻跟着重算，
            // 否则要等下一秒的衰变 tick 才自愈，玩家会看到条卡一下
            ElementVialItem.refreshProgress(into);
        }
    }

    /** 供外部（容器、掉落物）复用的加权平均计算。 */
    public static int averageTimer(List<ItemStack> stacks) {
        long weighted = 0L;
        int count = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            weighted += (long) ElementVialItem.getTimer(stack) * stack.getCount();
            count += stack.getCount();
        }
        return count == 0 ? 0 : (int) (weighted / count);
    }
}
