package DaoOfModding.Cultivationcraft.Client;

import DaoOfModding.Cultivationcraft.Common.Alchemy.PillEffects;
import DaoOfModding.Cultivationcraft.Common.Items.AlchemyPillItem;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class AlchemyPillPresentation {
    public static Component name(ItemStack stack) {
        return PillEffects.identified(Minecraft.getInstance().player, stack)
                ? Component.literal(stack.getTag().getString("PillName")) : Component.literal("???");
    }

    public static int color(ItemStack stack, int layer) {
        return PillEffects.identified(Minecraft.getInstance().player, stack)
                ? 0xFF000000 | stack.getTag().getInt("Color") : 0xFF000000;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof AlchemyPillItem)) return;
        var lines = event.getToolTip();
        lines.clear(); // Includes advanced tooltips: no recipe/effect leaks before identification.
        lines.add(name(stack));
        if (!PillEffects.identified(Minecraft.getInstance().player, stack)) return;
        var tag = stack.getTag();
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        String effect = tag.getString("Effect").toLowerCase(Locale.ROOT);
        double amount = tag.getDouble("Amount");
        int duration = tag.getInt("Duration");
        if (effect.equals("heal") || effect.equals("heal_over_time") || effect.equals("qi") || effect.equals("qi_over_time")) amount *= 100;
        lines.add(Component.translatable("cultivationcraft.pill.tier_purity", tag.getInt("Tier"), tag.getInt("Purity")).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("cultivationcraft.pill.effect." + effect, format.format(amount), duration));
        ResourceLocation affinity = ResourceLocation.tryParse(tag.getString("Affinity"));
        if (affinity != null) {
            lines.add(Component.translatable("cultivationcraft.pill.affinity", Component.translatable(affinity.getPath())));
            lines.add(Component.translatable("cultivationcraft.pill.foundation_only").withStyle(ChatFormatting.GRAY));
        }
        if (effect.equals("food")) lines.add(Component.translatable("cultivationcraft.pill.body_only").withStyle(ChatFormatting.GRAY));
        if (effect.equals("qi") || effect.equals("qi_over_time"))
            lines.add(Component.translatable("cultivationcraft.pill.external_only").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("cultivationcraft.pill.shared_cooldown", tag.getInt("Cooldown")).withStyle(ChatFormatting.GRAY));
    }
}
