package lNalator.Cultivationcraft;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import lNalator.Cultivationcraft.Client.ClientItemControl;
import lNalator.Cultivationcraft.Client.Animations.GenericQiPoses;
import lNalator.Cultivationcraft.Client.Textures.initTextures;
import lNalator.Cultivationcraft.Client.Tooltip.PlantBadgeTooltip;
import lNalator.Cultivationcraft.Client.Tooltip.PlantBadgeTooltipData;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Reflection;
import lNalator.Cultivationcraft.Common.Register;
import lNalator.Cultivationcraft.Common.Advancements.CultivationAdvancements;
import lNalator.Cultivationcraft.Common.Blocks.BlockRegister;
import lNalator.Cultivationcraft.Common.Items.ItemRegister;
import lNalator.Cultivationcraft.Common.Qi.ExternalCultivationHandler;
import lNalator.Cultivationcraft.Common.Qi.QiSourceConfig;
import lNalator.Cultivationcraft.Common.Qi.TechniqueControl;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPartNames;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.Lungs.BreathingHandler;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Quests.DefaultQuests;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import lNalator.Cultivationcraft.Network.PacketHandler;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


// The value here should match an entry in the META-INF/mods.toml file
@Mod("cultivationcraft")
public class Cultivationcraft {
    public static final String MODID = "cultivationcraft";

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    protected static final ModList MOD_LIST = ModList.get();

    public Cultivationcraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonInit);
        modEventBus.addListener(this::clientInit);
        modEventBus.addListener(this::registerTooltipFactories);
        modEventBus.addListener(this::buildCreativeTabs);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.Server.spec, "cultivationcraft.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.Client.spec, "cultivationcraft-client.toml");

        Register.init(modEventBus);
        BlockRegister.init(modEventBus);
        ItemRegister.init(modEventBus);
        lNalator.Cultivationcraft.Common.Knowledge.JadeSlipLootModifier.init(modEventBus);
        lNalator.Cultivationcraft.Common.Alchemy.AlchemyEffects.init(modEventBus);
        lNalator.Cultivationcraft.Common.Qi.Elements.ElementalStates.init(modEventBus);
        BodyPartNames.registerLungLocations();
        CultivationAdvancements.init(modEventBus);
    }

    protected void commonInit(final FMLCommonSetupEvent event) {
        PacketHandler.init();

        QiSourceConfig.init();
        Elements.init();
        TechniqueControl.init();
        BodyPartNames.init();
        Reflection.setup();
        DefaultQuests.init();
        BreathingHandler.init();
        ExternalCultivationHandler.init();
    }
    protected void clientInit(final FMLClientSetupEvent event) {
        ClientItemControl.init(event);
        GenericQiPoses.init();
        DefaultTechniqueStatIDs.init();
        initTextures.init();
        // Tooltip factories are registered via RegisterClientTooltipComponentFactoriesEvent
    }

    protected void registerTooltipFactories(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(PlantBadgeTooltipData.class, PlantBadgeTooltip::new);
    }

    // Item.Properties.tab() was removed in 1.19.3: items join the vanilla tabs here
    private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ItemRegister.JADE_CHUNK);
            event.accept(ItemRegister.SPIRIT_STONE);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ItemRegister.ELEMENT_WAND);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(BlockRegister.ALCHEMY_CAULDRON_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(BlockRegister.JADE_ORE_ITEM);
            event.accept(BlockRegister.SPIRIT_STONE_ORE_ITEM);
        }
    }
}
