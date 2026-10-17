package com.water2h2o.elementalverse.element;

import com.water2h2o.elementalverse.registry.EVItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 「推进一格并换形态」的共享实现。
 *
 * <p>玩家背包、掉落物、方块容器里的元素都要走这一步，所以逻辑只写一份。
 *
 * <h2>为什么区分「就地改」和「替换」</h2>
 * 稳定元素 → 衍生元素只换数据组件，**物品本身没变**。这时如果返回新 {@link ItemStack}
 * 让调用方替换，客户端会把整格物品当成换了个东西，手持时会看到物品反复"丢掉再捡起"。
 * 所以：只换组件的情况**就地改**（返回 null），只有物品类型真的变了才返回新堆叠。
 */
public final class DecayAdvance {

    /**
     * 推进一个堆叠。
     *
     * @param multiplier 额外的时间倍率（容器 / 冷却手段用；1.0 = 不变）
     * @return 需要替换成的新堆叠；已就地修改、或无需变化时返回 null
     */
    public static ItemStack advance(ItemStack stack, Level level, BlockPos pos,
                                    int elapsedTicks, double multiplier) {
        int scaled = (int) Math.round(elapsedTicks * multiplier);
        if (scaled <= 0) {
            return null;
        }
        if (!DecayEngine.tickStack(stack, level, pos, scaled)) {
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
            // 走到尽头：容器消失，留下死寂尘（4 尘压 1 块）
            return new ItemStack(EVItems.DEAD_DUST.get(), count);
        }

        // 只换组件：就地改，保留计时器（绝对口径）
        ElementVialItem.setElement(stack, next, timer);
        return null;
    }

    /** 单次推进，倍率 1.0。 */
    public static ItemStack advance(ItemStack stack, Level level, BlockPos pos, int elapsedTicks) {
        return advance(stack, level, pos, elapsedTicks, 1.0D);
    }

    private DecayAdvance() {
    }
}
