package DaoOfModding.Cultivationcraft.Common.Knowledge;

import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;

public final class JadeSlipLootModifier extends LootModifier {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Cultivationcraft.MODID);
    public static final Codec<JadeSlipLootModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(instance.group(Codec.STRING.fieldOf("pool").forGetter(modifier -> modifier.pool),
                    Codec.floatRange(0, 1).fieldOf("chance").forGetter(modifier -> modifier.chance),
                    Codec.intRange(1, 8).optionalFieldOf("min_slips", 1).forGetter(modifier -> modifier.minSlips),
                    Codec.intRange(1, 8).optionalFieldOf("max_slips", 1).forGetter(modifier -> modifier.maxSlips)))
            .apply(instance, JadeSlipLootModifier::new));
    static { SERIALIZERS.register("jade_slip", () -> CODEC); }
    private final String pool;
    private final float chance;
    private final int minSlips;
    private final int maxSlips;
    private record Choice(KnowledgeEntry entry, double weight) {}
    private JadeSlipLootModifier(LootItemCondition[] conditions, String pool, float chance, int minSlips, int maxSlips) {
        super(conditions);
        if (minSlips > maxSlips) throw new IllegalArgumentException("Jade slip min_slips must not exceed max_slips");
        this.pool = pool; this.chance = chance; this.minSlips = minSlips; this.maxSlips = maxSlips;
    }
    public static void init(IEventBus bus) { SERIALIZERS.register(bus); }
    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getRandom().nextFloat() >= chance) return loot;
        var choices = new ArrayList<Choice>();
        for (KnowledgeEntry entry : KnowledgeEntry.all()) {
            if (!entry.hasSlip() || !entry.available()) continue;
            double weight = entry.lootWeight(pool, context.getQueriedLootTableId());
            if (weight > 0) choices.add(new Choice(entry, weight));
        }
        int count = minSlips + context.getRandom().nextInt(maxSlips - minSlips + 1);
        for (int roll = 0; roll < count && !choices.isEmpty(); roll++) {
            double total = choices.stream().mapToDouble(Choice::weight).sum();
            double draw = context.getRandom().nextDouble() * total;
            int selected = choices.size() - 1;
            for (int i = 0; i < choices.size(); i++) {
                draw -= choices.get(i).weight();
                if (draw < 0) { selected = i; break; }
            }
            // Sample without replacement so multi-slip chests contain different teachings.
            KnowledgeEntry entry = choices.remove(selected).entry();
            loot.add(PlayerKnowledge.create(context.getLevel(), entry, context.getRandom()));
        }
        return loot;
    }
}
