package com.water2h2o.elementalverse.client;

import com.water2h2o.elementalverse.menu.RitualAltarMenu;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 仪式台界面（作者提供的 176×166 底图）。
 *
 * <p>底图原来的中间位置画的是“输入 → 输出”箭头，而代码在这里放的是剑槽；
 * 所以屏幕上会补一个原版槽位底图盖住箭头，并标“剑槽”。
 */
public class RitualAltarScreen extends AbstractContainerScreen<RitualAltarMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("elemental_verse", "textures/gui/ritual_altar.png");

    public RitualAltarScreen(RitualAltarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
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
        // 关键：必须显式传纹理尺寸 176x166。
        // 6 参数版本的 blit 默认按 256x256 采样，会把这张图整体放大、
        // 于是作者画的槽位和代码里的槽位全部错开（作者在 layout 文件里专门提醒过）。
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);

        // 剑槽在申明板正下方（内容坐标 62,53）。贴图那一带是空白，
        // 补一个原版槽位底图 + 标注。中间的"输入 → 输出"箭头保持可见。
        graphics.blit(INVENTORY_LOCATION, x + 61, y + 52, 7, 83, 18, 18);
        graphics.drawString(this.font, Component.translatable(EVLang.RITUAL_TOOL_LABEL),
                x + 81, y + 57, 0xFF404B44, false);
    }
}
