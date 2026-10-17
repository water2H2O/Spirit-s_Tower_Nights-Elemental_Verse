package com.water2h2o.elementalverse.client;

import com.water2h2o.elementalverse.element.ElementCarrier;
import com.water2h2o.elementalverse.element.ElementVialItem;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * 在元素容器下面画一条**长得像耐久条**的横条，用来显示"还剩多久变质"。
 *
 * <h2>为什么不用原版耐久（DAMAGE 组件）</h2>
 * <ul>
 *   <li>带损伤的物品**不能堆叠** —— 而我们刚把元素瓶做成 64 一叠</li>
 *   <li>{@code DAMAGE} 每秒变化会让服务器不停推送整格物品，容易再次出现闪动</li>
 * </ul>
 * 所以这里只借用"外观"：用 NeoForge 的物品装饰钩子自己画 13×2 的条，
 * 数据来自一个**每秒最多变一次的小整数**（{@code decay_progress}）。
 *
 * <p>条的外观刻意和原版耐久条一致（同样是 x+2, y+13 起的 13 像素），
 * 玩家一眼就懂，不用学新东西。
 */
public class DecayBarDecorator implements IItemDecorator {

    /** 与耐久条一致的内外框位置。 */
    private static final int BAR_LEFT = 2;
    private static final int BAR_TOP = 13;
    private static final int BAR_RIGHT = 15;
    private static final int BAR_BOTTOM = 15;

    private static final int COLOR_BACKGROUND = 0xFF000000;
    private static final int COLOR_HEALTHY = 0xFF3CE03C;
    private static final int COLOR_WARNING = 0xFFE8C22A;
    private static final int COLOR_CRITICAL = 0xFFE03C3C;

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        if (!ElementCarrier.isCarrier(stack)) {
            return false;
        }

        // 0 = 刚装好，100 = 马上就要变了
        int progress = ElementVialItem.getProgress(stack);
        if (progress <= 0) {
            // 空瓶、不衰变的元素（金、芽待定）都不画
            return false;
        }
        if (progress > 100) {
            progress = 100;
        }

        int width = BAR_RIGHT - BAR_LEFT;
        int filled = Math.max(1, Math.round(width * (100 - progress) / 100.0F));

        int color = progress < 50 ? COLOR_HEALTHY
                : progress < 80 ? COLOR_WARNING
                : COLOR_CRITICAL;

        // 描黑底 + 彩色填充，和原版耐久条同样的像素位置
        graphics.fill(x + BAR_LEFT, y + BAR_TOP, x + BAR_RIGHT, y + BAR_BOTTOM, COLOR_BACKGROUND);
        graphics.fill(x + BAR_LEFT, y + BAR_TOP + 1, x + BAR_LEFT + filled, y + BAR_BOTTOM, color);

        return true;
    }
}
