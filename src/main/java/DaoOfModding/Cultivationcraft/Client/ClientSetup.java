package DaoOfModding.Cultivationcraft.Client;

import DaoOfModding.Cultivationcraft.Common.Blocks.BlockRegister;
import DaoOfModding.Cultivationcraft.Common.Register;
import DaoOfModding.Cultivationcraft.Client.GUI.Screens.AlchemyCauldronScreen;
import DaoOfModding.Cultivationcraft.Client.Renderers.BlockEntityRenderers.AlchemyCauldronRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    @SubscribeEvent
    public static void registerPillDecorations(net.minecraftforge.client.event.RegisterItemDecorationsEvent event) {
        event.register(DaoOfModding.Cultivationcraft.Common.Items.ItemRegister.ALCHEMY_PILL.get(), AlchemyPillPresentation::renderCooldown);
    }

    @SubscribeEvent
    public static void registerPillColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register(AlchemyPillPresentation::color, DaoOfModding.Cultivationcraft.Common.Items.ItemRegister.ALCHEMY_PILL.get());
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockRegister.ALCHEMY_CAULDRON_ENTITY.get(), AlchemyCauldronRenderer::new);
    }

    @SubscribeEvent
    public static void setKeybindings(RegisterKeyMappingsEvent event) {
        KeybindingControl.init(event);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(Register.ALCHEMY_CAULDRON_MENU.get(), AlchemyCauldronScreen::new));
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                BlockRegister.PROCEDURAL_PLANT.get(), RenderType.cutout()));
    }
}
