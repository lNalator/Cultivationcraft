package lNalator.Cultivationcraft.Client.GUI.Screens.CultivationTypeScreens;

import net.minecraft.client.gui.GuiComponent;
import lNalator.Cultivationcraft.Client.GUI.GUIButton;
import lNalator.Cultivationcraft.Client.Textures.AlphaOverlayTexture;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.ClientPacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QiCondenserScreen extends CultivationTypeScreen {

    // Cycles through the absorbed elements to purge, in the generating cycle order, then back to none
    protected static final ResourceLocation[] PURGE_ORDER = {Elements.fireElement, Elements.earthElement, Elements.woodElement, Elements.waterElement,
            Elements.windElement, Elements.iceElement, Elements.lightningElement};

    protected GUIButton purgeButton = new GUIButton("PURGE", "");

    final int purgeButtonX = 60;
    final int purgeButtonY = 140;

    // Where the condenser was last drawn, for its tooltip
    protected int panelX = 0;
    protected int panelY = 0;

    public QiCondenserScreen() {
        TEXTURE = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/qicondenser.png");

        xSize = 76;
        ySize = 115;
    }

    @Override
    public void renderButtons(PoseStack poseStack, int xPos, int yPos, int mouseX, int mouseY, GuiComponent gui) {
        super.renderButtons(poseStack, xPos, yPos, mouseX, mouseY, gui);

        if (!(CultivatorStats.getCultivatorStats(Minecraft.getInstance().player).getCultivation() instanceof QiCondenserCultivation condenser)) {
            return;
        }

        String purging = condenser.isPurging() ? Elements.getElement(condenser.getPurgingElement()).getName() : Component.translatable("cultivationcraft.gui.purge.none").getString();
        String text = Component.translatable("cultivationcraft.gui.purge", purging).getString();

        purgeButton.setText(text);
        purgeButton.width = GUIButton.baseWidth + Minecraft.getInstance().font.width(text);
        purgeButton.setPos(xPos + purgeButtonX, yPos + purgeButtonY);
        purgeButton.render(poseStack, mouseX, mouseY, gui);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonPressed) {
        if (super.mouseClicked(mouseX, mouseY, buttonPressed)) {
            return true;
        }

        if (purgeButton.mouseClick((int) mouseX, (int) mouseY, buttonPressed)) {
            purgeButton.unselect();

            if (CultivatorStats.getCultivatorStats(Minecraft.getInstance().player).getCultivation() instanceof QiCondenserCultivation condenser) {
                ClientPacketHandler.sendPurgeElementToServer(nextPurge(condenser));
            }

            return true;
        }

        return false;
    }

    // The next absorbed element after the one being purged, or none after the last one
    protected ResourceLocation nextPurge(QiCondenserCultivation condenser) {
        List<ResourceLocation> purgeable = new ArrayList<>();

        for (ResourceLocation element : PURGE_ORDER) {
            if (condenser.canPurge(element)) {
                purgeable.add(element);
            }
        }

        for (ResourceLocation element : condenser.getElementAmounts().keySet()) {
            if (!purgeable.contains(element) && condenser.canPurge(element)) {
                purgeable.add(element);
            }
        }

        int current = purgeable.indexOf(condenser.getPurgingElement());

        return current + 1 < purgeable.size() ? purgeable.get(current + 1) : Elements.noElement;
    }

    // Hovering the condenser shows the share of every element absorbed, Pure Qi and the void left by purges
    @Override
    public void renderTooltips(PoseStack poseStack, int mouseX, int mouseY, Screen screen, CultivationType cultivation) {
        if (!(cultivation instanceof QiCondenserCultivation condenser) || mouseX < panelX || mouseX >= panelX + xSize || mouseY < panelY || mouseY >= panelY + ySize + 15) {
            return;
        }

        HashMap<ResourceLocation, Float> amounts = condenser.getElementAmounts();
        float voidQi = condenser.getVoid();
        float total = voidQi;

        for (float amount : amounts.values()) {
            total += amount;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("cultivationcraft.gui.purge.absorbed"));

        for (Map.Entry<ResourceLocation, Float> entry : amounts.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }

            String name = entry.getKey().equals(Elements.anyElement) ? Component.translatable("cultivationcraft.gui.purge.pure").getString() : Elements.getElement(entry.getKey()).getName();

            if (entry.getKey().equals(condenser.getPurgingElement())) {
                name = Component.translatable("cultivationcraft.gui.purge.purging", name).getString();
            }

            lines.add(Component.literal(name + ": " + percent(entry.getValue(), total) + "%"));
        }

        if (voidQi > 0) {
            lines.add(Component.literal("§7" + Component.translatable("cultivationcraft.gui.purge.void").getString() + ": " + percent(voidQi, total) + "%"));
        }

        screen.renderComponentTooltip(poseStack, lines, mouseX, mouseY);
    }

    protected static int percent(float amount, float total) {
        return total > 0 ? Math.round(amount / total * 100) : 0;
    }

    public void render(PoseStack PoseStack, float partialTicks, int xPos, int yPos, Screen screen, CultivationType cultivation, Font font) {
        panelX = xPos;
        panelY = yPos;

        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float progressInterval = 1f / cultivation.getMaxStage();
        float currentProgress = (float) cultivation.getTechLevelProgressWithoutPrevious(cultivation.getPassive().getClass().toString()) / (float) cultivation.getMaxTechLevelWithoutPrevious();
        currentProgress *= progressInterval;
        currentProgress += progressInterval * (cultivation.getStage() - 1);

        currentProgress *= 0.9;

        ((AlphaOverlayTexture) Minecraft.getInstance().getTextureManager().getTexture(TEXTURE)).setAlphaSize(1f - currentProgress);

        GuiComponent.blit(PoseStack, xPos, yPos, screen.getBlitOffset(), 0, 0, xSize, ySize, xSize, ySize);

        ResourceLocation element = ((QiCondenserCultivation) cultivation).getCurrentElementFocus();
        int percent = (int) (((QiCondenserCultivation) cultivation).getElementFocusAmount(element) * 100);

        String text = Elements.getElement(element).getName() + " " + percent + "%";

        font.draw(PoseStack, text, xPos + xSize / 2 - font.width(text), yPos + ySize + 5, 0);
    }
}
