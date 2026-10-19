package com.water2h2o.elementalverse.reaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import com.water2h2o.elementalverse.SpiritsTowerNightsElementalVerse;
import com.water2h2o.elementalverse.element.ElementValue;

import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

/**
 * 反应表：从数据包 `data/&lt;namespace&gt;/reaction/*.json` 读取。
 *
 * <p>加一条魔法 = 写一个 JSON，不动 Java。
 *
 * <h2>两级匹配</h2>
 * <ol>
 *   <li><b>精确配方</b>：投入向量与某条反应的消耗相等（容差见 {@link ElementValue#EPSILON}）</li>
 *   <li><b>主成分兜底</b>：没有精确配方时，由调用方按主成分律生成通用现象</li>
 * </ol>
 *
 * 第二级是"自由"的保证：玩家永远可以"扔一堆火"得到一个大火，
 * 而不是对着"没有对应配方"发呆。
 */
public final class ReactionManager {

    public static final String DIRECTORY = "reaction";

    /** 资源重载在后台线程写、服务端线程读，所以用 volatile。 */
    private static volatile List<Reaction> reactions = List.of();

    private ReactionManager() {
    }

    /** 服务端启动 / 数据包重载时调用。 */
    public static void reload(ResourceManager resources, RegistryOps<JsonElement> ops) {
        List<Reaction> loaded = new ArrayList<>();

        for (Resource resource : resources.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).values()) {
            try (var reader = resource.openAsReader()) {
                JsonElement json = com.google.gson.JsonParser.parseReader(reader);
                Reaction.CODEC.parse(ops, json)
                        .resultOrPartial(error -> SpiritsTowerNightsElementalVerse.LOGGER
                                .warn("[元素之诗] 反应式解析失败：{}", error))
                        .ifPresent(loaded::add);
            } catch (Exception failure) {
                SpiritsTowerNightsElementalVerse.LOGGER.warn("[元素之诗] 反应式读取失败", failure);
            }
        }

        reactions = List.copyOf(loaded);

        // 守恒自检：写坏了要在日志里看见，而不是玩家先发现
        for (Reaction reaction : reactions) {
            if (!reaction.isBalanced()) {
                SpiritsTowerNightsElementalVerse.LOGGER.warn(
                        "[元素之诗] 反应式不守恒（现象当量 > 投入强度）：{}", reaction);
            }
        }

        SpiritsTowerNightsElementalVerse.LOGGER.info("[元素之诗] 反应表就绪：{} 条", reactions.size());
    }

    /**
     * 条件匹配：优先命中"要求更多分量"的那条（更特殊的配方优先）。
     */
    public static Optional<Reaction> match(ElementValue input) {
        Reaction best = null;
        double bestSize = -1;
        for (Reaction reaction : reactions) {
            if (!reaction.accepts(input)) {
                continue;
            }
            double size = reaction.consume().magnitude();
            if (size > bestSize) {
                bestSize = size;
                best = reaction;
            }
        }
        return Optional.ofNullable(best);
    }

    public static List<Reaction> all() {
        return reactions;
    }

    /** 调试用：把当前表打出来。 */
    public static List<String> describeAll() {
        List<String> lines = new ArrayList<>();
        for (Reaction reaction : reactions) {
            lines.add(reaction.consume() + " -> " + reaction.phenomenon().type()
                    + "(" + reaction.phenomenon().element().getSerializedName()
                    + " " + reaction.phenomenon().amount() + "/s x " + reaction.phenomenon().duration()
                    + "t, 当量 " + reaction.phenomenon().cost() + ")");
        }
        return lines;
    }
}
