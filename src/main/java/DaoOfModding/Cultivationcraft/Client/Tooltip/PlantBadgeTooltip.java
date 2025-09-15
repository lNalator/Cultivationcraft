package DaoOfModding.Cultivationcraft.Client.Tooltip;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.ResourceLocation;

public class PlantBadgeTooltip implements ClientTooltipComponent {
    private final PlantBadgeTooltipData data;
    public PlantBadgeTooltip(PlantBadgeTooltipData data) { this.data = data; }

    @Override
    public int getHeight() { return 12; }

    @Override
    public int getWidth(Font font) { return 12 * 2 + 4; }

    public void renderImage(Font font, int x, int y, PoseStack pose, BufferSource buffer) {
        var mc = Minecraft.getInstance();
        var e = ClientPlantCatalog.get(data.speciesId());
        if (e == null) return;
        int color = e.color;
        // Draw simple colored squares as placeholders; replace with textures if provided.
        GuiComponent.fill(pose, x, y, x + 12, y + 12, 0xFF000000 | color);

        // Element icon
        ResourceLocation elementIcon = new ResourceLocation("cultivationcraft", "textures/gui/element_icons/" + e.element.replace(':','_') + ".png");
        // If texture exists, draw it; else draw border
        var tex = mc.getTextureManager().getTexture(elementIcon);
        if (tex != null) {
            mc.getTextureManager().bindForSetup(elementIcon);
            GuiComponent.blit(pose, x + 16, y, 0, 0, 12, 12, 12, 12);
        } else {
            GuiComponent.fill(pose, x + 16, y, x + 16 + 12, y + 12, 0xFF333333);
        }
        // Host badge overlay
        if (data.host()) {
            GuiComponent.fill(pose, x + 9, y - 2, x + 9 + 6, y + 4, 0xAA00FFFF);
        }
    }
}

