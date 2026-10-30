package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Elements {

    protected static HashMap<ResourceLocation, Element> Elements = new HashMap<>();

    public static final ResourceLocation noElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.none");
    public static final ResourceLocation fireElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.fire");
    public static final ResourceLocation earthElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.earth");
    public static final ResourceLocation woodElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.wood");
    public static final ResourceLocation windElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.wind");
    public static final ResourceLocation waterElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.water");

    public static final ResourceLocation iceElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.ice");
    public static final ResourceLocation lightningElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.lightning");

    public static final ResourceLocation anyElement = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.elements.any");

    protected static ArrayList<ResourceLocation> defaultElements = new ArrayList<>();
    protected static HashMap<ResourceKey<Level>, ArrayList<ResourceLocation>> dimensionElements = new HashMap<>();

    // Golden Core pressure loop: each element presses the next one
    protected static final ResourceLocation[] PRESSURE_LOOP = {fireElement, woodElement, windElement, earthElement, waterElement};

    // Whether an attacker's core element presses (deals extra damage to) the target's core element.
    // Ice and Lightning press every other element and are pressed by every other element; Pure (noElement) is outside the loop
    public static boolean presses(ResourceLocation attacker, ResourceLocation target) {
        if (attacker.equals(target) || attacker.equals(noElement) || target.equals(noElement) || attacker.equals(anyElement) || target.equals(anyElement)) {
            return false;
        }

        if (attacker.equals(iceElement) || attacker.equals(lightningElement) || target.equals(iceElement) || target.equals(lightningElement)) {
            return true;
        }

        for (int i = 0; i < PRESSURE_LOOP.length; i++) {
            if (PRESSURE_LOOP[i].equals(attacker)) {
                return PRESSURE_LOOP[(i + 1) % PRESSURE_LOOP.length].equals(target);
            }
        }

        return false;
    }

    public static void init() {
        createElements();
        createMaterialElements();

        createDimensionRules();
    }

    protected static void createDimensionRules() {
        addDefaultDimensionRule(noElement);
        addDefaultDimensionRule(fireElement);
        addDefaultDimensionRule(earthElement);
        addDefaultDimensionRule(woodElement);
        addDefaultDimensionRule(windElement);
        addDefaultDimensionRule(waterElement);
        addDefaultDimensionRule(iceElement);
        addDefaultDimensionRule(lightningElement);

        addDimensionRule(Level.NETHER, fireElement);
    }

    public static void addDefaultDimensionRule(ResourceLocation element) {
        defaultElements.add(element);
    }

    public static void addDimensionRule(ResourceKey<Level> dimension, ResourceLocation element) {
        if (!dimensionElements.containsKey(dimension)) {
            dimensionElements.put(dimension, new ArrayList<ResourceLocation>());
        }

        dimensionElements.get(dimension).add(element);
    }

    public static ArrayList<ResourceLocation> getDimensionRules(ResourceKey<Level> dimension) {
        if (dimensionElements.containsKey(dimension)) {
            return dimensionElements.get(dimension);
        }

        return defaultElements;
    }

    protected static void createElements() {
        addElement(new Element(noElement, new Color(1f, 1f, 1f), 0));
        addElement(new FireElement(fireElement, new Color(1f, 0, 0), 0.2));
        addElement(new EarthElement(earthElement, new Color(1f, 0.5f, 0), 0.75));
        addElement(new WoodElement(woodElement, new Color(0, 0.5f, 0), 0.05));
        addElement(new WindElement(windElement, new Color(0, 1f, 0.25f), 0.8));
        addElement(new WaterElement(waterElement, new Color(0, 0, 1f), 0.2));

        addVariant(waterElement, new IceElement(iceElement, new Color(0, 1f, 1f), 0.5, 0.02));
        addVariant(windElement, new LightningElement(lightningElement, new Color(1f, 1f, 0), 0, 0.02));
    }

    // Replaces the 1.19 Material -> element map, see data/cultivationcraft/tags/blocks/elements/
    protected static void createMaterialElements() {
        BlockElements.addBlockElement(BlockBehaviour.BlockStateBase::isAir, windElement);

        addElementTag("water", waterElement);
        addElementTag("ice", iceElement);
        addElementTag("fire", fireElement);
        addElementTag("wood", woodElement);
        addElementTag("earth", earthElement);
    }

    protected static void addElementTag(String name, ResourceLocation element) {
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, new ResourceLocation(Cultivationcraft.MODID, "elements/" + name));
        BlockElements.addBlockElement(state -> state.is(tag), element);
    }

    // Adds a new element of the specified resourceLocation to the Elements list
    public static void addElement(Element element) {
        Elements.put(element.getResourceLocation(), element);
    }

    // Adds a new variant of the specified name to the Elements list
    public static void addVariant(ResourceLocation elementLocation, ElementVariant variant) {
        Elements.put(variant.getResourceLocation(), variant);

        getElement(elementLocation).addVariant(variant);
    }

    public static Set<ResourceLocation> getElements() {
        return Elements.keySet();
    }

    // Returns the element of the supplied id
    public static Element getElement(ResourceLocation element) {
        if (element.compareTo(anyElement) == 0) {
            return Elements.get(noElement);
        }

        return Elements.get(element);
    }
}
