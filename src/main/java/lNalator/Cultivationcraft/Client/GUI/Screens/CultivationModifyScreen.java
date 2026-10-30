package lNalator.Cultivationcraft.Client.GUI.Screens;

import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.TechniqueStatModification;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class CultivationModifyScreen extends TechniqueModifyScreen {

    CultivationType cultivation;

    public CultivationModifyScreen(CultivationType cult, int activeTab) {
        super(cult.getPassive(), activeTab);

        cultivation = cult;
    }

    @Override
    protected void drawStats(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (cultivation.statsCanLevel() && selectedTech.getFirstStatChange() != null) {
            TechniqueStatModification modifications = getStatModification();

            stats = new ArrayList<>();
            for (ResourceLocation stat : cultivation.getPassive().getTechniqueStats()) {
                stats.add(stat);
            }

            for (int stat = 0; stat < stats.size(); stat++) {
                ResourceLocation statLocation = stats.get(stat);

                double value = modifications.getStatChange(statLocation);
                drawStatChanges(guiGraphics, stat, stats.get(stat), value);

                // Only draw button if this stat can level
                if (selectedTech.getStatChangesPerLevel(statLocation) != null) {
                    drawButton(guiGraphics, stat, mouseX, mouseY);
                }
            }
        }

        drawStats(cultivation, guiGraphics, new ArrayList<>(), 0);
        ResourceReadout.draw(guiGraphics, font, Minecraft.getInstance().player,
                (width - xSize) / 2 + 20, (height - ySize) / 2 + 137, xSize - 40);

        stats = new ArrayList<>();
        for (ResourceLocation stat : cultivation.getPassive().getTechniqueStats()) {
            stats.add(stat);
        }
    }

    protected void drawStats(CultivationType cultivating, GuiGraphics guiGraphics, ArrayList<ResourceLocation> done, int location) {
        stats = new ArrayList<>();

        for (ResourceLocation stat : cultivating.getPassive().getTechniqueStats()) {
            if (!done.contains(stat)) {
                stats.add(stat);
            }
        }

        for (int stat = 0; stat < stats.size(); stat++) {
            ResourceLocation statLocation = stats.get(stat);
            done.add(statLocation);

            double statValue = cultivating.getCultivationStat(Minecraft.getInstance().player, statLocation);

            drawStat(guiGraphics, stat + location, stats.get(stat), statValue);
        }

        if (cultivating.getPreviousCultivation() != null) {
            drawStats(cultivating.getPreviousCultivation(), guiGraphics, done, location + stats.size());
        }
    }
}
