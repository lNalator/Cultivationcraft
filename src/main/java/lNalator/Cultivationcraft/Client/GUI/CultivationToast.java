package lNalator.Cultivationcraft.Client.GUI;

import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.CultivationNotifier;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

// Advancement-style toast with an icon, a title, one line of text and a sound per type
public class CultivationToast implements Toast {

    protected final CultivationNotifier.Type type;
    protected final Component title;
    protected final Component text;

    public CultivationToast(CultivationNotifier.Type type, Component title, Component text) {
        this.type = type;
        this.title = title;
        this.text = text;
    }

    public static void show(CultivationNotifier.Type type, Component title, Component text) {
        if (!Config.Client.enabled(type)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getToasts().addToast(new CultivationToast(type, title, text));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound(type), 1.0F));
    }

    protected static SoundEvent sound(CultivationNotifier.Type type) {
        return switch (type) {
            case SUCCESS -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
            case PROGRESS -> SoundEvents.PLAYER_LEVELUP;
            case ALERT -> SoundEvents.NOTE_BLOCK_BASS;
        };
    }

    protected ItemStack icon() {
        return new ItemStack(switch (type) {
            case SUCCESS -> Items.NETHER_STAR;
            case PROGRESS -> Items.EXPERIENCE_BOTTLE;
            case ALERT -> Items.REDSTONE_TORCH;
        });
    }

    protected int titleColor() {
        return switch (type) {
            case SUCCESS -> 0xFFFF00;
            case PROGRESS -> 0x80FF80;
            case ALERT -> 0xFF5555;
        };
    }

    @Override
    public Visibility render(PoseStack stack, ToastComponent toasts, long time) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        toasts.blit(stack, 0, 0, 0, 0, width(), height());

        var font = toasts.getMinecraft().font;
        font.draw(stack, title, 30, 7, titleColor());

        // ponytail: only the first line fits the vanilla 32px toast
        List<FormattedCharSequence> lines = font.split(text, 125);
        if (!lines.isEmpty()) {
            font.draw(stack, lines.get(0), 30, 18, 0xFFFFFF);
        }

        toasts.getMinecraft().getItemRenderer().renderAndDecorateFakeItem(icon(), 8, 8);

        return time >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
