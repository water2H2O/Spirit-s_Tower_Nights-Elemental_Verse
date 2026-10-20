package com.water2h2o.elementalverse.element;

import java.util.List;

import com.water2h2o.elementalverse.registry.EVItems;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 创造小包：只在创造模式拿得到，**施法不用自己存元素**。
 *
 * <p>放在背包任意位置即可生效；不用打开、不用装东西。
 * 定位和创世神给的那堆调试工具一样 —— 用来试法术手感，不该出现在生存流程里。
 */
public class PouchItemCreative extends Item {

    public PouchItemCreative(Properties properties) {
        super(properties);
    }

    /** 背包里（含快捷栏、副手、装备）有没有这个小包。 */
    public static boolean carriedBy(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(EVItems.CREATIVE_POUCH.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(EVLang.CREATIVE_POUCH_HINT)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(EVLang.CREATIVE_POUCH_ONLY)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
