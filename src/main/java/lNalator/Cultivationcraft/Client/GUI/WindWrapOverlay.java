package lNalator.Cultivationcraft.Client.GUI;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Levitation + Wind synergy: "2/3" above the Qi orb while entities are wrapped in wind
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class WindWrapOverlay {

    private static int count = 0;
    private static int max = 0;

    private WindWrapOverlay() {
    }

    public static void set(int newCount, int newMax) {
        count = newCount;
        max = newMax;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        count = 0;
    }

    public static void render(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();

        if (count <= 0 || mc.player == null) {
            return;
        }

        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();
        String text = Component.translatable("cultivationcraft.windwrap.counter", count, max).getString();

        // Centred above the Qi orb drawn by Renderer.renderStamina (50 px wide, from 55 px above the bottom)
        int x = (int) (scaledWidth * 0.1 - 5) + 25 - mc.font.width(text) / 2;
        int y = scaledHeight - 66;

        mc.font.drawShadow(poseStack, text, x, y, 0xD8F5FF);
    }
}
