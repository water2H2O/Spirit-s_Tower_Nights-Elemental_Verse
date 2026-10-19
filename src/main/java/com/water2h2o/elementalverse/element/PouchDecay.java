package com.water2h2o.elementalverse.element;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 元素小包的**变质逻辑**：包里的元素也会坏，冷却剂会减缓它。
 *
 * <p>规则（与 {@link PouchContents} 配套）：
 * <ul>
 *   <li>包里有霜 → 其余东西 ×0.8；有冰 → ×0.5（冰更好，优先取它）</li>
 *   <li>冷却剂自己被当作"燃料"，**不参与衰变**</li>
 *   <li>每 60 秒消耗一个冷却剂，先耗霜、后耗冰</li>
 * </ul>
 *
 * <p>冷却剂按**实际经过的时间**消耗：用这次 tick 覆盖的区间里跨过了几个 30/60 秒来算，
 * 不依赖扫描发生在哪个 tick，因此大世界轮转 / 区块刚加载也不会漏烧。
 */
public final class PouchDecay {

    /** 新放入（timer = 0）的东西最多补一个扫描周期，避免大世界轮转造成一次性大幅衰变。 */
    private static final int FRESH_GRACE_TICKS = 100;

    /** 推进一个小包里的内容。 */
    public static void tick(ItemStack pouch, Level level, BlockPos pos, int elapsedTicks) {
        List<ItemStack> items = PouchContents.get(pouch);
        boolean changed = false;

        double multiplier = PouchContents.coolingMultiplier(items);

        // 只有存放格会衰变；燃料格里的霜/冰是被"花掉"的燃料
        for (int slot = 0; slot < PouchContents.STORAGE; slot++) {
            ItemStack inside = items.get(slot);
            if (inside.isEmpty() || !ElementCarrier.isCarrier(inside)) {
                continue;
            }

            ElementType beforeType = ElementVialItem.getElement(inside);
            int beforeTimer = ElementVialItem.getTimer(inside);
            int stackElapsed = beforeTimer == 0
                    ? Math.min(elapsedTicks, FRESH_GRACE_TICKS)
                    : elapsedTicks;
            ItemStack replacement = DecayAdvance.advance(inside, level, pos, stackElapsed, multiplier);
            if (replacement != null) {
                items.set(slot, replacement);
                changed = true;
            } else if (ElementVialItem.getElement(inside) != beforeType
                    || ElementVialItem.getTimer(inside) != beforeTimer) {
                changed = true;
            }
        }

        // 烧冷却剂：霜 30 秒一个、冰 60 秒一个（作者定：一个霜 = 半个冰）
        // 只在**真的有东西需要保护**时才烧。按本次 tick 覆盖的时间段内跨过了几个周期来扣，
        // 这样“每 100 tick 扫一批区块”也不会因为没扫在 600 的整数倍上而漏烧。
        long period = PouchContents.burnInterval(items);
        if (period > 0 && elapsedTicks > 0) {
            long now = level.getGameTime();
            long before = now - elapsedTicks;
            long due = now / period - before / period;
            for (long i = 0; i < due; i++) {
                if (!PouchContents.hasProtectedContents(items)) {
                    break;
                }
                if (!PouchContents.consumeCoolant(items)) {
                    break;
                }
                changed = true;
            }
        }

        if (changed) {
            PouchContents.set(pouch, items);
        }
    }

    private PouchDecay() {
    }
}
