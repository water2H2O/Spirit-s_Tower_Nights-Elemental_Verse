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
 * <p>消耗用"游戏时间取模"判定，因此不需要给每个小包存一个计时器 —— 无状态、可存档跳变也不出错。
 */
public final class PouchDecay {

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

            ItemStack replacement = DecayAdvance.advance(inside, level, pos, elapsedTicks, multiplier);
            if (replacement != null) {
                items.set(slot, replacement);
            }
            changed = true;
        }

        // 烧冷却剂：霜 30 秒一个、冰 60 秒一个（作者定：一个霜 = 半个冰）
        // 只在**真的有东西需要保护**时才烧
        if (PouchContents.hasProtectedContents(items)) {
            long period = PouchContents.burnInterval(items);
            if (period > 0 && level.getGameTime() % period == 0L) {
                if (PouchContents.consumeCoolant(items)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            PouchContents.set(pouch, items);
        }
    }

    private PouchDecay() {
    }
}
