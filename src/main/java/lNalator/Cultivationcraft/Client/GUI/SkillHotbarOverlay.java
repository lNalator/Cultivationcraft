package lNalator.Cultivationcraft.Client.GUI;

import lNalator.Cultivationcraft.Client.KeybindingControl;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Network.ClientPacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class SkillHotbarOverlay {

    protected static final ResourceLocation WIDGETS_TEX_PATH = new ResourceLocation("textures/gui/widgets.png");

    // Whether the hotbar is active or not
    protected static boolean isActive = false;

    // The hotbar slot currently selected
    protected static int skillSelected = 0;

    // Render in the post-render phase if the hotbar is active
    // In the pre-render phase if it isn't
    // This is necessary so that it renders in the right order for transparency to work properly
    public static void PreRenderSkillHotbar(GuiGraphics guiGraphics) {
        if (!isActive) {
            RenderSkillHotbar(guiGraphics);
        }
    }

    public static void PostRenderSkillHotbar(GuiGraphics guiGraphics) {
        if (isActive) {
            RenderSkillHotbar(guiGraphics);
        }
    }

    protected static void RenderSkillHotbar(GuiGraphics guiGraphics) {
        // Enable transparency
        RenderSystem.enableBlend();

        int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // Store the current Z layer to reset back to later
        guiGraphics.pose().pushPose();

        // Set the Z-layer to either in front of or behind the item hotbar based on whether this hotbar is active or not
        if (isActive) {
            guiGraphics.pose().translate(0, 0, 200); 
        }else {
            guiGraphics.pose().translate(0, 0, -91);
        }

        // Draw the hotbar
        guiGraphics.blit(WIDGETS_TEX_PATH, scaledWidth - 83, scaledHeight - 30, 0, 0, 182, 22);

        // Draw the selection box
        guiGraphics.blit(WIDGETS_TEX_PATH, scaledWidth - 83 - 1 + skillSelected * 20, scaledHeight - 30 - 1, 0, 22, 24, 22);

        // Draw icons in the selection box
        TechniqueIcons.renderIcons(guiGraphics, scaledWidth + 3 - 83, scaledHeight + 3 - 30, 20);
        TechniqueIcons.highlightActive(guiGraphics, scaledWidth + 3 - 83, scaledHeight + 3 - 30, 20);
        TechniqueIcons.showCooldowns(guiGraphics, scaledWidth + 3 - 83, scaledHeight + 3 - 30, 20);
        renderKeyLabels(guiGraphics, scaledWidth + 3 - 83, scaledHeight + 3 - 30);

        // Reset the Z layer to its original value
        guiGraphics.pose().popPose();
    }

    // Called when the use item button is pressed and the hotbar is active
    public static void useSkill(boolean keyDown) {
        if (!isActive()) {
            return;
        }

        useSkill(skillSelected, keyDown);
    }

    // Use the technique in the given slot (from the use button or the slot's dedicated key)
    public static void useSkill(int slot, boolean keyDown) {
        Technique tech = CultivatorTechniques.getCultivatorTechniques(Minecraft.getInstance().player).getTechnique(slot);

        if (tech != null && (tech.getType() != Technique.useType.Channel || !(tech.isActive() && keyDown))) // Send message to server to use this skill
        {
            ClientPacketHandler.sendTechniqueUseToServer(slot, keyDown);
        }

        if (tech != null & !keyDown && tech.getType() != Technique.useType.Channel) {
            tech.deactivate(Minecraft.getInstance().player);
        }
    }

    // Draw the bound key of each slot in the top-left corner of its icon (visible even behind the item hotbar)
    protected static void renderKeyLabels(GuiGraphics guiGraphics, int xpos, int ypos) {
        if (KeybindingControl.techniqueKeys == null) {
            return;
        }

        for (int i = 0; i < KeybindingControl.techniqueKeys.length; i++) {
            KeyMapping key = KeybindingControl.techniqueKeys[i];

            if (key.isUnbound()) {
                continue;
            }

            // ponytail: long key names ("Left Shift") are cut to 3 chars
            String label = key.getTranslatedKeyMessage().getString();
            label = label.substring(0, Math.min(3, label.length()));

            // The pose is already translated to the hotbar's Z layer
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(xpos + i * 20 - 2, ypos - 2, 1);
            guiGraphics.pose().scale(0.5F, 0.5F, 1F);
            guiGraphics.drawString(Minecraft.getInstance().font, label, 0, 0, 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    public static void setSelection(int selection) {
        skillSelected = selection;
    }

    public static void changeCurrentSelection(double direction) {
        if (direction > 0.0D) {
            direction = 1.0D;
        }

        if (direction < 0.0D) {
            direction = -1.0D;
        }

        skillSelected -= direction;

        if (skillSelected > 8) {
            skillSelected = 0; 
        }else if (skillSelected < 0) {
            skillSelected = 8;
        }
    }

    public static void switchActive() {
        isActive = !isActive;
    }

    public static void setActive(boolean on) {
        isActive = on;
    }

    public static boolean isActive() {
        return isActive;
    }

    public static void reset() {
        isActive = false;
        skillSelected = 0;
    }
}
