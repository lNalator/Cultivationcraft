package DaoOfModding.Cultivationcraft.Client;

import DaoOfModding.Cultivationcraft.Client.Renderers.BakedModels.ProceduralPlantBakedModel;
import DaoOfModding.Cultivationcraft.Client.Renderers.BakedModels.ProceduralPlantTextures;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ProceduralPlantRenderSetup {
    private static final ResourceLocation PROCEDURAL_PLANT = new ResourceLocation(Cultivationcraft.MODID, "procedural_plant");

    private ProceduralPlantRenderSetup() {}

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        ProceduralPlantTextures.register(event);
    }

    @SubscribeEvent
    public static void onModelsReady(ModelEvent.BakingCompleted event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        List<ModelResourceLocation> targets = new ArrayList<>();
        for (ResourceLocation key : models.keySet()) {
            if (key instanceof ModelResourceLocation mrl) {
                if (mrl.getNamespace().equals(PROCEDURAL_PLANT.getNamespace()) && mrl.getPath().equals(PROCEDURAL_PLANT.getPath())) {
                    targets.add(mrl);
                }
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        BakedModel base = models.get(targets.get(0));
        if (base == null) {
            return;
        }
        ProceduralPlantBakedModel replacement = new ProceduralPlantBakedModel(base);
        for (ModelResourceLocation key : targets) {
            models.put(key, replacement);
        }
    }
}



