package com.water2h2o.elementalverse.element;

import java.util.List;

import com.water2h2o.elementalverse.config.EVConfig;
import com.water2h2o.elementalverse.element.gathering.Gathering;
import com.water2h2o.elementalverse.registry.EVDataComponents;
import com.water2h2o.elementalverse.registry.EVItems;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.water2h2o.elementalverse.element.ElementAxis;
import com.water2h2o.elementalverse.element.ElementValue;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 元素瓶 —— 模组唯一的元素载体。
 *
 * <p>「空元素瓶」和「水元素瓶」是**同一个物品**，靠 {@link EVDataComponents#ELEMENT} 区分。
 * 这样采集出来天然就是「装着东西的瓶子」，而加新元素只需要动枚举。
 *
 * <h2>图标怎么分</h2>
 * 物品本身只有一个，图标靠 {@code custom_model_data} 组件 + 物品模型的 overrides 分发
 * （见 {@code assets/elemental_verse/models/item/element_vial.json}）。
 * 组件值 = {@link ElementType#modelIndex()}，0／缺失 = 空瓶。
 * 这条链路不需要任何客户端代码。
 */
public class ElementVialItem extends Item {

    public ElementVialItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------ 组件读写

    /** 瓶子里装的是什么；没有组件就是空瓶。 */
    public static ElementType getElement(ItemStack stack) {
        ElementType type = stack.get(EVDataComponents.ELEMENT.get());
        return type == null ? ElementType.EMPTY : type;
    }

    public static int getTimer(ItemStack stack) {
        Integer timer = stack.get(EVDataComponents.DECAY_TIMER.get());
        return timer == null ? 0 : timer;
    }

    /**
     * 只更新计时器。
     *
     * <p>刻意**不动** {@code custom_model_data}：每秒一次的计时推进如果连带改模型值，
     * 会让客户端把物品当成"变了"，白白刷新一次同步与渲染。
     */
    public static void setTimer(ItemStack stack, int timer) {
        stack.set(EVDataComponents.DECAY_TIMER.get(), timer);
    }

    /** 这个堆叠是否应该画衰变条：空瓶、金、芽这类不衰变的不画。 */
    public static boolean hasDecayBar(ItemStack stack) {
        ElementType type = getElement(stack);
        return DecayEngine.decays(type) && EVConfig.getThreshold(type) > 0;
    }

    /** 衰变进度 0..100（给客户端的耐久条用）。 */
    public static int getProgress(ItemStack stack) {
        Integer progress = stack.get(EVDataComponents.DECAY_PROGRESS.get());
        return progress == null ? 0 : progress;
    }

    /** 只在整数百分比真的变了时才写，避免每 tick 都标记物品变更。 */
    public static void setProgress(ItemStack stack, int progress) {
        if (getProgress(stack) == progress) {
            return;
        }
        if (progress <= 0) {
            stack.remove(EVDataComponents.DECAY_PROGRESS.get());
        } else {
            stack.set(EVDataComponents.DECAY_PROGRESS.get(), Math.min(100, progress));
        }
    }

    /** 按当前计时器与阈值算出百分比；换形态后阈值会变，必须重算。 */
    public static void refreshProgress(ItemStack stack) {
        ElementType type = getElement(stack);
        if (type.isEmpty()) {
            setProgress(stack, 0);
            return;
        }
        int threshold = EVConfig.getThreshold(type);
        if (threshold <= 0) {
            setProgress(stack, 0);
            return;
        }
        int timer = getTimer(stack);
        setProgress(stack, (int) Math.min(100L, timer * 100L / threshold));
    }

    /** 换形态：元素、计时器、模型序号一起写。只在真正变形时调用。 */
    public static void setElement(ItemStack stack, ElementType type, int timer) {
        if (type.isEmpty()) {
            stack.remove(EVDataComponents.ELEMENT.get());
            stack.remove(EVDataComponents.DECAY_TIMER.get());
            stack.remove(EVDataComponents.DECAY_PROGRESS.get());
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        } else {
            stack.set(EVDataComponents.ELEMENT.get(), type);
            stack.set(EVDataComponents.DECAY_TIMER.get(), timer);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.modelIndex()));
            refreshProgress(stack);
        }
    }

    /** 造一个指定形态的容器（创造物品栏与衰变换形态都用它）。载体由元素决定。 */
    public static ItemStack createStack(ElementType type) {
        ItemStack stack = new ItemStack(type.carrier().item());
        setElement(stack, type, 0);
        return stack;
    }

    /** 空瓶的默认实例。 */
    public static ItemStack emptyVial() {
        return new ItemStack(EVItems.ELEMENT_VIAL.get());
    }

    // ------------------------------------------------------------ 显示

    /**
     * 名字。
     *
     * <p>空的时候用**物品自己的名字**（元素瓶 / 折射棱镜 / 聚影瓶），
     * 装了东西之后：
     * <ul>
     *   <li>元素瓶 → 「水元素瓶」（专门的键）</li>
     *   <li>棱镜 / 聚影瓶 → 直接叫元素名（「光元素」），容器由外形区分</li>
     * </ul>
     */
    @Override
    public Component getName(ItemStack stack) {
        ElementType type = getElement(stack);
        if (type.isEmpty()) {
            return Component.translatable(getDescriptionId());
        }
        if (this == EVItems.ELEMENT_VIAL.get()) {
            return Component.translatable(EVLang.vial(type)).withStyle(type.color());
        }
        return type.displayName();
    }

    /** 逐轴列出这条元素贡献的值（作者：要能看到"详细加的元素值"）。 */
    public static void appendValueLines(ElementType type, List<Component> tooltip) {
        if (type == null || type.isEmpty()) {
            return;
        }
        ElementValue value = type.value();
        for (ElementAxis axis : ElementAxis.values()) {
            double amount = value.get(axis);
            if (Math.abs(amount) < 1.0E-3D) {
                continue;
            }
            String text = Component.translatable(
                    "axis.elemental_verse." + axis.getSerializedName()).getString()
                    + " " + (amount > 0 ? "+" : "-")
                    + String.format(java.util.Locale.ROOT, "%.1f", Math.abs(amount));
            tooltip.add(Component.literal(text)
                    .withStyle(amount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }

    /**
     * 悬停：阶段描述 + 六轴数值。
     *
     * <p>用**同步到客户端**的 {@code decay_progress} 判断阶段，而不是 {@code decay_timer}——
     * 后者刻意不同步（同步会导致手持闪动），所以这里不能也不必读它。
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        ElementType type = getElement(stack);
        if (type.isEmpty()) {
            tooltip.add(Component.translatable(EVLang.TOOLTIP_EMPTY_VIAL)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        // 这条元素到底加了多少 —— 六轴逐条列出，正值绿、负值红（不衰变的也显示）
        appendValueLines(type, tooltip);

        if (!type.decays()) {
            tooltip.add(Component.translatable(EVLang.TOOLTIP_NO_DECAY)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        int progress = getProgress(stack);
        DecayStage stage = progress >= 80 ? DecayStage.DEAD
                : progress >= 50 ? DecayStage.DERIVED
                : DecayStage.STABLE;
        tooltip.add(Component.translatable(stage.descriptionKey()).withStyle(ChatFormatting.GRAY));
    }

    /**
     * 关键：**别播换手动效。**
     *
     * <p>只要堆叠的数据组件变了，客户端就认为"这个物品变了"，会重播手持/拾取动画 ——
     * 玩家看到的就是元素瓶在手上闪。形态变化（只换组件）应该完全静默。
     *
     * <p>真正的物品类型变化（→ 死寂物质）仍会正常播动效。
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (!slotChanged && !oldStack.isEmpty() && !newStack.isEmpty()
                && oldStack.is(this) && newStack.is(this)) {
            return false;
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    // ------------------------------------------------------------ 采集
    // 全部规则在 element.gathering.Gathering 里，这里只做转发 ——
    // 七种元素 + 冰 + 光暗 的判定都集中在那个类，避免两处实现分叉。

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return Gathering.use(level, player, hand);
    }
}
