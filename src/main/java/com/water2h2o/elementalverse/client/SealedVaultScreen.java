package com.water2h2o.elementalverse.client;

import com.water2h2o.elementalverse.menu.SealedVaultMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 密封仓界面。
 *
 * <p>直接复用**原版发射器**的 GUI 贴图（3×3 布局完全一致），
 * 这样不用等新素材也能有正经界面；以后换成自定义贴图只要改这一个常量。
 */
public class SealedVaultScreen extends AbstractContainerScreen<SealedVaultMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    public SealedVaultScreen(SealedVaultMenu menu, Inventory playerInventory, Component title) {
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
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
}
