package DaoOfModding.Cultivationcraft.Client.GUI;

import DaoOfModding.Cultivationcraft.Client.genericClientFunctions;
import DaoOfModding.Cultivationcraft.Client.GUI.Screens.BodyforgeScreen;
import DaoOfModding.Cultivationcraft.Client.GUI.Screens.StatScreen;
import DaoOfModding.Cultivationcraft.Client.GUI.Screens.TechniqueScreen;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Qi.CultivationTypes;
import DaoOfModding.Cultivationcraft.Common.Register;
import DaoOfModding.Cultivationcraft.Network.ClientPacketHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class ScreenTabControl
{
    protected static final int TAB_BAR_NUMBER = 4;

    protected static final int TAB_BAR_PADDING = 2;

    protected static final int TAB_BAR_X_SIZE = 49;
    protected static final int TAB_BAR_Y_SIZE = 14;

    protected static final int TAB_BAR_U = 97;
    protected static final int TAB_BAR_V = 242;

    protected static final int HIGHLIGHTED_TAB_BAR_X_SIZE = 49;
    protected static final int HIGHLIGHTED_TAB_BAR_Y_SIZE = 13;

    protected static final int HIGHLIGHTED_TAB_BAR_U = 206;
    protected static final int HIGHLIGHTED_TAB_BAR_V = 243;

    protected static final int SELECTED_TAB_BAR_X_SIZE = 49;
    protected static final int SELECTED_TAB_BAR_Y_SIZE = 17;

    protected static final int SELECTED_TAB_BAR_U = 0;
    protected static final int SELECTED_TAB_BAR_V = 239;

    protected static final int fontColor = Color.BLACK.getRGB();

    public static void drawTabs(PoseStack PoseStack, int screenX, int screenY, Screen drawTo)
    {
        // Loop through each tab, checking if the mouse is over it and highlighting it if so
        for (int i = 0; i < TAB_BAR_NUMBER; i++)
            drawTo.blit(PoseStack, screenX + (TAB_BAR_X_SIZE + TAB_BAR_PADDING) * i, screenY, TAB_BAR_U, TAB_BAR_V, TAB_BAR_X_SIZE, TAB_BAR_Y_SIZE);
    }

    public static void highlightTabs(PoseStack PoseStack, int tabSelected, int mouseX, int mouseY, int screenX, int screenY, Screen drawTo)
    {
        mouseX = mouseX - screenX;
        mouseY = mouseY - screenY;

        // Loop through each tab, checking if the mouse is over it and highlighting it if so
        for (int i = 0; i < TAB_BAR_NUMBER; i++)
            if (tabSelected != i)
            {
                if (mouseOver(mouseX, mouseY, i))
                    drawTo.blit(PoseStack, screenX + (TAB_BAR_X_SIZE + TAB_BAR_PADDING) * i, screenY, HIGHLIGHTED_TAB_BAR_U, HIGHLIGHTED_TAB_BAR_V, HIGHLIGHTED_TAB_BAR_X_SIZE, HIGHLIGHTED_TAB_BAR_Y_SIZE);
            }
            else
                drawTo.blit(PoseStack, screenX + (TAB_BAR_X_SIZE + TAB_BAR_PADDING) * i, screenY, SELECTED_TAB_BAR_U, SELECTED_TAB_BAR_V, SELECTED_TAB_BAR_X_SIZE, SELECTED_TAB_BAR_Y_SIZE);
    }

    public static void tabText(PoseStack PoseStack, int screenX, int screenY, Font font)
    {
        for (int i = 0; i < TAB_BAR_NUMBER; i++)
        {
            String text = Component.translatable("cultivationcraft.gui.tab" + i).getString();

            // Draw the text centered on each tab
            font.draw(PoseStack, text, screenX + (TAB_BAR_X_SIZE + TAB_BAR_PADDING) * i + TAB_BAR_X_SIZE / 2 - font.width(text) / 2 + 1, screenY + font.lineHeight / 2, fontColor);
        }
    }

    public static boolean mouseClick(int mouseX, int mouseY, int screenX, int screenY, int buttonPressed)
    {
        // If not a left click, ignore
        if (buttonPressed != 0)
            return false;

        mouseX = mouseX - screenX;
        mouseY = mouseY - screenY;

        // Loop through each tab, checking if the mouse is over it and switching to it if so
        for (int i = 0; i < TAB_BAR_NUMBER; i++)
            if (mouseOver(mouseX, mouseY, i))
            {
                if (i == 0)
                    Minecraft.getInstance().forceSetScreen(new StatScreen());
                else if (i == 1)
                    Minecraft.getInstance().forceSetScreen(new TechniqueScreen());
                else if (i == 2)
                {
                    int cultivationType = CultivatorStats.getCultivatorStats(genericClientFunctions.getPlayer()).getCultivationType();

                    if (cultivationType == CultivationTypes.QI_CONDENSER)
                        ClientPacketHandler.sendKeypressToServer(Register.keyPresses.FLYINGSWORDSCREEN);
                    else if (cultivationType == CultivationTypes.BODY_CULTIVATOR)
                        Minecraft.getInstance().forceSetScreen(new BodyforgeScreen());
                }

                return true;
            }

        return false;
    }

    public static boolean mouseOver(int mouseX, int mouseY, int tab)
    {
        if (mouseX >= (TAB_BAR_X_SIZE + TAB_BAR_PADDING) * tab && mouseX <= (TAB_BAR_X_SIZE + TAB_BAR_PADDING) * tab + TAB_BAR_X_SIZE &&
            mouseY >= 0 && mouseY <= TAB_BAR_Y_SIZE)
            return true;

        return false;
    }
}
