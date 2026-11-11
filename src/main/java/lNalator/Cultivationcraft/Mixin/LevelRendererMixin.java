package lNalator.Cultivationcraft.Mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import lNalator.Cultivationcraft.Client.Renderers.TribulationRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Unique
    private boolean cultivationcraft$fadedClouds = false;

    // Vanilla clouds fade out near a tribulation storm, and are not drawn at all under it
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void cultivationcraft$fadeClouds(CallbackInfo callback) {
        float cover = TribulationRenderer.vanillaCloudCover();

        if (cover >= 0.99f) {
            callback.cancel();
        } else if (cover > 0) {
            RenderSystem.setShaderColor(1, 1, 1, 1 - cover);
            cultivationcraft$fadedClouds = true;
        }
    }

    @Inject(method = "renderClouds", at = @At("RETURN"))
    private void cultivationcraft$restoreCloudColor(CallbackInfo callback) {
        if (cultivationcraft$fadedClouds) {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            cultivationcraft$fadedClouds = false;
        }
    }

    // Wind: near a storm the clouds drift faster, their offset follows the render ticks
    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;ticks:I"))
    private int cultivationcraft$windClouds(LevelRenderer renderer) {
        return ((LevelRendererAccessor) renderer).cultivationcraft$getTicks() + TribulationRenderer.vanillaCloudWindTicks();
    }
}
