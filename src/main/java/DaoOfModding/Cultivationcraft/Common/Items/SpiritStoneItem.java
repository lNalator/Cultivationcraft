package DaoOfModding.Cultivationcraft.Common.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;
import javax.annotation.Nullable;

public final class SpiritStoneItem extends Item {
    public SpiritStoneItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("cultivationcraft.spirit_stone.refine").withStyle(ChatFormatting.GRAY));
    }
}
