package com.water2h2o.elementalverse.client;

import com.water2h2o.elementalverse.menu.RitualAltarMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 仪式台界面（176×184 底图）。
 *
 * <pre>
 *        [命名牌]
 *   [料] + [辅料] ( [态] )
 *   ─────────────────
 *   [申明板]  →  [产出]
 * </pre>
 */
public class RitualAltarScreen extends AbstractContainerScreen<RitualAltarMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("elemental_verse", "textures/gui/ritual_altar.png");

    /** 底图尺寸；blit 必须显式传，否则按 256×256 采样会整体错位。 */
    private static final int TEX_W = 176;
    private static final int TEX_H = 184;

    public RitualAltarScreen(RitualAltarMenu menu, Inventory playerInventory, Component title) {
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
    }
}
