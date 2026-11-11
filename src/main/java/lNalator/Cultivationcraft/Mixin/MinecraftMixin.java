package lNalator.Cultivationcraft.Mixin;

import lNalator.Cultivationcraft.Client.Renderers.DivineSenseBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    // Divine Sense outlines the entities it senses with the glowing effect, through walls
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void cultivationcraft$divineSenseGlowing(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (DivineSenseBlockRenderer.isGlowing(entity)) {
            callback.setReturnValue(true);
        }
    }
}
