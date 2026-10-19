package lNalator.Cultivationcraft.Client.GUI.Screens;

import lNalator.Cultivationcraft.Client.GUI.ScreenTabControl;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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
    public void render(PoseStack PoseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(PoseStack);
        drawGuiBackgroundLayer(PoseStack, partialTicks, mouseX, mouseY);
        super.render(PoseStack, mouseX, mouseY, partialTicks);
    }

    protected void drawGuiBackgroundLayer(PoseStack PoseStack, float partialTicks, int mouseX, int mouseY) {
        // Draw the generic tab screen
        RenderSystem.setShaderTexture(0, tabTexture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        drawPanel(PoseStack, this, tab, mouseX, mouseY, edgeSpacingX - (PANEL_WIDTH - xSize) / 2, edgeSpacingY - (PANEL_HEIGHT - ySize) / 2);

        // Draw anything specific to this tab
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        this.blit(PoseStack, edgeSpacingX, edgeSpacingY, 0, 0, this.xSize, this.ySize);
    }

    // Draw the tabs and the panel at (x, y) with the tab texture bound
    // The panel texture is 256x178: stretch it to PANEL_WIDTH x PANEL_HEIGHT from its four corners (the middle is plain)
    public static void drawPanel(PoseStack PoseStack, Screen screen, int tab, int mouseX, int mouseY, int x, int y) {
        ScreenTabControl.drawTabs(PoseStack, x, y, screen);

        int halfW = PANEL_WIDTH / 2, halfH = PANEL_HEIGHT / 2;
        int restW = PANEL_WIDTH - halfW, restH = PANEL_HEIGHT - halfH;
        screen.blit(PoseStack, x, y, 0, 0, halfW, halfH);
        screen.blit(PoseStack, x + halfW, y, 256 - restW, 0, restW, halfH);
        screen.blit(PoseStack, x, y + halfH, 0, 178 - restH, halfW, restH);
        screen.blit(PoseStack, x + halfW, y + halfH, 256 - restW, 178 - restH, restW, restH);

        ScreenTabControl.highlightTabs(PoseStack, tab, mouseX, mouseY, x, y, screen);
        ScreenTabControl.tabText(PoseStack, x, y, Minecraft.getInstance().font);
    }
}
