package DaoOfModding.Cultivationcraft.Client.GUI;

import DaoOfModding.Cultivationcraft.Client.Animations.BodyPartGUIs;
import DaoOfModding.Cultivationcraft.Client.Animations.BodyPartNames;
import DaoOfModding.Cultivationcraft.Common.Capabilities.BodyModifications.BodyModifications;
import DaoOfModding.Cultivationcraft.Common.Capabilities.BodyModifications.IBodyModifications;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.BodyPart;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Map;

public class BodyforgeScreen extends Screen
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyforge.png");

    private DropdownList bodyParts;
    private DropdownList bodySubParts;

    private ArrayList<GUIButton> buttons = new ArrayList<GUIButton>();

    private final int bodyPartListXPos = 75;
    private final int bodyPartListYPos = 50;

    private final int bodySubPartListXPos = 75;
    private final int bodySubPartListYPos = 75;

    private final int buttonMinXPos = 75;
    private final int buttonMaxXPos = 170;

    private final int buttonMinYPos = 100;

    private final int xSize = 175;
    private final int ySize = 178;

    private String selectedPosition = null;

    public BodyforgeScreen()
    {
        super(new TranslationTextComponent("cultivationcraft.gui.bodyforge"));
        updateBodyPartList();
    }

    private void updateBodyPartList()
    {
        bodyParts = new DropdownList();
        bodyParts.addItem("", "");

        ClientPlayerEntity player = Minecraft.getInstance().player;

        // Create a list of all body part positions that can currently be modified
        // TODO: Or have already been modified
        ArrayList<String> positions = new ArrayList<String>();

        for (BodyPart part : BodyPartNames.getParts())
            if (!positions.contains(part.getPosition()) && part.canBeForged(player))
                positions.add(part.getPosition());

        // Add all valid positions into the DropdownList with the appropriate display name
        for (String pos : positions)
            bodyParts.addItem(BodyPartNames.getDisplayName(pos), pos);

        updateBodySubPartList();
    }

    private void updateBodySubPartList()
    {
        bodySubParts = new DropdownList();

        ClientPlayerEntity player = Minecraft.getInstance().player;
        ArrayList<String> positions = new ArrayList<String>();

        for (BodyPart part : BodyPartNames.getParts())
            if (part.getPosition() == selectedPosition && !positions.contains(part.getSubPosition()) && part.canBeForged(player))
                positions.add(part.getSubPosition());

        for (String pos : positions)
            bodySubParts.addItem(BodyPartNames.getDisplayName(selectedPosition, pos), pos);

        // If no sub-positions are found make dropdown list blank
        if (positions.size() == 0)
            bodySubParts.addItem("", "");

        updateButtons();
    }

    private void updateButtons()
    {
        buttons.clear();

        ClientPlayerEntity player = Minecraft.getInstance().player;

        for (BodyPart part : BodyPartNames.getParts())
            if (part.getPosition() == selectedPosition && part.getSubPosition() == bodySubParts.getSelected() && part.canBeForged(player))
                buttons.add(new GUIButton(part.getID(), part.getDisplayName()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonPressed)
    {
        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        if (ScreenTabControl.mouseClick((int)mouseX, (int)mouseY, edgeSpacingX, edgeSpacingY, buttonPressed))
            return true;

        // Change the selected position if a position is clicked in the dropdown list
        String changed = (String)bodyParts.mouseClick((int)mouseX - (edgeSpacingX + bodyPartListXPos), (int)mouseY - (edgeSpacingY + bodyPartListYPos), buttonPressed);

        if (changed != null)
        {
            selectedPosition = changed;
            updateBodySubPartList();
            return true;
        }

        if (bodySubParts.mouseClick((int)mouseX - (edgeSpacingX + bodySubPartListXPos), (int)mouseY - (edgeSpacingY + bodySubPartListYPos), buttonPressed) != null)
        {
            updateButtons();
            return true;
        }

        int xpos = buttonMinXPos;
        int ypos = 0;
        for (GUIButton button : buttons)
        {
            // If there is not enough space for this button, move onto the next line
            if (xpos + button.width > buttonMaxXPos)
            {
                ypos++;
                xpos = buttonMinXPos;
            }

            int currentY = buttonMinYPos + ypos * (4 + GUIButton.height);

            if (button.mouseClick((int)mouseX - (edgeSpacingX + xpos), (int)mouseY - (edgeSpacingY + currentY), buttonPressed))
            {
                changeSelection(button);
                return true;
            }

            // Move the xpos next to this button
            xpos += button.width + 3;
        }

        if (super.mouseClicked(mouseX, mouseY, buttonPressed))
            return true;

        return false;
    }

    public void changeSelection(GUIButton selected)
    {
        for (GUIButton button : buttons)
            if (button != selected)
                button.unselect();

        // TODO: Send new selection to server
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double direction)
    {
        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        // Scroll through the bodyPart list
        if (bodyParts.mouseScroll((int)mouseX - (edgeSpacingX + bodyPartListXPos), (int)mouseY - (edgeSpacingY + bodyPartListYPos), direction))
            return true;

        if (bodySubParts.mouseScroll((int)mouseX - (edgeSpacingX + bodySubPartListXPos), (int)mouseY - (edgeSpacingY + bodySubPartListYPos), direction))
            return true;

        return false;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        drawGuiBackgroundLayer(matrixStack, partialTicks, mouseX, mouseY);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawGuiForgroundLayer(matrixStack, partialTicks, mouseX, mouseY);
    }

    protected void drawGuiForgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        // Render part buttons
        int xpos = buttonMinXPos;
        int ypos = 0;
        for (GUIButton button : buttons)
        {
            // If there is not enough space for this button, move onto the next line
            if (xpos + button.width > buttonMaxXPos)
            {
                ypos++;
                xpos = buttonMinXPos;
            }

            int currentY = buttonMinYPos + ypos * (4 + GUIButton.height);

            button.render(matrixStack, edgeSpacingX + xpos, edgeSpacingY + currentY, mouseX, mouseY, this);

            // Move the xpos next to this button
            xpos += button.width + 3;
        }

        // Render the BodyPart dropdown list
        bodySubParts.render(matrixStack, edgeSpacingX + bodySubPartListXPos, edgeSpacingY + bodySubPartListYPos, mouseX, mouseY, this);
        bodyParts.render(matrixStack, edgeSpacingX + bodyPartListXPos, edgeSpacingY + bodyPartListYPos, mouseX, mouseY, this);
    }

    protected void drawGuiBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);

        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;
        this.blit(matrixStack, edgeSpacingX, edgeSpacingY, 0, 0, this.xSize, this.ySize);

        ScreenTabControl.highlightTabs(matrixStack, 2, mouseX, mouseY, edgeSpacingX, edgeSpacingY, this);


        // Draw the players body parts
        int bodyPosX = edgeSpacingX + 20;
        int bodyPosY = edgeSpacingY + 60;

        IBodyModifications modifications = BodyModifications.getBodyModifications(Minecraft.getInstance().player);

        // Get the body GUI, this is special as all other GUIs positions are based off this
        BodyPartGUI base;

        if (modifications.hasModification(BodyPartNames.bodyPosition))
            base = BodyPartGUIs.getGUI(modifications.getModification(BodyPartNames.bodyPosition).getModelIDs().get(0));
        else
            base = BodyPartGUIs.getGUI(BodyPartNames.DefaultBody);

        boolean highlight = false;
        if (selectedPosition == BodyPartNames.bodyPosition)
            highlight = true;

        base.render(matrixStack, bodyPosX, bodyPosY, highlight, this);


        // Draw generic body parts if alterations don't exist
        highlight = false;
        if (!modifications.hasModification(BodyPartNames.headPosition))
        {
            if (selectedPosition == BodyPartNames.headPosition)
                highlight = true;

            BodyPartGUIs.getGUI(BodyPartNames.DefaultHead).render(matrixStack, bodyPosX, bodyPosY, highlight, this, base);
        }


        highlight = false;
        if (!modifications.hasModification(BodyPartNames.armPosition))
        {
            if (selectedPosition == BodyPartNames.armPosition)
                highlight = true;

            BodyPartGUIs.getGUI(BodyPartNames.DefaultRightArm).render(matrixStack, bodyPosX, bodyPosY, highlight, this, base);
            BodyPartGUIs.getGUI(BodyPartNames.DefaultLeftArm).render(matrixStack, bodyPosX, bodyPosY, highlight,this, base);
        }

        highlight = false;
        if (!modifications.hasModification(BodyPartNames.legPosition))
        {
            if (selectedPosition == BodyPartNames.legPosition)
                highlight = true;

            BodyPartGUIs.getGUI(BodyPartNames.DefaultRightLeg).render(matrixStack, bodyPosX, bodyPosY, highlight, this, base);
            BodyPartGUIs.getGUI(BodyPartNames.DefaultLeftLeg).render(matrixStack, bodyPosX, bodyPosY, highlight, this, base);
        }

        // Loop through and draw all other body part GUIs if they exist
        for (Map.Entry<String, BodyPart> entry : modifications.getModifications().entrySet())
        {
            // Don't redraw the body
            if (entry.getKey() != BodyPartNames.bodyPosition)
                for (String ID : entry.getValue().getModelIDs())
                {
                    highlight = false;

                    if (selectedPosition == BodyPartNames.getPart(ID).getPosition())
                        highlight = true;

                    BodyPartGUI gui = BodyPartGUIs.getGUI(ID);

                    if (gui != null)
                        gui.render(matrixStack, bodyPosX, bodyPosY, highlight, this, base);
                }
        }
    }
}
