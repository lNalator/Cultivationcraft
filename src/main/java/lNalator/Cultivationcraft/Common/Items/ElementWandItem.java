package lNalator.Cultivationcraft.Common.Items;

import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// Debug stick to test elements without a matching cultivation:
// right-click an entity = elemental hit, a block = block effect, the air = hit yourself; sneak + right/left-click = next/previous element
public class ElementWandItem extends Item {

    protected static final ResourceLocation[] ELEMENTS = {Elements.fireElement, Elements.waterElement, Elements.iceElement,
            Elements.windElement, Elements.lightningElement, Elements.earthElement, Elements.woodElement};
    protected static final float DAMAGE = 4;

    public ElementWandItem(Properties properties) {
        super(properties);
    }

    protected static ResourceLocation element(ItemStack stack) {
        return ELEMENTS[Math.floorMod(stack.getOrCreateTag().getInt("Element"), ELEMENTS.length)];
    }

    protected static void hit(Player player, LivingEntity target, ItemStack stack) {
        // Skip invulnerability frames so effects can be chained quickly
        target.invulnerableTime = 0;
        target.hurt(new QiDamageSource("player", player, element(stack), true), DAMAGE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                cycle(player, stack, 1);
            } else {
                hit(player, player, stack);
            }
        }

        // Consume (no swing) when cycling, otherwise the swing would cycle back (see onEntitySwing)
        return player.isShiftKeyDown() ? InteractionResultHolder.consume(stack) : InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    protected static void cycle(Player player, ItemStack stack, int step) {
        stack.getOrCreateTag().putInt("Element", Math.floorMod(stack.getOrCreateTag().getInt("Element") + step, ELEMENTS.length));
        player.displayClientMessage(Component.translatable(element(stack).getPath()), true);
    }

    // Sneak + left-click (any target or the air) = previous element. Every left-click swings, and the server sees the swing.
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (!entity.level.isClientSide && entity instanceof Player player && player.isShiftKeyDown()) {
            cycle(player, stack, -1);
        }

        return false;
    }

    // While sneaking, left-click neither breaks the block nor attacks the entity
    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player) {
        return !player.isShiftKeyDown();
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity entity) {
        return player.isShiftKeyDown();
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            // Falls through to use(), which cycles
            return InteractionResult.PASS;
        }

        if (!player.level.isClientSide) {
            hit(player, target, stack);
        }

        return InteractionResult.sidedSuccess(player.level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!context.getLevel().isClientSide) {
            Elements.getElement(element(context.getItemInHand())).effectBlock(context.getLevel(), context.getClickedPos());
        }

        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).append(" (").append(Component.translatable(element(stack).getPath())).append(")");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cultivationcraft.element_wand.tooltip"));
    }
}
