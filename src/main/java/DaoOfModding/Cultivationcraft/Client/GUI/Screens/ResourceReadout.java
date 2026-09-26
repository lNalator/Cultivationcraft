package DaoOfModding.Cultivationcraft.Client.GUI.Screens;

import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Qi.CultivationTypes;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.FoodStats.QiFoodStats;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class ResourceReadout {
    static void draw(PoseStack pose, Font font, Player player, int x, int y, int width) {
        if (player == null) return;
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        double stamina = Math.max(0, player.getFoodData() instanceof QiFoodStats food
                ? food.getTrueFoodLevel() : player.getFoodData().getFoodLevel());
        boolean external = CultivatorStats.getCultivatorStats(player).getCultivationType() == CultivationTypes.QI_CONDENSER;
        String[] keys = {external ? "available_qi" : "available_stamina", "available_hp"};
        String[] values = {format.format(stamina),
                format.format(player.getHealth()) + " / " + format.format(player.getMaxHealth())};
        for (int i = 0; i < keys.length; i++) {
            Component label = Component.translatable("cultivationcraft.gui." + keys[i]);
            font.drawShadow(pose, label, x, y + i * 14, 0xDDDDDD);
            float scale = Math.min(1, Math.max(1, width - font.width(label) - 8) / (float) Math.max(1, font.width(values[i])));
            pose.pushPose();
            pose.translate(x + width, y + i * 14, 0);
            pose.scale(scale, scale, 1);
            font.drawShadow(pose, values[i], -font.width(values[i]), 0, 0xFFFFFF);
            pose.popPose();
        }
    }
}
