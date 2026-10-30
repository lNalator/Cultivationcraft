package lNalator.Cultivationcraft.Client.GUI.Screens.CultivationTypeScreens;

import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;

public class FoundationEstablishmentScreen extends CultivationTypeScreen {

    public FoundationEstablishmentScreen() {
        TEXTURE = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/foundationestablishment.png");

        xSize = 76;
        ySize = 115;
    }

    public void render(GuiGraphics guiGraphics, float partialTicks, int xPos, int yPos, CultivationType cultivation, Font font) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float maxProgress = cultivation.getMaxStage() * cultivation.getMaxTechLevelWithoutPrevious();
        float currentProgress = cultivation.getTechLevelProgress(cultivation.getPassive());
        float height = currentProgress / maxProgress;

        guiGraphics.blit(TEXTURE, xPos, yPos + (int) (ySize * (1 - height)), 0, (int) (ySize * (1 - height)), xSize, (int) (ySize * height), xSize, ySize);
    }
}
