package lNalator.Cultivationcraft.Client.GUI.Screens.CultivationTypeScreens;

import lNalator.Cultivationcraft.Client.ClientListeners;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

public class CoreFormingScreen extends CultivationTypeScreen {

    final int midX = 38;
    final int midY = 56;

    final float coreRotationDiameter = 12;
    final float coreRotationSpeed = 0.25f;

    public CoreFormingScreen() {
        xSize = 10;
        ySize = 10;

        addConceptButton();
    }

    public void render(GuiGraphics guiGraphics, float partialTicks, int xPos, int yPos, CultivationType cultivation, Font font) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        CultivationType cult = cultivation;
        int coreNumber = 0;
        double maxCore = cult.getStage();

        // Stop any division by zero errors
        if (maxCore > 1) {
            maxCore -= 1;
        }

        while (cult instanceof CoreFormingCultivation) {
            renderCore(guiGraphics, partialTicks, xPos + midX - (xSize / 2), yPos + midY, ((CoreFormingCultivation) cult).getFocus(), coreNumber / maxCore);

            coreNumber++;
            cult = cult.getPreviousCultivation();
        }
    }

    public void renderCore(GuiGraphics guiGraphics, float partialTicks, float xPos, float yPos, TechniqueModifier focus, double rotationPos) {
        // Rotate the core if it is not the first one
        if (rotationPos > 0) {
            // Determine the starting angle of the core
            rotationPos *= 360;
            // Move the angle based on the current tick
            rotationPos = (rotationPos + (((ClientListeners.tick + partialTicks) * coreRotationSpeed) % 360.0)) % 360.0;

            // Convert angle to radians
            rotationPos = Math.toRadians(rotationPos);

            xPos += Math.cos(rotationPos) * coreRotationDiameter;
            yPos += Math.sin(rotationPos) * coreRotationDiameter;
        }

        focus.getCoreTexture().render(guiGraphics.pose(), xPos, yPos, xSize, ySize, 0);
    }
}
