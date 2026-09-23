package DaoOfModding.Cultivationcraft.Client.Renderers.BakedModels;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.TextureStitchEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles registration and lookup of the procedural plant texture parts.
 */
public final class ProceduralPlantTextures {
    private static final List<Material> STEMS = buildMaterials(PlantVisuals.stemVariantCount(), PlantVisuals::stemTexture);
    private static final List<Material> FOLIAGE = buildMaterials(PlantVisuals.foliageVariantCount(), PlantVisuals::foliageTexture);
    private static final List<Material> FRUITS = buildMaterials(PlantVisuals.fruitVariantCount(), PlantVisuals::fruitTexture);

    private ProceduralPlantTextures() {}

    private static List<Material> buildMaterials(int count, java.util.function.IntFunction<ResourceLocation> supplier) {
        List<Material> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new Material(InventoryMenu.BLOCK_ATLAS, supplier.apply(i)));
        }
        return Collections.unmodifiableList(result);
    }

    public static void register(TextureStitchEvent.Pre event) {
        if (!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) return;
        STEMS.forEach(material -> event.addSprite(material.texture()));
        FOLIAGE.forEach(material -> event.addSprite(material.texture()));
        FRUITS.forEach(material -> event.addSprite(material.texture()));
    }

    public static int stemCount() {
        return STEMS.size();
    }

    public static int foliageCount() {
        return FOLIAGE.size();
    }

    public static int fruitCount() {
        return FRUITS.size();
    }

    public static Material stemMaterial(int index) {
        return STEMS.get(Math.max(0, Math.min(index, STEMS.size() - 1)));
    }

    public static Material foliageMaterial(int index) {
        return FOLIAGE.get(Math.max(0, Math.min(index, FOLIAGE.size() - 1)));
    }

    public static Material fruitMaterial(int index) {
        return FRUITS.get(Math.max(0, Math.min(index, FRUITS.size() - 1)));
    }

    public static TextureAtlasSprite sprite(Material material) {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(material.atlasLocation());
        return atlas.getSprite(material.texture());
    }

    public static TextureAtlasSprite stemSprite(int index) {
        return sprite(stemMaterial(index));
    }

    public static TextureAtlasSprite foliageSprite(int index) {
        return sprite(foliageMaterial(index));
    }

    public static TextureAtlasSprite fruitSprite(int index) {
        return sprite(fruitMaterial(index));
    }
}
