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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 把"哪只手"传给客户端，界面那侧才能找回同一个堆叠
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new PouchMenu(id, inventory, stack),
                            Component.translatable(EVLang.ITEM_POUCH)),
                    buffer -> buffer.writeEnum(hand));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** 悬停时显示"装有几件东西"，方便不看界面也知道里面有货。 */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        int count = 0;
        double cooling = 1.0D;
        for (ItemStack inside : PouchContents.get(stack)) {
            if (!inside.isEmpty()) {
                count++;
            }
        }
        cooling = PouchContents.coolingMultiplier(PouchContents.get(stack));

        tooltip.add(Component.translatable(EVLang.TOOLTIP_POUCH_COUNT, count)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        if (cooling < 1.0D) {
            tooltip.add(Component.translatable(EVLang.TOOLTIP_POUCH_COOLING,
                            String.format(java.util.Locale.ROOT, "×%.2f", cooling))
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        }
    }
}
