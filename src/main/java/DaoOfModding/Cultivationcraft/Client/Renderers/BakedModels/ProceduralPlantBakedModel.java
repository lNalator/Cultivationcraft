package DaoOfModding.Cultivationcraft.Client.Renderers.BakedModels;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantVisuals;
import DaoOfModding.Cultivationcraft.Common.Items.ProceduralPlantItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic baked model composed from stem/foliage/fruit sprite layers tinted by species colour.
 */
public class ProceduralPlantBakedModel implements BakedModel {
    private final BakedModel baseModel;
    private final List<BakedQuad> templateQuads;
    private final TextureAtlasSprite particleSprite;
    private final ItemTransforms transforms;

    private final Map<Integer, List<BakedQuad>> stemCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<BakedQuad>> foliageCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<BakedQuad>> fruitCache = new ConcurrentHashMap<>();

    private final ItemOverrides overrides;

    public ProceduralPlantBakedModel(BakedModel baseModel) {
        this.baseModel = baseModel;
        BlockState sampleState = BlockRegister.PROCEDURAL_PLANT.get().defaultBlockState();
        this.templateQuads = List.copyOf(baseModel.getQuads(sampleState, null, RandomSource.create(), ModelData.EMPTY, null));
        this.particleSprite = baseModel.getParticleIcon();
        this.transforms = baseModel.getTransforms();
        this.overrides = new SpeciesOverrides(this);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (side != null) {
            return Collections.emptyList();
        }
        int species = 0;
        if (state != null && state.hasProperty(ProceduralPlantBlock.SPECIES)) {
            species = state.getValue(ProceduralPlantBlock.SPECIES);
        }
        return getQuadsForSpecies(species);
    }

    private List<BakedQuad> getQuadsForSpecies(int species) {
        ClientPlantCatalog.Entry entry = ClientPlantCatalog.get(species);
        if (entry == null) {
            return templateQuads;
        }

        List<BakedQuad> result = new ArrayList<>(16);
        result.addAll(getStemLayer(entry.stemVariant));
        result.addAll(getFoliageLayer(entry.foliageVariant));
        if (entry.fruitVariant != PlantVisuals.NO_FRUIT) {
            result.addAll(getFruitLayer(entry.fruitVariant));
        }
        return List.copyOf(result);
    }

    private List<BakedQuad> getStemLayer(int variant) {
        return stemCache.computeIfAbsent(variant, idx -> remap(templateQuads, ProceduralPlantTextures.stemSprite(idx)));
    }

    private List<BakedQuad> getFoliageLayer(int variant) {
        return foliageCache.computeIfAbsent(variant, idx -> remap(templateQuads, ProceduralPlantTextures.foliageSprite(idx)));
    }

    private List<BakedQuad> getFruitLayer(int variant) {
        if (variant == PlantVisuals.NO_FRUIT) {
            return Collections.emptyList();
        }
        return fruitCache.computeIfAbsent(variant, idx -> remap(templateQuads, ProceduralPlantTextures.fruitSprite(idx)));
    }

    private List<BakedQuad> remap(List<BakedQuad> template, TextureAtlasSprite sprite) {
        List<BakedQuad> list = new ArrayList<>(template.size());
        for (BakedQuad quad : template) {
            list.add(retexture(quad, sprite));
        }
        return List.copyOf(list);
    }

    private BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {
        int[] data = quad.getVertices().clone();
        TextureAtlasSprite old = quad.getSprite();
        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            float u = Float.intBitsToFloat(data[offset + 4]);
            float v = Float.intBitsToFloat(data[offset + 5]);
            float un = unInterpolate(u, old.getU0(), old.getU1());
            float vn = unInterpolate(v, old.getV0(), old.getV1());
            data[offset + 4] = Float.floatToRawIntBits(sprite.getU(un));
            data[offset + 5] = Float.floatToRawIntBits(sprite.getV(vn));
        }
        return new BakedQuad(data, 0, quad.getDirection(), sprite, quad.isShade());
    }

    private float unInterpolate(float value, float min, float max) {
        float span = max - min;
        if (span <= 0.00001f) {
            return 0f;
        }
        return ((value - min) / span) * 16f;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particleSprite;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }

    public List<BakedQuad> getQuadsForSpeciesCached(int species) {
        return getQuadsForSpecies(species);
    }

    private static class SpeciesOverrides extends ItemOverrides {
        private final ProceduralPlantBakedModel parent;
        private final Map<Integer, ItemModel> itemCache = new ConcurrentHashMap<>();

        private SpeciesOverrides(ProceduralPlantBakedModel parent) {
            this.parent = parent;
        }

        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            int species = ProceduralPlantItem.readSpecies(stack);
            return itemCache.computeIfAbsent(species, id -> new ItemModel(parent, id));
        }
    }

    private static class ItemModel implements BakedModel {
        private final ProceduralPlantBakedModel parent;
        private final int species;

        private ItemModel(ProceduralPlantBakedModel parent, int species) {
            this.parent = parent;
            this.species = species;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            if (side != null) {
                return Collections.emptyList();
            }
            return parent.getQuadsForSpeciesCached(species);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return parent.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return parent.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return parent.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return parent.getParticleIcon();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public ItemTransforms getTransforms() {
            return parent.getTransforms();
        }
    }
}
