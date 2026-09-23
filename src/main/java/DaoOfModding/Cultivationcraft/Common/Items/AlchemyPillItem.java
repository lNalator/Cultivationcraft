package DaoOfModding.Cultivationcraft.Common.Items;

import DaoOfModding.Cultivationcraft.Common.Alchemy.PillEffects;
import DaoOfModding.Cultivationcraft.Client.AlchemyPillPresentation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class AlchemyPillItem extends Item {
    public AlchemyPillItem(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) {
        Component name = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> AlchemyPillPresentation.name(stack));
        return name == null ? Component.literal("???") : name;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        return player instanceof ServerPlayer server && PillEffects.consume(server, stack)
                ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }
}
