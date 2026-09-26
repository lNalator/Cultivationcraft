package DaoOfModding.Cultivationcraft.Common.Alchemy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.UUID;

/** Names and identities survive reloads and are shared across dimensions. */
public final class PillCatalog extends SavedData {
    private String identity = UUID.randomUUID().toString();
    private CompoundTag names = new CompoundTag();
    public static PillCatalog get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PillCatalog::load, PillCatalog::new,
                "cultivationcraft_pill_catalog");
    }
    private static PillCatalog load(CompoundTag tag) {
        PillCatalog catalog = new PillCatalog();
        catalog.identity = tag.getString("Identity");
        catalog.names = tag.getCompound("Names");
        return catalog;
    }
    public String key(PillDefinition definition) { return identity + "/" + definition.id(); }
    public String name(PillDefinition definition, ServerLevel level) {
        String key = definition.id().toString();
        if (!names.contains(key)) {
            names.putString(key, definition.names().get(level.random.nextInt(definition.names().size())));
            setDirty();
        }
        return names.getString(key);
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putString("Identity", identity);
        tag.put("Names", names);
        return tag;
    }
}
