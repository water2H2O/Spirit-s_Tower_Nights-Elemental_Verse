package com.water2h2o.elementalverse.client;

import com.water2h2o.elementalverse.menu.RuneCabinetMenu;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 符文柜界面。
 *
 * <p>作者反馈"符文柜的 UI 有问题"：原来直接套了原版箱子贴图，
 * 于是它看起来就是一个普通箱子 —— 看不出通没通电、还能撑多久。
 *
 * <p>现在换成专用底图（176×180，配色跟仪式台一致），并在槽位下方加一条电量条：
 * 通电时按剩余比例填充并显示剩余时长，断电时整条变暗并写"未通电"。
 */
public class RuneCabinetScreen extends AbstractContainerScreen<RuneCabinetMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("elemental_verse", "textures/gui/rune_cabinet.png");

    /** 底图尺寸；blit 必须显式传，否则会按 256×256 采样而整体错位。 */
    private static final int TEX_W = 176;
    private static final int TEX_H = 180;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 74;
    private static final int BAR_W = 88;
    private static final int BAR_H = 8;

    public RuneCabinetScreen(RuneCabinetMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = TEX_H;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        int seconds = this.menu.energySeconds();

        // 冷却条：一格 = 1 游戏日（1200 秒），共 8 格。
        // 用"格"而不是比例条 —— 0.5 日的余量在比例条上只有 6%，看着像坏了。
        double days = seconds / 1200.0D;
        for (int i = 0; i < 8; i++) {
            int sx = x + BAR_X + i * 11;
            graphics.fill(sx, y + BAR_Y, sx + 10, y + BAR_Y + BAR_H, 0xFF2B2B2B);
            double frac = Math.max(0.0D, Math.min(1.0D, days - i));
            if (frac > 0.0D) {
                int width = Math.max(1, (int) Math.round(10 * frac));
                graphics.fill(sx, y + BAR_Y, sx + width, y + BAR_Y + BAR_H, 0xFF3FA9F5);
            }
        }

        // 右侧文字：剩余时长 / 未通电
        Component label = seconds <= 0
                ? Component.translatable(EVLang.RUNE_CABINET_UNPOWERED)
                : Component.translatable(EVLang.RUNE_CABINET_REMAINING,
                        seconds / 60, seconds % 60);
        graphics.drawString(this.font, label, x + BAR_X + BAR_W + 6, y + BAR_Y + 1,
                0xFF404B44, false);
    }
}
