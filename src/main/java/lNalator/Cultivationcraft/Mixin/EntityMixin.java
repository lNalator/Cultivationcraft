package lNalator.Cultivationcraft.Mixin;

import lNalator.Cultivationcraft.Client.Renderers.DivineSenseBlockRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    // The glowing outline takes the team colour: entities sensed by Divine Sense take the colour of their kind instead
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void cultivationcraft$divineSenseColor(CallbackInfoReturnable<Integer> callback) {
        int color = DivineSenseBlockRenderer.glowColor((Entity) (Object) this);

        if (color >= 0) {
            callback.setReturnValue(color);
        }
    }
}
