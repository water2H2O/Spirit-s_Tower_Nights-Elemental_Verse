package com.water2h2o.elementalverse.reaction;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.element.ElementAxis;
import com.water2h2o.elementalverse.element.ElementType;

/**
 * 态的六个参数（《魔法体系原理》五之二）。
 *
 * <p>第三个投入口决定这些值 —— 它**不贡献强度**，只决定"怎么发生"。
 * 这是把组合爆炸从 512 降下来的关键：三个格子是三次独立查表，不是三阶排列。
 *
 * @param intensity   烈度（火）：伤害 / 灼烧时刻的倍数
 * @param radius      范围（风）：格
 * @param durationSec 持续（光）：秒
 * @param buildRadius 造/拆（地）：格；正数=造物，负数=破坏
 * @param chain       传播（水）：可以额外跳几个目标
 * @param polarity    极性（草）：把伤害转成治疗的比例 0~1
 */
public record SpellParams(double intensity, double radius, int durationSec,
                          int buildRadius, int chain, double polarity) {

    /** 什么都没放时的默认态。 */
    public static final SpellParams DEFAULT = new SpellParams(1.0D, 1.5D, 1, 0, 0, 0.0D);

    /** 火：烈度。留递减，避免"投满 9 个 = 3.5 倍"变成唯一最优解。 */
    private static final double[] FIRE_INTENSITY = {1.0D, 2.0D, 3.5D};
    /** 风：范围。 */
    private static final double[] WIND_RADIUS = {1.5D, 3.0D, 5.0D};
    /** 光：持续秒数。 */
    private static final int[] LIGHT_SECONDS = {2, 5, 12};
    /** 地：造/拆半径。 */
    private static final int[] EARTH_RADIUS = {1, 2, 3};
    /** 水：连锁目标数。 */
    private static final int[] WATER_CHAIN = {1, 2, 4};
    /** 草：治疗比例。 */
    private static final double[] GRASS_POLARITY = {0.3D, 0.6D, 1.0D};

    /**
     * 从申明算出六参数。
     *
     * <p>只有**第三个投入口**的元素参与；
     * 它带的负分量（目前只有暗 = 负光）会让参数取反。
     */
    public static SpellParams of(Declaration declaration) {
        ElementType state = declaration.stateElement();
        int tier = declaration.stateTier();
        if (state == null || state.isEmpty() || tier <= 0) {
            return DEFAULT;
        }

        int index = Math.min(tier, 3) - 1;
        ElementAxis axis = state.value().dominant();
        double sign = state.value().dominantSign();

        double intensity = 1.0D;
        double radius = DEFAULT.radius();
        int duration = DEFAULT.durationSec();
        int build = 0;
        int chain = 0;
        double polarity = 0.0D;

        if (axis != null) {
            switch (axis) {
                case FIRE -> intensity = FIRE_INTENSITY[index];
                case WIND -> radius = WIND_RADIUS[index];
                case LIGHT -> duration = LIGHT_SECONDS[index];
                case EARTH -> build = (int) (EARTH_RADIUS[index] * sign);
                case WATER -> chain = WATER_CHAIN[index];
                case GRASS -> polarity = GRASS_POLARITY[index];
            }
        }

        // 负分量取反：暗当态 = 光取反（时间更短），负火的元素同理
        if (sign < 0) {
            duration = Math.max(1, duration / 3);
            intensity = Math.max(0.5D, intensity * 0.5D);
        }

        return new SpellParams(intensity, radius, duration, build, chain, polarity);
    }

    /** 持续时间的 tick 形式。 */
    public int durationTicks() {
        return Math.max(1, durationSec) * 20;
    }
}
