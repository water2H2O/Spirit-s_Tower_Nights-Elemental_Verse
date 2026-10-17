package com.water2h2o.elementalverse.element.gathering;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 矿物 → 土元素产出量。
 *
 * <p>作者 2026-10-17：地元素**不从石头取，改从矿物取**，不同稀有度给不同量。
 * 这样"挖矿"本身就有意义，也为以后"地元素在家长矿"（环境元素，总纲 12.2）留好接口。
 */
public final class OreYield {

    private OreYield() {
    }

    /**
     * @return 每次采集产出的份数；不是矿物返回 0
     */
    public static int of(BlockState state) {
        // 常见：1 份
        if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
            return 1;
        }
        // 中等：2 份
        if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.NETHER_QUARTZ_ORE)) {
            return 2;
        }
        // 稀有：3 份
        if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS)) {
            return 3;
        }
        return 0;
    }
}
