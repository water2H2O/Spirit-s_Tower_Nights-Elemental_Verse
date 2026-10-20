package com.water2h2o.elementalverse.client;

import com.water2h2o.elementalverse.menu.RitualAltarMenu;
import com.water2h2o.elementalverse.network.EVNetwork;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 仪式台界面（176×184 底图）。
 *
 * <pre>
 *   [ 命名（铁砧式输入框） ]
 *   [料] + [辅料] ( [态] )
 *   ─────────────────────
 *   [申明板]  →  [产出]
 * </pre>
 */
public class RitualAltarScreen extends AbstractContainerScreen<RitualAltarMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("elemental_verse", "textures/gui/ritual_altar.png");

    private static final int TEX_W = 176;
    private static final int TEX_H = 184;

    private EditBox nameField;

    public RitualAltarScreen(RitualAltarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = TEX_H;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        nameField = new EditBox(this.font, x + 14, y + 21, 146, 10, Component.literal("name"));
        nameField.setBordered(false);
        nameField.setMaxLength(32);
        nameField.setHint(Component.literal("命名（可选）").withStyle(ChatFormatting.DARK_GRAY));
        nameField.setValue(this.menu.customName());
        // 边打边同步：命名只在"取出产物"那一刻用得上，不需要提交按钮
        nameField.setResponder(text -> PacketDistributor.sendToServer(new EVNetwork.SetAltarName(text)));
        this.addRenderableWidget(nameField);
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
