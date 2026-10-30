package lNalator.Cultivationcraft.Client.GUI.ItemDecorators;

import lNalator.Cultivationcraft.Common.FlyingSwordBind;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemDecorator;

public class FlyingSwordDecorator implements IItemDecorator {

    public static final ResourceLocation icon = new ResourceLocation(Cultivationcraft.MODID, "textures/gui/flyingswordicon.png");

    private static final int xSize = 16;
    private static final int ySize = 16;

    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        // Don't do anything if the item isn't bound
        if (!FlyingSwordBind.isBound(stack)) {
            return false;
        }

        // Render as red if not the owning player
        if (FlyingSwordBind.getOwner(stack).compareTo(Minecraft.getInstance().player.getUUID()) == 0) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); 
        }else {
            RenderSystem.setShaderColor(0.8F, 0.0F, 0.0F, 1.0F);
        }

        guiGraphics.blit(icon, xOffset, yOffset, 300, 0, 0, xSize, ySize, xSize, ySize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        return true;
    }
}
