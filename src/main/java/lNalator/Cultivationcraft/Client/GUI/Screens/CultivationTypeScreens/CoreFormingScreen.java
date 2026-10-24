package lNalator.Cultivationcraft.Client.GUI.Screens.CultivationTypeScreens;

import lNalator.Cultivationcraft.Client.ClientListeners;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

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

    public void render(PoseStack PoseStack, float partialTicks, int xPos, int yPos, Screen screen, CultivationType cultivation, Font font) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        CultivationType cult = cultivation;
        int coreNumber = 0;
        double maxCore = cult.getStage();

        // Stop any division by zero errors
        if (maxCore > 1) {
            maxCore -= 1;
        }

        while (cult instanceof CoreFormingCultivation) {
            renderCore(PoseStack, partialTicks, xPos + midX - (xSize / 2), yPos + midY, screen, ((CoreFormingCultivation) cult).getFocus(), coreNumber / maxCore);

            coreNumber++;
            cult = cult.getPreviousCultivation();
        }
    }

    public void renderCore(PoseStack PoseStack, float partialTicks, float xPos, float yPos, Screen screen, TechniqueModifier focus, double rotationPos) {
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

        focus.getCoreTexture().render(PoseStack, xPos, yPos, xSize, ySize, screen.getBlitOffset());
    }
}
