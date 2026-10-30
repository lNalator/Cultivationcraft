package lNalator.Cultivationcraft.Client.Tooltip;

import lNalator.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

public class PlantBadgeTooltip implements ClientTooltipComponent {
    private final PlantBadgeTooltipData data;
    public PlantBadgeTooltip(PlantBadgeTooltipData data) { this.data = data; }

    @Override
    public int getHeight() { return 12; }

    @Override
    public int getWidth(Font font) { return 12 * 2 + 4; }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        var entry = ClientPlantCatalog.get(data.speciesId());
        int color = entry != null ? entry.color : 0x888888;
        // Colored square
        guiGraphics.fill(x, y, x + 12, y + 12, 0xFF000000 | color);

        // Element icon using new path: textures/gui/plants/element_icons/<element_path>.png
        if (entry != null && entry.element != null) {
            ResourceLocation el = new ResourceLocation(entry.element);
            ResourceLocation icon = new ResourceLocation("cultivationcraft", "textures/gui/plants/element_icons/" + el.getPath() + ".png");
            guiGraphics.blit(icon, x + 16, y, 0, 0, 12, 12, 12, 12);
        } else {
            guiGraphics.fill(x + 16, y, x + 28, y + 12, 0xFF333333);
        }
        // Host badge overlay
        if (data.host()) {
            guiGraphics.fill(x + 8, y - 2, x + 14, y + 4, 0xAA00FFFF);
        }

        // Tiny dynamic tier label (T1/T2/T3) when available
        if (data.dynTier() > 0) {
            String t = "T" + data.dynTier();
            int bx = x + 1;
            int by = y + 1;
            guiGraphics.fill(bx, by, bx + 11, by + 9, 0xAA000000);
            guiGraphics.drawString(font, t, bx + 2, by + 1, 0xFFFFFFFF, false);
        }
    }
}
