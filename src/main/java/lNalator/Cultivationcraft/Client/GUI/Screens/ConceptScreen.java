package lNalator.Cultivationcraft.Client.GUI.Screens;

import lNalator.Cultivationcraft.Client.GUI.GUIButton;
import lNalator.Cultivationcraft.Client.GUI.SelectableTextField;
import lNalator.Cultivationcraft.Client.GUI.TextField;
import lNalator.Cultivationcraft.Common.Qi.ExternalCultivationHandler;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.ClientPacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConceptScreen extends GenericTabScreen {

    protected int conceptTextX = 100;
    protected int conceptTextY = 35;
    protected int conceptTextWidth = 140;
    protected int conceptTextHeight = 121;

    protected int selectTextX = 10;
    protected int selectTextY = 35;
    protected int selectTextWidth = 80;
    protected int selectTextHeight = 121;

    protected int nameTextY = 22;

    protected TextField conceptText = new TextField();
    protected SelectableTextField selectText = new SelectableTextField();

    protected boolean selectable = false;

    protected GUIButton select;

    protected int selectButtonX = 127;
    protected int selectButtonY = 160;

    // Outside of a breakthrough, the player switches between the concepts unlocked and the ones condensed into a core
    protected boolean showCores = false;
    protected GUIButton unlockedButton;
    protected GUIButton coresButton;

    public ConceptScreen() {
        this(false);
    }

    public ConceptScreen(boolean choose) {
        super(0, Component.translatable("cultivationcraft.gui.concept"), new ResourceLocation(Cultivationcraft.MODID, "textures/gui/stats.png"));

        selectable = choose;
        generateText();

        selectText.setSize(selectTextWidth, selectTextHeight);

        if (selectable) {
            select = new GUIButton("select", Component.translatable("cultivationcraft.gui.select").getString());
            select.disable();
        } else {
            unlockedButton = new GUIButton("unlocked", Component.translatable("cultivationcraft.gui.concept.unlocked").getString());
            coresButton = new GUIButton("cores", Component.translatable("cultivationcraft.gui.concept.cores").getString());
            unlockedButton.select();
        }
    }

    public void generateText() {
        conceptText.resetScroll();

        HashMap<ResourceLocation, ArrayList<ResourceLocation>> categories = new HashMap<>();
        selectText = new SelectableTextField();

        Player player = Minecraft.getInstance().player;

        for (TechniqueModifier modifier : ExternalCultivationHandler.getModifiers()) {
            boolean owned = CultivatorStats.getCultivatorStats(player).getCultivation().getModifierRank(modifier.ID) > 0;

            if (selectable ? modifier.canChooseAsCore(player) : showCores ? owned : modifier.hasLearnt(player) && !owned) {
                if (!categories.containsKey(modifier.CATEGORY)) {
                    categories.put(modifier.CATEGORY, new ArrayList<>());
                }

                categories.get(modifier.CATEGORY).add(modifier.ID);
            }
        }

        for (Map.Entry<ResourceLocation, ArrayList<ResourceLocation>> entry : categories.entrySet()) {
            SelectableText category = new SelectableText(entry.getKey());

            for (ResourceLocation location : entry.getValue()) {
                // An owned core chosen again is reinforced: show the rank it would reach
                int rank = CultivatorStats.getCultivatorStats(Minecraft.getInstance().player).getCultivation().getModifierRank(location);

                if (selectable && rank > 0) {
                    category.addItem(new SelectableText(location, Component.translatable(location.toLanguageKey()).append(" ").append(Component.translatable("cultivationcraft.gui.rank", rank + 1))));
                } else if (showCores) {
                    category.addItem(new SelectableText(location, Component.translatable(location.toLanguageKey()).append(" ").append(Component.translatable("cultivationcraft.gui.rank", rank))));
                } else {
                    category.addItem(new SelectableText(location));
                }
            }

            selectText.addSelectable(category);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonPressed) {
        if (super.mouseClicked(mouseX, mouseY, buttonPressed)) {
            return true;
        }

        if (selectable && select.mouseClick((int) mouseX, (int) mouseY, buttonPressed)) {
            ClientPacketHandler.sendBreakthroughExtraToServer(selectText.getSelected().getID().toString());
            this.onClose();

            return true;
        }

        if (!selectable) {
            boolean unlockedClicked = unlockedButton.mouseClick((int) mouseX, (int) mouseY, buttonPressed);

            if (unlockedClicked || coresButton.mouseClick((int) mouseX, (int) mouseY, buttonPressed)) {
                showCores = !unlockedClicked;

                if (showCores) {
                    coresButton.select();
                    unlockedButton.unselect();
                } else {
                    unlockedButton.select();
                    coresButton.unselect();
                }

                generateText();
                selectText.setSize(selectTextWidth, selectTextHeight);

                return true;
            }
        }

        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        if (mouseX > selectTextX + edgeSpacingX && mouseX < selectTextX + edgeSpacingX + selectText.getWidth()) {
            if (mouseY > selectTextY + edgeSpacingY && mouseY < selectTextY + edgeSpacingY + selectTextHeight) {
                selectText.mouseClicked(mouseX - (selectTextX + edgeSpacingX), mouseY - (selectTextY + edgeSpacingY), buttonPressed);
            }
        }

        if (mouseX > conceptTextX + edgeSpacingX && mouseX < conceptTextX + edgeSpacingX + conceptTextWidth) {
            if (mouseY > conceptTextY + edgeSpacingY && mouseY < conceptTextY + edgeSpacingY + conceptTextHeight) {
                return conceptText.mouseClicked(mouseX - (conceptTextX + edgeSpacingX), mouseY - (conceptTextY + edgeSpacingY), buttonPressed);
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double direction) {
        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        if (mouseX > selectTextX + edgeSpacingX && mouseX < selectTextX + edgeSpacingX + selectText.getWidth()) {
            if (mouseY > selectTextY + edgeSpacingY && mouseY < selectTextY + edgeSpacingY + selectTextHeight) {
                return selectText.mouseScrolled(direction);
            }
        }

        if (mouseX > conceptTextX + edgeSpacingX && mouseX < conceptTextX + edgeSpacingX + conceptTextWidth) {
            if (mouseY > conceptTextY + edgeSpacingY && mouseY < conceptTextY + edgeSpacingY + conceptTextHeight) {
                return conceptText.mouseScrolled(direction);
            }
        }

        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        String text = selectText.getText();

        if (!selectable && selectText.getSelectables().isEmpty()) {
            text = Component.translatable(showCores ? "cultivationcraft.gui.concept.nocores" : "cultivationcraft.gui.concept.nounlocked").getString();
        }

        if (selectable) {
            select.disable();
        }

        if (selectText.getSelected() != null) {
            String name = Component.translatable(selectText.getSelected().getName()).getString();
            guiGraphics.drawString(font, name, edgeSpacingX + this.xSize / 2 - font.width(name) / 2, edgeSpacingY + nameTextY, Color.darkGray.getRGB(), false);

            // Only allow the select button to be pressed if a concept is selected and not a category
            if (selectable) {
                select.enable();

                for (SelectableText test : selectText.getSelectables()) {
                    if (test == selectText.getSelected()) {
                        select.disable();
                    }
                }
            }
        }

        conceptText.setPos(edgeSpacingX + conceptTextX, edgeSpacingY + conceptTextY);
        conceptText.setSize(conceptTextWidth, conceptTextHeight);
        conceptText.setText(text);
        conceptText.render(guiGraphics, font, mouseX, mouseY);

        selectText.setPos(edgeSpacingX + selectTextX, edgeSpacingY + selectTextY);
        selectText.render(guiGraphics, font, mouseX, mouseY);

        if (selectable) {
            select.setPos(edgeSpacingX + selectButtonX - select.width / 2, selectButtonY + edgeSpacingY);
            select.render(guiGraphics, mouseX, mouseY);
        } else {
            unlockedButton.setPos(edgeSpacingX + selectButtonX - unlockedButton.width - 2, selectButtonY + edgeSpacingY);
            unlockedButton.render(guiGraphics, mouseX, mouseY);
            coresButton.setPos(edgeSpacingX + selectButtonX + 2, selectButtonY + edgeSpacingY);
            coresButton.render(guiGraphics, mouseX, mouseY);
        }
    }
}
