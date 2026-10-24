package lNalator.Cultivationcraft.Client.GUI.Screens.CultivationTypeScreens;

import lNalator.Cultivationcraft.Client.GUI.GUIButton;
import lNalator.Cultivationcraft.Client.GUI.Screens.ConceptScreen;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CultivationTypeScreen {

    ResourceLocation TEXTURE = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/stats.png");

    int xSize = 1;
    int ySize = 1;

    // Opens the concepts unlocked and condensed into cores, for the realms that learn concepts
    protected GUIButton conceptButton = null;

    final int conceptButtonX = 60;
    final int conceptButtonY = 140;

    protected void addConceptButton() {
        conceptButton = new GUIButton("CONCEPT", Component.translatable("cultivationcraft.gui.concept").getString());
    }

    public void renderButtons(PoseStack poseStack, int xPos, int yPos, int mouseX, int mouseY, GuiComponent gui) {
        if (conceptButton != null) {
            conceptButton.setPos(xPos + conceptButtonX, yPos + conceptButtonY);
            conceptButton.render(poseStack, mouseX, mouseY, gui);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int buttonPressed) {
        if (conceptButton != null && conceptButton.mouseClick((int) mouseX, (int) mouseY, buttonPressed)) {
            conceptButton.unselect();
            Minecraft.getInstance().forceSetScreen(new ConceptScreen());
            return true;
        }

        return false;
    }

    public void render(PoseStack PoseStack, float partialTicks, int xPos, int yPos, Screen screen, CultivationType cultivation, Font font) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        GuiComponent.blit(PoseStack, xPos, yPos, screen.getBlitOffset(), 0, 0, xSize, ySize, xSize, ySize);
    }
}
