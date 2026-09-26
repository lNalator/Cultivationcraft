package DaoOfModding.Cultivationcraft.Common.Items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import java.util.List;

public final class JadeSlipItem extends Item {
    public JadeSlipItem(Properties properties) { super(properties.stacksTo(16)); }
    @Override public Component getName(ItemStack stack) {
        return stack.hasTag() && !stack.getTag().getString("SlipTitle").isBlank()
                ? Component.literal(stack.getTag().getString("SlipTitle")) : super.getName(stack);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        Boolean known = net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> stack.hasTag() && DaoOfModding.Cultivationcraft.Client.ClientKnowledge.knows(stack.getTag().getString("Knowledge")));
        if (Boolean.TRUE.equals(known)) lines.add(Component.translatable("cultivationcraft.jade.already_learned").withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("cultivationcraft.jade.hint").withStyle(ChatFormatting.GRAY));
    }
}
