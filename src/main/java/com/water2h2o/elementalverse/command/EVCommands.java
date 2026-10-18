package com.water2h2o.elementalverse.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import com.water2h2o.elementalverse.element.ElementAxis;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementValue;
import com.water2h2o.elementalverse.reaction.Reaction;
import com.water2h2o.elementalverse.reaction.ReactionManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 调试命令：把"算出来的元素值"直接打出来。
 *
 * <p>元素值这套东西玩家不该看见（作者要求"底层可以很复杂，玩家看到的必须简单"），
 * 但它在开发期必须可观测 —— 否则调数值全靠猜。
 *
 * <pre>
 *   /ev value fire        看某元素的向量、强度、主成分
 *   /ev clash water fire  看"水遇到火"相减之后剩下什么（灭火的原理）
 *   /ev reactions         列出当前数据包加载的所有反应式
 * </pre>
 */
@EventBusSubscriber(modid = "elemental_verse")
public final class EVCommands {

    private EVCommands() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("ev")
                .requires(source -> source.hasPermission(0))

                .then(Commands.literal("value")
                        .then(Commands.argument("element", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "element");
                                    return printValue(context.getSource(), name);
                                })))

                .then(Commands.literal("clash")
                        .then(Commands.argument("first", StringArgumentType.word())
                                .then(Commands.argument("second", StringArgumentType.word())
                                        .executes(context -> {
                                            String first = StringArgumentType.getString(context, "first");
                                            String second = StringArgumentType.getString(context, "second");
                                            return clash(context.getSource(), first, second);
                                        }))))

                .then(Commands.literal("heat")
                        .executes(context -> {
                            double heat = com.water2h2o.elementalverse.heat.ChunkHeat.get(
                                    context.getSource().getLevel(),
                                    context.getSource().getPlayerOrException().blockPosition());
                            context.getSource().sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal(
                                            "当前区块余热：" + Math.round(heat * 10) / 10.0));
                            return 1;
                        }))

                .then(Commands.literal("reactions")
                        .executes(context -> {
                            for (String line : ReactionManager.describeAll()) {
                                context.getSource().sendSystemMessage(Component.literal(line));
                            }
                            return ReactionManager.all().size();
                        })));
    }

    private static int printValue(CommandSourceStack source, String name) {
        ElementType type = ElementType.byName(name);
        if (type == null) {
            source.sendFailure(Component.literal("未知元素：" + name));
            return 0;
        }
        ElementValue value = type.value();
        ElementAxis dominant = value.dominant();
        source.sendSystemMessage(Component.literal(
                name + " = " + value
                        + "  强度=" + round(value.magnitude())
                        + "  主成分=" + (dominant == null ? "无（湮灭）" : dominant.getSerializedName())));
        return 1;
    }

    private static int clash(CommandSourceStack source, String firstName, String secondName) {
        ElementType first = ElementType.byName(firstName);
        ElementType second = ElementType.byName(secondName);
        if (first == null || second == null) {
            source.sendFailure(Component.literal("未知元素"));
            return 0;
        }

        ElementValue net = first.value().clashTo(second.value());
        ElementAxis dominant = net.dominant();

        source.sendSystemMessage(Component.literal(
                firstName + " 遇上 " + secondName
                        + " → 剩下 " + net
                        + "  强度=" + round(net.magnitude())
                        + "  归零=" + net.isZero()
                        + "  主成分=" + (dominant == null ? "无（湮灭）" : dominant.getSerializedName())));

        // 顺便看看这一步能不能精确匹配到某条反应
        Reaction matched = ReactionManager.match(net).orElse(null);
        source.sendSystemMessage(Component.literal(
                "  精确配方匹配：" + (matched == null ? "无（会走主成分兜底）" : matched.phenomenon().type())));
        return 1;
    }

    private static String round(double value) {
        return String.valueOf(Math.round(value * 1000) / 1000.0);
    }
}
