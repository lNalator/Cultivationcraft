package lNalator.Cultivationcraft.Client.GUI;

import lNalator.Cultivationcraft.Client.genericClientFunctions;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class TechniqueIcons {

    protected static final ResourceLocation HIGHLIGHT = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/highlight.png");
    protected static final ResourceLocation COOLDOWN = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/cooldown.png");

    // Loop through all player icons and draw them from the supplied coordinates
    // Remember to rebind texture aftwards
    public static void renderIcons(GuiGraphics guiGraphics, int xpos, int ypos, int spacing) {
        if (genericClientFunctions.getPlayer() == null) {
            return;
        }

        ICultivatorTechniques techs = CultivatorTechniques.getCultivatorTechniques(genericClientFunctions.getPlayer());

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            if (techs.getTechnique(i) != null) {
                guiGraphics.blit(techs.getTechnique(i).getIcon(), xpos + i * spacing, ypos, 0, 0, 16, 16, 16, 16);
            }
        }
    }

    // Highlight all techniques that are currently active
    public static void highlightActive(GuiGraphics guiGraphics, int xpos, int ypos, int spacing) {
        ICultivatorTechniques techs = CultivatorTechniques.getCultivatorTechniques(genericClientFunctions.getPlayer());

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            if (techs.getTechnique(i) != null && techs.getTechnique(i).isActive()) {
                highlightIcon(guiGraphics, xpos, ypos, spacing, i);
            }
        }
    }

    // Highlight all techniques that are currently active
    public static void showCooldowns(GuiGraphics guiGraphics, int xpos, int ypos, int spacing) {
        ICultivatorTechniques techs = CultivatorTechniques.getCultivatorTechniques(genericClientFunctions.getPlayer());

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            if (techs.getTechnique(i) != null && techs.getTechnique(i).getCooldown() > 0) {
                cooldownIcon(guiGraphics, xpos, ypos, spacing, i);
            }
        }
    }

    // Returns the technique number the mouse is over
    // Returns -1 if mouse isn't over a technique
    public static int mouseOver(int xpos, int ypos, int mouseX, int mouseY, int spacing) {
        if (mouseY < ypos || mouseY > ypos + 16) {
            return -1;
        }

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            if (mouseX >= xpos + i * spacing && mouseX <= xpos + i * spacing + 16) {
                return i;
            }
        }

        return -1;
    }

    public static void mouseOverHighlight(GuiGraphics guiGraphics, int xpos, int ypos, int spacing, int mouseX, int mouseY) {
        int mouseOver = mouseOver(xpos, ypos, mouseX, mouseY, spacing);

        if (mouseOver == -1) {
            return;
        }

        highlightIcon(guiGraphics, xpos, ypos, spacing, mouseOver);
    }

    // Highlight the selected icon
    // Remember to rebind texture afterwards
    public static void highlightIcon(GuiGraphics guiGraphics, int xpos, int ypos, int spacing, int icon) {
        RenderSystem.enableBlend();

        // Set the texture to be semi-transparent
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

        guiGraphics.blit(HIGHLIGHT, xpos + icon * spacing, ypos, 0, 0, 16, 16, 16, 16);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1F);
    }

    // Darken the selected icon
    // Remember to rebind texture afterwards
    public static void cooldownIcon(GuiGraphics guiGraphics, int xpos, int ypos, int spacing, int icon) {
        RenderSystem.enableBlend();

        // Set the texture to be semi-transparent
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

        guiGraphics.blit(COOLDOWN, xpos + icon * spacing, ypos, 0, 0, 16, 16, 16, 16);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1F);

        // TODO: Display how long left on cooldown?
    }
}
