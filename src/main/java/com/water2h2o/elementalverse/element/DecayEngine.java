package com.water2h2o.elementalverse.element;

import com.water2h2o.elementalverse.config.EVConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 衰变的全部计算。**纯粹的加减法，不碰注册表、不碰世界状态** —— 这样它最好测、最好改。
 *
 * <p>设计要点（总纲 2.5）：
 * <ul>
 *   <li>不存「还剩多久」，只存「已经走了多久」（{@code decay_timer} 组件）。</li>
 *   <li>推进是**离散的**：每 {@code decay.tickInterval} tick 累加一次，没加载就不累加。
 *       所以「世界停下，东西就停下」是天然成立的，不是额外判断出来的。</li>
 *   <li>环境修正（光在暗处加速等）在这里生效 —— 它按「这次推进有多少 tick」缩放，
 *       所以走在路上把光元素带进矿洞，下一跳就会变快。</li>
 * </ul>
 */
public final class DecayEngine {

    /** 单次推进的上限，防止有人把 tickInterval 调得离谱导致 int 溢出。 */
    private static final int MAX_STEP = 200;

    /** 计时器的硬上限。到顶就不再累加，避免长时间放置把 int 撑爆。 */
    private static final int TIMER_CAP = Integer.MAX_VALUE / 2;

    // ---------------------------------------------------------------- 查询

    /** 这个形态会不会随时间变。 */
    public static boolean decays(ElementType type) {
        return type != null && !type.isEmpty() && type.decays() && type.stage1() != null;
    }

    /** 当前处在哪一段。 */
    public static DecayStage getStage(ItemStack stack) {
        ElementType type = ElementVialItem.getElement(stack);
        if (!decays(type)) {
            return type.isEmpty() ? DecayStage.EMPTY : DecayStage.DEAD;
        }

        int[] durations = EVConfig.getStageDurations(type);
        if (durations[0] <= 0) {
            // 没进时长表（霜、芽、金）—— 阶段 01 视作不再变化
            return DecayStage.DEAD;
        }

        int timer = ElementVialItem.getTimer(stack);
        if (timer >= durations[0] + durations[1]) {
            return DecayStage.DEAD;
        }
        return timer >= durations[0] ? DecayStage.DERIVED : DecayStage.STABLE;
    }

    /** 距离下一次变质还有多少秒（只给调试 tooltip 用）。 */
    public static int getRemainingSeconds(ItemStack stack) {
        ElementType type = ElementVialItem.getElement(stack);
        if (!decays(type)) {
            return 0;
        }
        int[] durations = EVConfig.getStageDurations(type);
        int total = durations[0] + durations[1];
        int timer = ElementVialItem.getTimer(stack);
        return Math.max(0, (total - timer) / 20);
    }

    // ---------------------------------------------------------------- 推进

    /**
     * 推进一个物品堆叠的衰变计时。
     *
     * @param stack 要推进的堆叠（会被就地修改）
     * @param level 它所在的维度；为 null 表示算不出来（不做环境修正）
     * @param pos   它所在的位置；为 null 表示在玩家身上（会退回用玩家坐标）
     * @return true 表示它**越过了阈值、形态应该换了**，调用方负责替换物品
     */
    public static boolean tickStack(ItemStack stack, Level level, BlockPos pos, int elapsedTicks) {
        if (!EVConfig.isDecayEnabled()) {
            return false;
        }

        ElementType type = ElementVialItem.getElement(stack);
        if (!decays(type)) {
            return false;
        }

        int[] durations = EVConfig.getStageDurations(type);
        if (durations[0] <= 0) {
            return false;
        }

        int timer = ElementVialItem.getTimer(stack);
        int total = durations[0] + durations[1];
        if (timer >= total) {
            // 已经该换了；调用方这次没换成功的话，下一跳还会再报一次
            return true;
        }

        double multiplier = EVConfig.getGlobalMultiplier() * environmentalFactor(type, level, pos);
        int advanced = (int) Math.round(Math.min(elapsedTicks, MAX_STEP) * multiplier);
        if (advanced < 1) {
            // 减速超过 100% 时至少也走 0；只有真的被减到 0 才跳过
            advanced = 0;
        }

        int next = Math.min(TIMER_CAP, timer + advanced);
        ElementVialItem.setElement(stack, type, next);

        return next >= durations[0];
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
                return fast;   // 暗处加速
            }
            if (brightness >= 12) {
                return slow;   // 光下更稳
            }
            return 1.0D;
        }

        // 暗元素
        if (!EVConfig.darknessSpeedsShadow()) {
            return 1.0D;
        }
        if (brightness >= 12) {
            return fast;       // 见光就散
        }
        if (brightness <= 3) {
            return slow;
        }
        return 1.0D;
    }

    /** 光暗在当前环境下是"在变快"还是"在变慢"，给 tooltip 与调试用。 */
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
