package lNalator.Cultivationcraft.Client.GUI.Screens;

import lNalator.Cultivationcraft.Client.GUI.ScreenTabControl;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Containers.RefinementContainer;
import lNalator.Cultivationcraft.Common.Qi.Techniques.MeditateTechnique;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.awt.*;

public class RefinementScreen extends AbstractContainerScreen<RefinementContainer> {

    // The original 256x178 layout sits in the shared panel offset by EXTRA_X / TOP_Y,
    // with the inventory pushed down by EXTRA_Y to fit a full-width status line
    protected static final int EXTRA_X = (GenericTabScreen.PANEL_WIDTH - 256) / 2;
    protected static final int EXTRA_Y = GenericTabScreen.PANEL_HEIGHT - 178;
    protected static final int TOP_Y = EXTRA_Y / 2;
    // Status line, relative to the panel
    protected static final int STATUS_Y = 98;

    protected static final int PROGRESS_BAR_X_POS = 84 + EXTRA_X;
    protected static final int PROGRESS_BAR_Y_POS = 77 + TOP_Y;

    protected static final int PROGRESS_BAR_X_SIZE = 89;
    protected static final int PROGRESS_BAR_Y_SIZE = 4;

    protected static final int PROGRESS_BAR_U = 0;
    protected static final int PROGRESS_BAR_V = 252;

    private int tab = 2;

    public RefinementScreen(RefinementContainer container, Inventory playerInv, Component title) {
        super(container, playerInv, title);

        imageWidth = GenericTabScreen.PANEL_WIDTH;
        imageHeight = GenericTabScreen.PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        // Slots are positioned for the original panel: shift them into the enlarged one
        this.leftPos += 41 + EXTRA_X;
        this.topPos += EXTRA_Y;
        tab = lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats
                .getCultivatorStats(Minecraft.getInstance().player).getCultivationType()
                == lNalator.Cultivationcraft.Common.Qi.CultivationTypes.BODY_CULTIVATOR ? 4 : 2;
    }

    @Override
    public void render(PoseStack PoseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(PoseStack);
        super.render(PoseStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(PoseStack, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonPressed) {
        int edgeSpacingX = (this.width - this.imageWidth) / 2;
        int edgeSpacingY = (this.height - this.imageHeight) / 2;

        if (ScreenTabControl.mouseClick((int) mouseX, (int) mouseY, edgeSpacingX, edgeSpacingY, buttonPressed)) {
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, buttonPressed)) {
            return true;
        }

        return false;
    }

    @Override
    protected void renderLabels(PoseStack PoseStack, int mouseX, int mouseY) {
        final float PLAYER_LABEL_XPOS = 8;
        final float PLAYER_LABEL_YPOS = 178 - 94;

        final float BAG_LABEL_YPOS = 24 + TOP_Y - EXTRA_Y;

        // Label coordinates start at the shifted slot origin, convert panel coordinates back to them
        final int panelToLabelX = -(41 + EXTRA_X);
        final int panelToLabelY = -EXTRA_Y;

        String testString = Component.translatable("cultivationcraft.gui.refinement").getString();

        float BAG_LABEL_XPOS = (178 / 2.0F) - font.width(testString) / 2.0F;
        font.draw(PoseStack, testString, BAG_LABEL_XPOS, BAG_LABEL_YPOS, Color.darkGray.getRGB());

        font.draw(PoseStack, this.playerInventoryTitle.getString(),
                PLAYER_LABEL_XPOS, PLAYER_LABEL_YPOS, Color.darkGray.getRGB());

        String progress;

        if (menu.getRefinementProgress() == 1) {
            progress = Component.translatable("cultivationcraft.gui.done").getString(); 
        }else if (menu.getRefinementProgress() == 0) {
            progress = ""; 
        }else {
            progress = Component.translatable("cultivationcraft.gui.remaining", (int) menu.getRemainingSeconds()).getString();
        }

        int color = Color.darkGray.getRGB();

        // Refining needs Meditate equipped (see RefinementProcessor), say so instead of a silent stall
        if (!menu.slots.get(menu.slots.size() - 1).getItem().isEmpty() && !hasMeditate()) {
            progress = Component.translatable("cultivationcraft.refinement.needs_meditate").getString();
            color = Color.red.getRGB();
        }

        // Full-width status line between the progress bar and the inventory, centered and wrapped
        final int margin = 8;
        float y = STATUS_Y + panelToLabelY;
        for (FormattedCharSequence line : font.split(Component.literal(progress), imageWidth - margin * 2)) {
            font.draw(PoseStack, line, panelToLabelX + (imageWidth - font.width(line)) / 2.0F, y, color);
            y += font.lineHeight;
        }
    }

    protected static boolean hasMeditate() {
        ICultivatorTechniques techs = CultivatorTechniques.getCultivatorTechniques(Minecraft.getInstance().player);
        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            if (techs.getTechnique(i) instanceof MeditateTechnique) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderBg(PoseStack PoseStack, float partialTicks, int mouseX, int mouseY) {
        drawGuiBackgroundLayer(PoseStack, partialTicks, mouseX, mouseY);

        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int edgeSpacingX = (this.width - this.imageWidth) / 2;
        int edgeSpacingY = (this.height - this.imageHeight) / 2;
        // Original overlay: top part (slot, bar) in place, inventory part pushed down by EXTRA_Y
        final int split = 90;
        this.blit(PoseStack, edgeSpacingX + EXTRA_X, edgeSpacingY + TOP_Y, 0, 0, 256, split);
        this.blit(PoseStack, edgeSpacingX + EXTRA_X, edgeSpacingY + split + EXTRA_Y, 0, split, 256, 178 - split);

        // Draw the progress bar based on refinement progress
        float progress = menu.getRefinementProgress() * PROGRESS_BAR_X_SIZE;

        // If progress is greater than 0 draw the progress bar
        if (progress > 0) {
            this.blit(PoseStack, edgeSpacingX + PROGRESS_BAR_X_POS, edgeSpacingY + PROGRESS_BAR_Y_POS, PROGRESS_BAR_U, PROGRESS_BAR_V, (int) progress, PROGRESS_BAR_Y_SIZE); 
        }// If progress is less than 0 draw the progress to unbind bar
        else {
            this.blit(PoseStack, edgeSpacingX + PROGRESS_BAR_X_POS + PROGRESS_BAR_X_SIZE - (int) (progress * -1), edgeSpacingY + PROGRESS_BAR_Y_POS,
                    PROGRESS_BAR_U, PROGRESS_BAR_V - PROGRESS_BAR_Y_SIZE,
                    (int) progress * -1, PROGRESS_BAR_Y_SIZE);
        }
    }

    protected void drawGuiBackgroundLayer(PoseStack PoseStack, float partialTicks, int mouseX, int mouseY) {
        // Draw the generic tab screen
        RenderSystem.setShaderTexture(0, GenericTabScreen.tabTexture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int edgeSpacingX = (this.width - this.imageWidth) / 2;
        int edgeSpacingY = (this.height - this.imageHeight) / 2;

        GenericTabScreen.drawPanel(PoseStack, this, tab, mouseX, mouseY, edgeSpacingX, edgeSpacingY);
    }

    protected static final ResourceLocation TEXTURE = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/refinement.png");
}
