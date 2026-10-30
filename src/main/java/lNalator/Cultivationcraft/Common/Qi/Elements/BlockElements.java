package lNalator.Cultivationcraft.Common.Qi.Elements;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class BlockElements {

    // Block Materials were removed in 1.20: elements are matched with block state predicates (mostly block tags), checked in insertion order
    protected static LinkedHashMap<Predicate<BlockState>, ResourceLocation> blockElement = new LinkedHashMap<>();

    public static void addBlockElement(Predicate<BlockState> predicate, ResourceLocation Element) {
        blockElement.put(predicate, Element);
    }

    public static ResourceLocation getBlockElement(BlockState state) {
        for (Map.Entry<Predicate<BlockState>, ResourceLocation> entry : blockElement.entrySet()) {
            if (entry.getKey().test(state)) {
                return entry.getValue();
            }
        }

        return Elements.noElement;
    }
}
