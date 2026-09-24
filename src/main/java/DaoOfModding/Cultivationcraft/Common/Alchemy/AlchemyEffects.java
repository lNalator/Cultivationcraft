package DaoOfModding.Cultivationcraft.Common.Alchemy;

import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Visible timers for the batch-specific benefits managed by PillEffects. */
public final class AlchemyEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Cultivationcraft.MODID);
    public static final RegistryObject<MobEffect> QI_RESTORATION = EFFECTS.register("qi_restoration", () -> new PillStatus(0x88C5CC));
    public static final RegistryObject<MobEffect> QI_ABSORPTION = EFFECTS.register("qi_absorption", () -> new PillStatus(0xB6ACD4));
    public static void init(IEventBus bus) { EFFECTS.register(bus); }
    private static final class PillStatus extends MobEffect {
        private PillStatus(int color) { super(MobEffectCategory.BENEFICIAL, color); }
    }
}
