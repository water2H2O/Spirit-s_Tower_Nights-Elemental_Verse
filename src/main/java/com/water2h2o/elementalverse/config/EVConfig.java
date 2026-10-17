package com.water2h2o.elementalverse.config;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 可调项。
 *
 * <p>衰变时长**实测后调**是预期内的事，所以这里把整套时长放在配置里，
 * 而不是散在代码常量里 —— 你不用重新编译就能试。
 *
 * <p>两套预设：
 * <ul>
 *   <li>{@link Preset#OBSERVE} —— 秒级，几分钟能看完全程。默认，给作者观察用。</li>
 *   <li>{@link Preset#NORMAL} —— 日级，正式手感（总纲 2.3 的数值）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class EVConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 一个游戏日有多少 tick（20 tick/秒 × 1200 秒）。 */
    private static final int TICKS_PER_DAY = 24000;

    // ---------------------------------------------------------------- 预设

    /** 时长预设。 */
    public enum Preset implements StringRepresentable {
        /** 秒级：水元素 20 秒进入下一阶段，全程 1 分钟。 */
        OBSERVE("observe"),
        /** 日级：总纲 2.3 的正式数值。 */
        NORMAL("normal");

        private final String id;

        Preset(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    // ---------------------------------------------------------------- 配置项

    public static final ModConfigSpec.EnumValue<Preset> DECAY_PRESET = BUILDER
            .comment("时长预设：observe = 秒级（方便观察自然变化），normal = 日级（正式手感）")
            .translation(EVLang.config("decayPreset"))
            .defineEnum("decay.preset", Preset.OBSERVE);

    public static final ModConfigSpec.BooleanValue DECAY_ENABLED = BUILDER
            .comment("总开关。关掉之后元素永远不会变质（调试用，也给不喜欢这机制的服主）")
            .translation(EVLang.config("decayEnabled"))
            .define("decay.enabled", true);

    public static final ModConfigSpec.DoubleValue DECAY_GLOBAL_MULTIPLIER = BUILDER
            .comment("全局衰变速度倍率。1.0 = 按预设；10.0 = 快十倍")
            .translation(EVLang.config("decayGlobalMultiplier"))
            .defineInRange("decay.globalMultiplier", 1.0D, 0.01D, 1000.0D);

    public static final ModConfigSpec.IntValue DECAY_TICK_INTERVAL = BUILDER
            .comment("多久检查一次（tick）。20 = 每秒一次")
            .translation(EVLang.config("decayTickInterval"))
            .defineInRange("decay.tickInterval", 20, 1, 200);

    public static final ModConfigSpec.IntValue DECAY_MAX_CONTAINERS_PER_TICK = BUILDER
            .comment("每秒最多检查多少个容器（性能兜底，防止有人造一万个箱子）")
            .translation(EVLang.config("decayMaxContainers"))
            .defineInRange("decay.maxContainersPerTick", 64, 1, 100000);

    public static final ModConfigSpec.DoubleValue LIGHT_DARKNESS_MULTIPLIER = BUILDER
            .comment("光在暗处 / 暗在亮处的加速倍率。亮处对光也会按这个倍率减速")
            .translation(EVLang.config("lightDarknessMultiplier"))
            .defineInRange("decay.lightDarknessMultiplier", 2.0D, 1.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue DARKNESS_SPEEDS_SHADOW = BUILDER
            .comment("暗元素是否在亮处加速（总纲 2.3：见光就散）")
            .translation(EVLang.config("darknessSpeedsShadow"))
            .define("decay.darknessSpeedsShadow", true);

    public static final ModConfigSpec.BooleanValue SHOWS_EXACT_SECONDS = BUILDER
            .comment("调试用：tooltip 显示精确剩余秒数。正式玩的时候应该是关的 —— 不给数字是刻意的")
            .translation(EVLang.config("showsExactSeconds"))
            .define("decay.showsExactSeconds", false);

    /** 主类在别的包里要用它注册配置，所以必须 public。 */
    public static final ModConfigSpec SPEC = BUILDER.build();

    // ---------------------------------------------------------------- 时长表

    /** 预设 → (元素 → {阶段①, 阶段②})，单位 tick。 */
    private static final Map<Preset, Map<ElementType, int[]>> PRESET_DURATIONS = buildPresets();

    /** 当前生效的时长表，配置加载/重载时刷新。 */
    private static Map<ElementType, int[]> durations = PRESET_DURATIONS.get(Preset.OBSERVE);

    /** 两套预设的完整时长表。 */
    private static Map<Preset, Map<ElementType, int[]>> buildPresets() {
        Map<ElementType, int[]> observe = new EnumMap<>(ElementType.class);
        observe.put(ElementType.WATER, seconds(20, 40));   // 全程 1 分钟
        observe.put(ElementType.GRASS, seconds(25, 50));
        observe.put(ElementType.FIRE, seconds(30, 60));
        observe.put(ElementType.WIND, seconds(35, 70));
        observe.put(ElementType.EARTH, seconds(45, 90));
        observe.put(ElementType.LIGHT, seconds(60, 120));
        observe.put(ElementType.SHADOW, seconds(60, 120));

        Map<ElementType, int[]> normal = new EnumMap<>(ElementType.class);
        normal.put(ElementType.EARTH, days(8, 16));
        normal.put(ElementType.WIND, days(3, 6));
        normal.put(ElementType.WATER, days(3.5D, 7.5D));
        normal.put(ElementType.GRASS, days(2.5D, 5));
        normal.put(ElementType.FIRE, days(2, 4));
        normal.put(ElementType.LIGHT, days(12, 20));
        normal.put(ElementType.SHADOW, days(12, 20));

        Map<Preset, Map<ElementType, int[]>> all = new EnumMap<>(Preset.class);
        all.put(Preset.OBSERVE, observe);
        all.put(Preset.NORMAL, normal);
        return all;
    }

    private static int[] seconds(int stage1, int stage2) {
        return new int[] { stage1 * 20, stage2 * 20 };
    }

    private static int[] days(double stage1, double stage2) {
        return new int[] { clampTicks(stage1 * TICKS_PER_DAY), clampTicks(stage2 * TICKS_PER_DAY) };
    }

    /** 单段时长上限：约 12 游戏日。防止两段相加把 int 撑爆（那会让东西永远不变坏）。 */
    private static int clampTicks(double ticks) {
        int max = 12 * TICKS_PER_DAY;
        if (ticks > max) {
            return max;
        }
        return Math.max(20, (int) Math.round(ticks));
    }

    // ---------------------------------------------------------------- 读取

    /** 当前预设。配置还没加载时给一个安全的默认值。 */
    public static Preset getPreset() {
        try {
            return DECAY_PRESET.get();
        } catch (IllegalStateException notLoadedYet) {
            return Preset.OBSERVE;
        }
    }

    public static boolean isDecayEnabled() {
        try {
            return DECAY_ENABLED.get();
        } catch (IllegalStateException notLoadedYet) {
            return true;
        }
    }

    public static double getGlobalMultiplier() {
        try {
            return DECAY_GLOBAL_MULTIPLIER.get();
        } catch (IllegalStateException notLoadedYet) {
            return 1.0D;
        }
    }

    public static int getTickInterval() {
        try {
            return DECAY_TICK_INTERVAL.get();
        } catch (IllegalStateException notLoadedYet) {
            return 20;
        }
    }

    public static int getMaxContainersPerTick() {
        try {
            return DECAY_MAX_CONTAINERS_PER_TICK.get();
        } catch (IllegalStateException notLoadedYet) {
            return 64;
        }
    }

    public static double getLightDarknessMultiplier() {
        try {
            return LIGHT_DARKNESS_MULTIPLIER.get();
        } catch (IllegalStateException notLoadedYet) {
            return 2.0D;
        }
    }

    public static boolean showsExactSeconds() {
        try {
            return SHOWS_EXACT_SECONDS.get();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    public static boolean darknessSpeedsShadow() {
        try {
            return DARKNESS_SPEEDS_SHADOW.get();
        } catch (IllegalStateException notLoadedYet) {
            return true;
        }
    }

    /**
     * 某个元素的 {阶段①, 阶段②} 时长，单位 tick。
     * 没在表里的元素（霜、芽、金）返回 {0, 0}，表示它们阶段 01 不参与衰变。
     */
    public static int[] getStageDurations(ElementType type) {
        int[] found = durations.get(type);
        if (found == null) {
            return new int[] { 0, 0 };
        }
        return found;
    }

    public static int[] getStageDurationsFor(Preset preset, ElementType type) {
        int[] found = PRESET_DURATIONS.get(preset).get(type);
        if (found == null) {
            return new int[] { 0, 0 };
        }
        return found;
    }

    /**
     * 某个形态的**绝对**衰变阈值（tick）—— 距离「下一次变形」的总时长。
     *
     * <p>计时器是绝对口径（跨形态连续累计，见 {@code DecayEngine} 的说明），
     * 所以阈值必须也是绝对的：
     *
     * <ul>
     *   <li>**稳定元素**：阈值 = 预设里的阶段①。水（观察预设）= 400 tick。</li>
     *   <li>**衍生元素**：阈值 = 它的父稳定元素的「阶段① + 阶段②」。
     *       冰 = 400 + 800 = 1200 tick，即水从采集到变成死寂共 60 秒。</li>
     * </ul>
     *
     * <p>返回 0 表示不参与衰变（霜、金、芽这类还没接入链的支线元素）。
     */
    public static int getThreshold(ElementType type) {
        if (type == null || type.isEmpty() || !type.decays()) {
            return 0;
        }

        int[] own = getStageDurations(type);
        if (own[0] > 0) {
            // 稳定元素：自己的阶段①就是阈值
            return own[0];
        }
        if (own[1] > 0) {
            // 若某天给衍生元素单独配了时长，直接用
            return own[1];
        }

        // 衍生元素：找它的父稳定元素，取「阶段① + 阶段②」
        for (ElementType parent : ElementType.values()) {
            if (parent.isBaseElement() && parent.stage1() == type) {
                int[] parentDurations = getStageDurations(parent);
                if (parentDurations[0] > 0) {
                    return parentDurations[0] + parentDurations[1];
                }
            }
        }
        return 0;
    }

    /** 配置加载或重载时刷新生效的时长表。 */
    @SubscribeEvent
    static void onConfigLoad(ModConfigEvent event) {
        Preset preset = DECAY_PRESET.get();
        durations = PRESET_DURATIONS.get(preset);
    }

    /** 给日志 / 调试用的人话描述。 */
    public static String describe(ElementType type) {
        int[] table = getStageDurations(type);
        if (table[0] <= 0) {
            return String.format(Locale.ROOT, "%s: 不衰变", type.id());
        }
        return String.format(Locale.ROOT, "%s: %d tick + %d tick", type.id(), table[0], table[1]);
    }

    private EVConfig() {
    }
}
