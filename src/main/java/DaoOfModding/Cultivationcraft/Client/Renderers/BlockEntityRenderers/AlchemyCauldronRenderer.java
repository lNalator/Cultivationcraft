package DaoOfModding.Cultivationcraft.Client.Renderers.BlockEntityRenderers;

import DaoOfModding.Cultivationcraft.Common.Blocks.entity.AlchemyCauldronBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

public class AlchemyCauldronRenderer implements BlockEntityRenderer<AlchemyCauldronBlockEntity> {
    private final ItemRenderer items;

    public AlchemyCauldronRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(AlchemyCauldronBlockEntity cauldron, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        double time = cauldron.getLevel() == null ? 0 : cauldron.getLevel().getGameTime() + (double) partialTick;
        int occupied = 0;
        for (int slot = 0; slot < AlchemyCauldronBlockEntity.SLOT_COUNT; slot++) {
            if (!cauldron.getItem(slot).isEmpty()) occupied++;
        }
        int orbitIndex = 0;
        for (int slot = 0; slot < AlchemyCauldronBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = cauldron.getItem(slot);
            if (stack.isEmpty()) continue;
            pose.pushPose();
            // One visual per occupied slot; the stack itself stays in the saved inventory.
            double phase = orbitIndex++ * Math.PI * 2 / occupied;
            double angle = time * 0.035 + phase;
            double radius = 0.19 + Math.sin(time * 0.025 + phase * 2) * 0.045;
            pose.translate(0.5 + Math.cos(angle) * radius,
                    0.82 + Math.sin(time * 0.05 + phase) * 0.065,
                    0.5 + Math.sin(angle) * radius);
            pose.mulPose(Vector3f.YP.rotationDegrees((float) ((time * 1.5 + slot * 40) % 360)));
            pose.scale(0.22f, 0.22f, 0.22f);
            items.renderStatic(stack, ItemTransforms.TransformType.GROUND, light, overlay, pose, buffers,
                    (int) cauldron.getBlockPos().asLong() + slot);
            pose.popPose();
        }
    }
}
