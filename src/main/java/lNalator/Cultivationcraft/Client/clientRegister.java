package lNalator.Cultivationcraft.Client;

import lNalator.Cultivationcraft.Client.GUI.ItemDecorators.FlyingSwordDecorator;
import lNalator.Cultivationcraft.Client.Particles.*;
import lNalator.Cultivationcraft.Client.Particles.Blood.BloodParticle;
import lNalator.Cultivationcraft.Client.Particles.Blood.GaseousBloodParticle;
import lNalator.Cultivationcraft.Client.Particles.FireParticle.FireParticle;
import lNalator.Cultivationcraft.Client.Particles.Spit.SpitParticle;
import lNalator.Cultivationcraft.Client.Particles.WaterParticle.WaterParticle;
import lNalator.Cultivationcraft.Client.Particles.WindParticle.WindParticle;
import lNalator.Cultivationcraft.Common.Register;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

public class clientRegister {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class RegistryEvents {

        @SubscribeEvent
        public static void onParticleFactoryRegistration(RegisterParticleProvidersEvent event) {
            event.register(Register.qiParticleType.get(), QiParticle.Factory::new);
            event.register(Register.bloodParticleType.get(), BloodParticle.Factory::new);
            event.register(Register.gaseousBloodParticleType.get(), GaseousBloodParticle.Factory::new);
            event.register(Register.spitParticleType.get(), SpitParticle.Factory::new);
            event.register(Register.waterParticleType.get(), WaterParticle.Factory::new);
            event.register(Register.windParticleType.get(), WindParticle.Factory::new);
            event.register(Register.fireParticleType.get(), FireParticle.Factory::new);
        }

        @SubscribeEvent
        public static void onItemDecoratorRegistration(RegisterItemDecorationsEvent event) {
            FlyingSwordDecorator decorator = new FlyingSwordDecorator();

            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                event.register(item, decorator);
            }
        }
    }
}
