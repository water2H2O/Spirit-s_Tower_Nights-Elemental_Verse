package com.water2h2o.elementalverse.element;

import java.util.List;

import com.water2h2o.elementalverse.registry.EVDataComponents;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 申明板：模组的心脏。
 *
 * <p>两种用法（见 `docs/分阶段/04-仪式台与申明板.md`）：
 * <ul>
 *   <li>**直接释放**：手持已定义的板子右键触发</li>
 *   <li>**当锻造模板**：刻到剑上，由剑触发</li>
 * </ul>
 *
 * <p>板子本身只是"载体"，写了什么由 {@link Declaration} 数据组件决定。
 */
public class DeclarationPlateItem extends Item {

    public DeclarationPlateItem(Properties properties) {
        super(properties);
    }

    public static Declaration getDeclaration(ItemStack stack) {
        Declaration declaration = stack.get(EVDataComponents.DECLARATION.get());
        return declaration == null ? Declaration.EMPTY : declaration;
    }

    public static void setDeclaration(ItemStack stack, Declaration declaration) {
        if (declaration == null || !declaration.isDefined()) {
            stack.remove(EVDataComponents.DECLARATION.get());
            // 回到"空板"图标
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        } else {
            stack.set(EVDataComponents.DECLARATION.get(), declaration);
            // 切到"已写"图标（值 1 = 写好的那张）
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                    new net.minecraft.world.item.component.CustomModelData(1));
        }
    }

    /** 造一块写好的板子。 */
    public static ItemStack written(ElementType element, int tier) {
        ItemStack stack = new ItemStack(com.water2h2o.elementalverse.registry.EVItems.DECLARATION_PLATE.get());
        setDeclaration(stack, new Declaration(element, tier, "release", "projectile"));
        return stack;
    }

    /**
     * 直接释放：拿着写好的板子右键。
     *
     * <p>空板右键**什么都不做**（它还没写东西）；写好的板子按档位扣材料再触发反应。
     */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!getDeclaration(stack).isDefined()) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        boolean cast = com.water2h2o.elementalverse.reaction.SpellCaster.cast(level, player, stack);
        return cast
                ? net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide())
                : net.minecraft.world.InteractionResultHolder.fail(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Declaration declaration = getDeclaration(stack);
        if (!declaration.isDefined()) {
            return Component.translatable(getDescriptionId());
        }
        return Component.translatable(EVLang.PLATE_WRITTEN, declaration.element().displayName());
    }

    /**
     * 悬停只显示"它是什么"，不显示数值。
     *
     * <p>作者要求：**玩家看到的一定简单**。元素值、强度这些是内部口径，
     * 不放出来；只说"哪种元素、几档"。
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        Declaration declaration = getDeclaration(stack);
        if (!declaration.isDefined()) {
            tooltip.add(Component.translatable(EVLang.PLATE_BLANK_HINT)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable(EVLang.PLATE_ELEMENT, declaration.element().displayName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(EVLang.PLATE_TIER, declaration.tier())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(EVLang.PLATE_OPERATION)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
