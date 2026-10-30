package lNalator.Cultivationcraft.Client.GUI.Screens;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.FoodStats.QiFoodStats;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class ResourceReadout {

    static void draw(GuiGraphics guiGraphics, Font font, Player player, int x, int y, int width) {
        if (player == null) {
            return;
        }
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        double stamina = Math.max(0, player.getFoodData() instanceof QiFoodStats food
                ? food.getTrueFoodLevel() : player.getFoodData().getFoodLevel());
        boolean external = CultivatorStats.getCultivatorStats(player).getCultivationType() == CultivationTypes.QI_CONDENSER;
        String[] keys = {external ? "available_qi" : "available_stamina", "available_hp"};
        String[] values = {format.format(stamina),
            format.format(player.getHealth()) + " / " + format.format(player.getMaxHealth())
                    + (player.getAbsorptionAmount() > 0 ? " (+" + format.format(player.getAbsorptionAmount()) + ")" : "")};
        for (int i = 0; i < keys.length; i++) {
            Component label = Component.translatable("cultivationcraft.gui." + keys[i]);
            guiGraphics.drawString(font, label, x, y + i * 14, 0xDDDDDD, true);
            float scale = Math.min(1, Math.max(1, width - font.width(label) - 8) / (float) Math.max(1, font.width(values[i])));
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(x + width, y + i * 14, 0);
            pose.scale(scale, scale, 1);
            guiGraphics.drawString(font, values[i], -font.width(values[i]), 0, 0xFFFFFF, true);
            pose.popPose();
        }
    }
}
