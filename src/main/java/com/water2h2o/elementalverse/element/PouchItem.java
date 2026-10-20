package com.water2h2o.elementalverse.element;

import com.water2h2o.elementalverse.menu.PouchMenu;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 元素小包：9 格便携容器。
 *
 * <p>作者 2026-10-17 的设计意图：**把几种元素装在一起随身带，方便现场做东西**；
 * 里面放霜（×0.8）或冰（×0.5）可以消耗它们来减缓变质。
 *
 * <p>物品本身只负责"打开界面"，内容存在 {@code pouch_contents} 数据组件里
 * （见 {@link PouchContents}）。
 */
public class PouchItem extends Item {

    public PouchItem(Properties properties) {
        super(properties);
    }

    /** 小包的名字用紫红色，和元素瓶区分开（作者 2026-10-20）。 */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(EVLang.ITEM_POUCH)
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 把"哪只手"传给客户端，界面那侧才能找回同一个堆叠
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new PouchMenu(id, inventory, stack),
                            Component.translatable(EVLang.ITEM_POUCH)
                                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)),
                    buffer -> buffer.writeEnum(hand));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** 悬停显示：装了几件、冷却剂几个、当前冷却倍率。 */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        java.util.List<ItemStack> items = PouchContents.get(stack);
        int count = 0;
        for (ItemStack inside : items) {
            if (!inside.isEmpty()) {
                count++;
            }
        }

        tooltip.add(Component.translatable(EVLang.TOOLTIP_POUCH_COUNT, count)
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        int coolant = PouchContents.coolantCount(items);
        double cooling = PouchContents.coolingMultiplier(items);
        if (cooling < 1.0D) {
            tooltip.add(Component.translatable(EVLang.TOOLTIP_POUCH_COOLING,
                            String.format(java.util.Locale.ROOT, "×%.2f", cooling), coolant)
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        } else if (coolant > 0) {
            // 有冷却剂但里面没东西可保护（不会消耗）
            tooltip.add(Component.translatable(EVLang.TOOLTIP_POUCH_IDLE, coolant)
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        }
    }
}
