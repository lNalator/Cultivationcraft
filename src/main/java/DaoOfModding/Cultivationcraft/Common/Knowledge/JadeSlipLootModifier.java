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

public final class JadeSlipLootModifier extends LootModifier {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Cultivationcraft.MODID);
    public static final Codec<JadeSlipLootModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(instance.group(Codec.STRING.fieldOf("pool").forGetter(modifier -> modifier.pool),
                    Codec.floatRange(0, 1).fieldOf("chance").forGetter(modifier -> modifier.chance)))
            .apply(instance, JadeSlipLootModifier::new));
    static { SERIALIZERS.register("jade_slip", () -> CODEC); }
    private final String pool;
    private final float chance;
    private JadeSlipLootModifier(LootItemCondition[] conditions, String pool, float chance) {
        super(conditions); this.pool = pool; this.chance = chance;
    }
    public static void init(IEventBus bus) { SERIALIZERS.register(bus); }
    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getRandom().nextFloat() >= chance) return loot;
        var choices = KnowledgeEntry.all().stream().filter(entry -> entry.hasSlip() && entry.available() && entry.lootPools().contains(pool)).toList();
        int weight = choices.stream().mapToInt(KnowledgeEntry::weight).sum();
        if (weight == 0) return loot;
        int choice = context.getRandom().nextInt(weight);
        for (KnowledgeEntry entry : choices) {
            choice -= entry.weight();
            if (choice < 0) {
                loot.add(PlayerKnowledge.create(context.getLevel(), entry, context.getRandom()));
                break;
            }
        }
        return loot;
    }
}
