package DaoOfModding.Cultivationcraft.Client.Tooltip;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;

public class PlantBadgeTooltip implements ClientTooltipComponent {
    private final PlantBadgeTooltipData data;
    public PlantBadgeTooltip(PlantBadgeTooltipData data) { this.data = data; }

    @Override
    public int getHeight() { return 12; }

    @Override
    public int getWidth(Font font) { return 12 * 2 + 4; }

    public void renderImage(Font font, int x, int y, PoseStack pose, ItemRenderer itemRenderer) {
        var entry = ClientPlantCatalog.get(data.speciesId());
        int color = entry != null ? entry.color : 0x888888;
        // Colored square
        GuiComponent.fill(pose, x, y, x + 12, y + 12, 0xFF000000 | color);

        // Element icon using new path: textures/gui/plants/element_icons/<element_path>.png
        if (entry != null && entry.element != null) {
            ResourceLocation el = new ResourceLocation(entry.element);
            ResourceLocation icon = new ResourceLocation("cultivationcraft", "textures/gui/plants/element_icons/" + el.getPath() + ".png");
            RenderSystem.setShaderTexture(0, icon);
            GuiComponent.blit(pose, x + 16, y, 0, 0, 12, 12, 12, 12);
        } else {
            GuiComponent.fill(pose, x + 16, y, x + 28, y + 12, 0xFF333333);
        }
        // Host badge overlay
        if (data.host()) {
            GuiComponent.fill(pose, x + 8, y - 2, x + 14, y + 4, 0xAA00FFFF);
        }
    }
}
