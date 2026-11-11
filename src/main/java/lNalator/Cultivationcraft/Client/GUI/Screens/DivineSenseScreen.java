package lNalator.Cultivationcraft.Client.GUI.Screens;

import com.mojang.blaze3d.vertex.PoseStack;
import lNalator.Cultivationcraft.Client.GUI.GUIButton;
import lNalator.Cultivationcraft.Client.GUI.ScreenTabControl;
import lNalator.Cultivationcraft.Client.KeybindingControl;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique.SenseProfile;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Divine Sense page of the cultivation menu: its state, current stats, what the next stage and realm give, its
 * categories and its controls. Divine Sense is innate (not in a technique slot), so this is where its stats live;
 * the radial menu shows a short version (DivineSenseMenuScreen).
 */
public class DivineSenseScreen extends GenericTabScreen {

    protected static final int TEXT_X = 16;
    protected static final int TEXT_Y = 14;
    protected static final int TEXT_WIDTH = 224;
    protected static final int SECTION_GAP = 4;

    protected static final String[] REALM_NAMES = {
            "cultivationcraft.divinesense.screen.realm.none",
            "cultivationcraft.divinesense.screen.realm.foundation",
            "cultivationcraft.divinesense.screen.realm.qicondensation",
            "cultivationcraft.divinesense.screen.realm.coreforming"
    };

    // The realm screens the player can be in, by DivineSenseTechnique.getRealm; above that, no next realm is known yet
    protected static final int MAX_REALM = 3;

    protected GUIButton back;

    protected int backX = 16;
    protected int buttonsY = 160;

    public DivineSenseScreen() {
        super(0, Component.translatable("cultivationcraft.divinesense.screen"), new ResourceLocation(Cultivationcraft.MODID, "textures/gui/stats.png"));

        back = new GUIButton("BACK", Component.translatable("cultivationcraft.divinesense.screen.back").getString());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonPressed) {
        if (super.mouseClicked(mouseX, mouseY, buttonPressed)) {
            return true;
        }

        if (back.mouseClick((int) mouseX, (int) mouseY, buttonPressed)) {
            ScreenTabControl.openTab();
            return true;
        }

        return false;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);

        int edgeSpacingX = (this.width - this.xSize) / 2;
        int edgeSpacingY = (this.height - this.ySize) / 2;

        DivineSenseTechnique sense = DivineSenseTechnique.find(minecraft.player);

        int x = edgeSpacingX + TEXT_X;
        int y = edgeSpacingY + TEXT_Y;

        if (sense == null) {
            for (var line : font.split(Component.translatable("cultivationcraft.divinesense.screen.unavailable"), TEXT_WIDTH)) {
                font.draw(poseStack, line, x, y, 0);
                y += font.lineHeight + 1;
            }
        } else {
            for (Component line : lines(sense)) {
                if (line == null) {
                    y += SECTION_GAP;
                    continue;
                }

                for (var wrapped : font.split(line, TEXT_WIDTH)) {
                    font.draw(poseStack, wrapped, x, y, 0);
                    y += font.lineHeight + 1;
                }
            }
        }

        back.setPos(edgeSpacingX + backX, edgeSpacingY + buttonsY);
        back.render(poseStack, mouseX, mouseY, this);
    }

    // The page's text; null = a small gap between two sections
    protected List<Component> lines(DivineSenseTechnique sense) {
        List<Component> lines = new ArrayList<>();
        CultivationType cultivation = CultivatorStats.getCultivatorStats(minecraft.player).getCultivation();
        int realm = DivineSenseTechnique.getRealm(minecraft.player);
        int maxStage = Math.max(1, cultivation.getMaxStage());
        int minorStage = Mth.clamp(cultivation.getStage(), 1, maxStage) - 1;
        SenseProfile current = DivineSenseTechnique.getSenseProfile(realm, minorStage);

        lines.add(Component.translatable("cultivationcraft.divinesense.menu.state",
                Component.translatable(sense.isActive() ? "cultivationcraft.divinesense.menu.on" : "cultivationcraft.divinesense.menu.off")));
        lines.add(null);

        // Current stats
        lines.add(Component.translatable("cultivationcraft.divinesense.stat.radius", current.radius()));
        lines.add(Component.translatable("cultivationcraft.divinesense.stat.highlights", current.maxHighlights()));

        if (CultivatorStats.getCultivatorStats(minecraft.player).getCultivationType() == CultivationTypes.QI_CONDENSER) {
            lines.add(Component.translatable("cultivationcraft.divinesense.stat.qicost",
                    String.format("%.1f", sense.getTechniqueStat(DefaultTechniqueStatIDs.qiCost, minecraft.player))));
        } else {
            lines.add(Component.translatable("cultivationcraft.divinesense.stat.hunger"));
        }

        lines.add(null);

        // What the next stage and the next realm give: major realms give much more than stages
        if (realm > 0 && minorStage + 1 < maxStage) {
            SenseProfile next = DivineSenseTechnique.getSenseProfile(realm, minorStage + 1);
            lines.add(Component.translatable("cultivationcraft.divinesense.screen.nextstage", next.radius(), next.maxHighlights()));
        }

        if (realm > 0 && realm < MAX_REALM) {
            SenseProfile next = DivineSenseTechnique.getSenseProfile(realm + 1, 0);
            lines.add(Component.translatable("cultivationcraft.divinesense.screen.nextrealm",
                    Component.translatable(REALM_NAMES[realm + 1]), next.radius(), next.maxHighlights()));
        }

        lines.add(Component.translatable("cultivationcraft.divinesense.screen.growth"));
        lines.add(null);

        // Categories sensed, set in the radial menu
        List<String> sensed = new ArrayList<>();
        addCategory(sense, sensed, DivineSenseTechnique.SENSE_QI_SOURCES, "cultivationcraft.divinesense.menu.qisources");
        addCategory(sense, sensed, DivineSenseTechnique.SENSE_BLOCKS, "cultivationcraft.divinesense.menu.blocks");
        addCategory(sense, sensed, DivineSenseTechnique.SENSE_CREATURES, "cultivationcraft.divinesense.menu.creatures");
        addCategory(sense, sensed, DivineSenseTechnique.SENSE_PLAYERS, "cultivationcraft.divinesense.menu.players");
        addCategory(sense, sensed, DivineSenseTechnique.SENSE_TRIBULATIONS, "cultivationcraft.divinesense.menu.tribulations");

        lines.add(Component.translatable("cultivationcraft.divinesense.screen.categories",
                sensed.isEmpty() || !sense.isActive() ? Component.translatable("cultivationcraft.divinesense.screen.nocategory").getString() : String.join(", ", sensed)));

        // Controls, with the key the player actually bound
        lines.add(Component.translatable("cultivationcraft.divinesense.screen.controls", KeybindingControl.keyBindings[1].getTranslatedKeyMessage()));

        return lines;
    }

    protected static void addCategory(DivineSenseTechnique sense, List<String> sensed, int category, String name) {
        if (sense.senses(category)) {
            sensed.add(Component.translatable(name).getString());
        }
    }
}
