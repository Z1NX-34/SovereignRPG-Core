package com.sovereignrpg.core.forge;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.player.Inventory;

public class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private static final ResourceLocation[] TEX = {
        new ResourceLocation("sovereignrpgcore", "textures/gui/forge_charm.png"),
        new ResourceLocation("sovereignrpgcore", "textures/gui/forge_discharm.png")
    };
    private static final int[] TAB_X = {4, 34};
    private static final Component[] TAB_TIP = {Component.literal("Charm (Ascension)"), Component.literal("Discharm (Desecration)")};

    public ForgeScreen(ForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(TEX[menu.tab() == 0 ? 0 : 1], leftPos, topPos, 0, 0, 176, 192, 176, 192);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        String t = menu.tab() == 0 ? "Charm" : "Discharm";
        g.drawString(font, Component.literal("Ruined Forge - " + t), 12, 31, 0xE8C25C, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        for (int i = 0; i < 2; i++) {
            if (inTab(i, mx, my)) g.renderTooltip(font, TAB_TIP[i], mx, my);
        }
        renderTooltip(g, mx, my);
    }

    private boolean inTab(int i, double mx, double my) {
        int x = leftPos + TAB_X[i], y = topPos;
        return mx >= x && mx < x + 28 && my >= y && my < y + 27;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < 2; i++) {
            if (inTab(i, mx, my)) {
                if (menu.tab() != i && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
