package com.water2h2o.elementalverse.element;

import java.util.List;

import com.water2h2o.elementalverse.config.EVConfig;
import com.water2h2o.elementalverse.registry.EVDataComponents;
import com.water2h2o.elementalverse.registry.EVItems;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

    public static void setElement(ItemStack stack, ElementType type, int timer) {
        if (type.isEmpty()) {
            stack.remove(EVDataComponents.ELEMENT.get());
            stack.remove(EVDataComponents.DECAY_TIMER.get());
        } else {
            stack.set(EVDataComponents.ELEMENT.get(), type);
            stack.set(EVDataComponents.DECAY_TIMER.get(), timer);
        }
    }

    /** 造一个指定形态的瓶子（创造物品栏与衰变换形态都用它）。 */
    public static ItemStack createStack(ElementType type) {
        ItemStack stack = new ItemStack(EVItems.ELEMENT_VIAL.get());
        setElement(stack, type, 0);
        return stack;
    }

    /** 空瓶的默认实例。 */
    public static ItemStack emptyVial() {
        return new ItemStack(EVItems.ELEMENT_VIAL.get());
    }

    // ------------------------------------------------------------ 显示

    @Override
    public Component getName(ItemStack stack) {
        ElementType type = getElement(stack);
        if (type.isEmpty()) {
            return Component.translatable(EVLang.ITEM_VIAL);
        }
        return Component.translatable(EVLang.vial(type));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        ElementType type = getElement(stack);
        if (type.isEmpty()) {
            tooltip.add(Component.translatable(EVLang.TOOLTIP_EMPTY_VIAL).withStyle(ChatFormatting.GRAY));
            return;
        }

        if (!EVConfig.isDecayEnabled()) {
            tooltip.add(Component.translatable(EVLang.TOOLTIP_DEBUG_DISABLED).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        if (!DecayEngine.decays(type)) {
            tooltip.add(Component.translatable(EVLang.TOOLTIP_NO_DECAY).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        // 刻意只给一句模糊描述，不给数字 —— 玩家该能判断「该处理了」，但不该读得出秒表
        DecayStage stage = DecayEngine.getStage(stack);
        tooltip.add(Component.translatable(stage.descriptionKey()).withStyle(ChatFormatting.DARK_GRAY));

        if (EVConfig.showsExactSeconds()) {
            tooltip.add(Component.translatable(EVLang.TOOLTIP_EXACT_SECONDS,
                    DecayEngine.getRemainingSeconds(stack)).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    // ------------------------------------------------------------ 采集
    // 阶段 01 只做一条最小链路：空瓶 → 水（或霜）。其余元素走创造物品栏。
    // 完整的采集系统（风要在高处、光暗要专用容器……）留给后面的阶段。

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!getElement(stack).isEmpty()) {
            // 已经装着东西了，不要覆盖掉玩家手里的元素
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide()) {
            return InteractionResultHolder.succeed(stack);
        }

        // 冰 → 霜（低温支线）；其余水源 → 水
        BlockState state = level.getBlockState(pos);
        ElementType collected = state.is(BlockTags.ICE) ? ElementType.FROST : ElementType.WATER;
        return InteractionResultHolder.succeed(fill(stack, player, collected));
    }

    /** 把瓶子换成装着指定元素的版本；一摞就拆一个出来，剩下的留在手上。 */
    public static ItemStack fill(ItemStack emptyVial, Player player, ElementType type) {
        ItemStack filled = createStack(type);
        emptyVial.shrink(1);
        if (emptyVial.isEmpty()) {
            return filled;
        }
        if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        return emptyVial;
    }
}
