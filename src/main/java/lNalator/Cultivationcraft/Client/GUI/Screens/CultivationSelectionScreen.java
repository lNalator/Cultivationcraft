package lNalator.Cultivationcraft.Client.GUI.Screens;

import lNalator.Cultivationcraft.Client.GUI.GUIButton;
import lNalator.Cultivationcraft.Client.GUI.TextField;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.ClientPacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CultivationSelectionScreen extends Screen {

    protected int type = CultivationTypes.QI_CONDENSER;

    protected ResourceLocation TEXTURE;

    public final int xSize = 256;
    public final int ySize = 178;

    protected final int cultivationTextX = 20;
    protected final int cultivationTextY = 50;
    protected final int cultivationTextWidth = 215;
    protected final int cultivationTextHeight = 100;

    protected TextField cultivationText = new TextField();

    protected GUIButton cultivate;

    protected int cultivateXPos = 127;
    protected int cultivateYPos = 155;

    protected int selectionSpacing = 20;

    protected int selectionXPos = 127;
    protected int selectionYPos = 25;

    protected GUIButton internal;
    protected GUIButton external;

    public CultivationSelectionScreen() {
        super(Component.translatable("cultivationcraft.gui.cultivationselection"));

        TEXTURE = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/blank.png");

        String cultivateString = Component.translatable("cultivationcraft.gui.cultivate").getString();
        cultivate = new GUIButton("CULTIVATE", cultivateString);

        String internalString = Component.translatable("cultivationcraft.gui.internal").getString();
        internal = new GUIButton("INTERNAL", internalString);

        String externalString = Component.translatable("cultivationcraft.gui.external").getString();
        external = new GUIButton("EXTERNAL", externalString);
        external.select();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonPressed) {
        if (super.mouseClicked(mouseX, mouseY, buttonPressed)) {
            return true;
        }

        if (internal.mouseClick((int) mouseX, (int) mouseY, buttonPressed)) {
            external.unselect();
            type = CultivationTypes.BODY_CULTIVATOR;

            return true;
        } else if (external.mouseClick((int) mouseX, (int) mouseY, buttonPressed)) {
            internal.unselect();
            type = CultivationTypes.QI_CONDENSER;

            return true;
        }

        // Choose selected cultivation method if button is pressed
        if (cultivate.mouseClick((int) mouseX, (int) mouseY, buttonPressed)) {
            ClientPacketHandler.sendCultivationTypeToServer(Minecraft.getInstance().player.getUUID(), type);
            this.onClose();
            return true;
        }

        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        drawGuiBackgroundLayer(guiGraphics, partialTicks, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        drawGuiForgroundLayer(guiGraphics, partialTicks, mouseX, mouseY);
    }

    protected void drawGuiBackgroundLayer(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, edgeSpacingX, edgeSpacingY, 0, 0, this.xSize, this.ySize);
    }

    protected void drawGuiForgroundLayer(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        internal.setPos(edgeSpacingX + selectionXPos - internal.width - selectionSpacing / 2, edgeSpacingY + selectionYPos);
        internal.render(guiGraphics, mouseX, mouseY);
        external.setPos(edgeSpacingX + selectionXPos + selectionSpacing / 2, edgeSpacingY + selectionYPos);
        external.render(guiGraphics, mouseX, mouseY);

        cultivationText.setPos(edgeSpacingX + cultivationTextX, edgeSpacingY + cultivationTextY);
        cultivationText.setSize(cultivationTextWidth, cultivationTextHeight);
        cultivationText.setText(Component.translatable("cultivationcraft.gui.cultivationtype." + type).getString());
        cultivationText.render(guiGraphics, font, mouseX, mouseY);

        cultivate.setPos(edgeSpacingX + cultivateXPos - cultivate.width / 2, edgeSpacingY + cultivateYPos);
        cultivate.render(guiGraphics, mouseX, mouseY);
    }
}
