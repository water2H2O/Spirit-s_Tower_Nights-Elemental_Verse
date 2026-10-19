package com.water2h2o.elementalverse.element;

import com.water2h2o.elementalverse.config.EVConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 衰变的全部计算。**纯粹的加减法，不碰注册表、不碰世界状态** —— 这样它最好测、最好改。
 *
 * <h2>计时口径：绝对，不重置</h2>
 * {@code decay_timer} 是「从装进瓶子到现在一共过了多少 tick」，**跨形态连续累计，不重置**。
 *
 * <p>这条是踩过坑才定下来的：早先每次变形都把计时器清零，结果
 * 「稳定元素 → 衍生元素」之后新形态又从 0 开始算，而衍生元素没有自己的时长条目，
 * 于是**永远到不了死寂物质**。改用绝对口径后：
 *
 * <pre>
 *   水 ：阈值 400            → 400 时变成霜（计时器仍是 400）
 *   霜 ：阈值 400+800=1200   → 1200 时变成死寂物质
 * </pre>
 *
 * 每条链的绝对阈值 = 父稳定元素的「阶段① + 阶段②」（见 {@link EVConfig#getThreshold}）。
 */
public final class DecayEngine {

    /** 计时器的硬上限。到顶就不再累加，避免长时间放置把 int 撑爆。 */
    private static final int TIMER_CAP = Integer.MAX_VALUE / 2;

    // ---------------------------------------------------------------- 查询

    /** 这个形态会不会随时间变。 */
    public static boolean decays(ElementType type) {
        return type != null && !type.isEmpty() && type.decays();
    }

    /** 当前处在哪一段。 */
    public static DecayStage getStage(ItemStack stack) {
        ElementType type = ElementVialItem.getElement(stack);
        if (!decays(type)) {
            return type.isEmpty() ? DecayStage.EMPTY : DecayStage.DEAD;
        }

        int threshold = EVConfig.getThreshold(type);
        if (threshold <= 0) {
            // 进了衰变表但没算出阈值（配置写坏 / 支线元素）—— 视作不再变化
            return DecayStage.DEAD;
        }

        if (ElementVialItem.getTimer(stack) >= threshold) {
            return DecayStage.DEAD;
        }
        return type.isBaseElement() ? DecayStage.STABLE : DecayStage.DERIVED;
    }

    /** 距离下一次变质还有多少秒（只给调试 tooltip 用）。 */
    public static int getRemainingSeconds(ItemStack stack) {
        ElementType type = ElementVialItem.getElement(stack);
        if (!decays(type)) {
            return 0;
        }
        int threshold = EVConfig.getThreshold(type);
        int timer = ElementVialItem.getTimer(stack);
        return Math.max(0, (threshold - timer) / 20);
    }

    // ---------------------------------------------------------------- 推进

    /**
     * 推进一个物品堆叠的衰变计时（只动计时器组件，不换形态）。
     *
     * @param stack       要推进的堆叠（会被就地修改）
     * @param level       它所在的维度；为 null 表示算不出来（不做环境修正）
     * @param pos         它所在的位置
     * @return true 表示它**越过了阈值、形态应该换了**，调用方负责替换／就地转换
     */
    public static boolean tickStack(ItemStack stack, Level level, BlockPos pos, int elapsedTicks) {
        if (!EVConfig.isDecayEnabled()) {
            return false;
        }

        ElementType type = ElementVialItem.getElement(stack);
        if (!decays(type)) {
            return false;
        }

        int threshold = EVConfig.getThreshold(type);
        if (threshold <= 0) {
            return false;
        }

        int timer = ElementVialItem.getTimer(stack);
        if (timer >= threshold) {
            // 已经到点了；调用方这次没换成功的话，下一跳还会再报一次
            return true;
        }

        double multiplier = EVConfig.getGlobalMultiplier() * environmentalFactor(type, level, pos);
        // 容器扫描可能一次补上很长的时间（大世界轮转 / 区块刚加载），不能再截断到 200 tick。
        long advanced = Math.round((double) Math.max(0, elapsedTicks) * multiplier);
        if (advanced < 1L) {
            return false;
        }

        int next = (int) Math.min(TIMER_CAP, (long) timer + advanced);
        ElementVialItem.setTimer(stack, next);

        // 耐久条用的百分比：整数变了才写（所以每秒最多一次小小更新）
        ElementVialItem.setProgress(stack, (int) Math.min(100L, next * 100L / threshold));

        return next >= threshold;
    }

    // ---------------------------------------------------------------- 环境修正

    /**
     * 光在暗处加速、暗在亮处加速。两个方向都会**减速**，但都不会完全停止 ——
     * 这是刻意的：给光打一盏灯是有效但不免费的缓解手段（总纲 2.3）。
     */
    public static double environmentalFactor(ElementType type, Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return 1.0D;
        }
        if (type != ElementType.LIGHT && type != ElementType.SHADOW) {
            return 1.0D;
        }

        int brightness;
        try {
            brightness = level.getMaxLocalRawBrightness(pos);
        } catch (RuntimeException unreachable) {
            return 1.0D;
        }

        double fast = EVConfig.getLightDarknessMultiplier();
        double slow = 1.0D / fast;

        if (type == ElementType.LIGHT) {
            if (brightness <= 3) {
                return fast;
            }
            if (brightness >= 12) {
                return slow;
            }
            return 1.0D;
        }

        if (!EVConfig.darknessSpeedsShadow()) {
            return 1.0D;
        }
        if (brightness >= 12) {
            return fast;
        }
        if (brightness <= 3) {
            return slow;
        }
        return 1.0D;
    }

    /** 光暗在当前环境下是"在变快"还是"在变慢"，给调试用。 */
    public static String describeEnvironment(ElementType type, Level level, BlockPos pos) {
        double factor = environmentalFactor(type, level, pos);
        if (factor > 1.0D) {
            return "加速 x" + factor;
        }
        if (factor < 1.0D) {
            return "减缓 x" + factor;
        }
        return "正常";
    }

    private DecayEngine() {
    }
}
