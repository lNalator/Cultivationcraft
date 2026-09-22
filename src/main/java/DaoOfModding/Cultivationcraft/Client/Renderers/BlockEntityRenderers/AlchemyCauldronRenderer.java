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
        float time = cauldron.getLevel() == null ? 0 : cauldron.getLevel().getGameTime() + partialTick;
        for (int slot = 0; slot < cauldron.getContainerSize(); slot++) {
            ItemStack stack = cauldron.getItem(slot);
            if (stack.isEmpty()) continue;
            pose.pushPose();
            // One visual per occupied slot; the stack itself stays in the saved inventory.
            pose.translate(0.28 + (slot % 3) * 0.22,
                    0.83 + Math.sin(time * 0.06 + slot) * 0.025, 0.28 + (slot / 3) * 0.22);
            pose.mulPose(Vector3f.YP.rotationDegrees(time * 1.5f + slot * 40));
            pose.scale(0.22f, 0.22f, 0.22f);
            items.renderStatic(stack, ItemTransforms.TransformType.GROUND, light, overlay, pose, buffers,
                    (int) cauldron.getBlockPos().asLong() + slot);
            pose.popPose();
        }
    }
}
