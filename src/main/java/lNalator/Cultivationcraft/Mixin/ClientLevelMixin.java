package lNalator.Cultivationcraft.Mixin;

import lNalator.Cultivationcraft.Client.Renderers.TribulationRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    // Colour of the tribulation storm's clouds
    @Unique
    private static final Vec3 cultivationcraft$STORM_CLOUDS = new Vec3(0.12, 0.12, 0.14);

    // Vanilla clouds darken to the storm's colour near a tribulation
    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void cultivationcraft$stormCloudColor(float partialTick, CallbackInfoReturnable<Vec3> callback) {
        float cover = TribulationRenderer.vanillaCloudCover();

        if (cover > 0) {
            callback.setReturnValue(callback.getReturnValue().lerp(cultivationcraft$STORM_CLOUDS, cover));
        }
    }
}
