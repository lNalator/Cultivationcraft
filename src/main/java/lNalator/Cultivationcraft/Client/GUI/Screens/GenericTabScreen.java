package lNalator.Cultivationcraft.Client.GUI.Screens;

import lNalator.Cultivationcraft.Client.GUI.ScreenTabControl;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class GenericTabScreen extends Screen {

    protected ResourceLocation TEXTURE;

    // Size of each tab's content layout, centered in the bigger panel
    public final int xSize = 256;
    public final int ySize = 178;

    // Size of the panel shared by every cultivation menu page (RefinementScreen included)
    public static final int PANEL_WIDTH = 296;
    public static final int PANEL_HEIGHT = 208;

    protected int tab;
    public static final ResourceLocation tabTexture = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/tabscreen.png");

    GenericTabScreen(int activeTab, Component name, ResourceLocation tabLocation) {
        super(name);

        tab = activeTab;
        TEXTURE = tabLocation;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonPressed) {
        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        if (ScreenTabControl.mouseClick((int) mouseX, (int) mouseY, edgeSpacingX - (PANEL_WIDTH - xSize) / 2, edgeSpacingY - (PANEL_HEIGHT - ySize) / 2, buttonPressed)) {
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, buttonPressed)) {
            return true;
        }

        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        drawGuiBackgroundLayer(guiGraphics, partialTicks, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    protected void drawGuiBackgroundLayer(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // Draw the generic tab screen
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        drawPanel(guiGraphics, tab, mouseX, mouseY, edgeSpacingX - (PANEL_WIDTH - xSize) / 2, edgeSpacingY - (PANEL_HEIGHT - ySize) / 2);

        // Draw anything specific to this tab
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.xSize, this.ySize);
    }

    // Draw the tabs and the panel at (x, y) with the tab texture
    // The panel texture is 256x178: stretch it to PANEL_WIDTH x PANEL_HEIGHT from its four corners (the middle is plain)
    public static void drawPanel(GuiGraphics guiGraphics, int tab, int mouseX, int mouseY, int x, int y) {
        ScreenTabControl.drawTabs(guiGraphics, x, y);

        int halfW = PANEL_WIDTH / 2, halfH = PANEL_HEIGHT / 2;
        int restW = PANEL_WIDTH - halfW, restH = PANEL_HEIGHT - halfH;
        guiGraphics.blit(tabTexture, x, y, 0, 0, halfW, halfH);
        guiGraphics.blit(tabTexture, x + halfW, y, 256 - restW, 0, restW, halfH);
        guiGraphics.blit(tabTexture, x, y + halfH, 0, 178 - restH, halfW, restH);
        guiGraphics.blit(tabTexture, x + halfW, y + halfH, 256 - restW, 178 - restH, restW, restH);

        ScreenTabControl.highlightTabs(guiGraphics, tab, mouseX, mouseY, x, y);
        ScreenTabControl.tabText(guiGraphics, x, y, Minecraft.getInstance().font);
    }
}
