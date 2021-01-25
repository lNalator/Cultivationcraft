package DaoOfModding.Cultivationcraft.Client;

import static org.lwjgl.glfw.GLFW.*;

import DaoOfModding.Cultivationcraft.Client.Animations.BodyPartGUIs;
import DaoOfModding.Cultivationcraft.Client.Animations.BodyPartModels;
import DaoOfModding.Cultivationcraft.Client.Animations.BodyPartNames;
import DaoOfModding.Cultivationcraft.Client.GUI.BodyPartGUI;
import DaoOfModding.Cultivationcraft.Client.GUI.FlyingSwordContainerScreen;
import DaoOfModding.Cultivationcraft.Client.GUI.SkillHotbarOverlay;
import DaoOfModding.Cultivationcraft.Common.Misc;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import DaoOfModding.Cultivationcraft.Common.Register;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.UUID;


public class ClientItemControl
{
    public static IWorld thisWorld;


    public static void init(FMLClientSetupEvent event)
    {
        KeybindingControl.init();

        ScreenManager.registerFactory(Register.ContainerTypeFlyingSword, FlyingSwordContainerScreen::new);

        setupDefaultBodyPartGUIs();
    }

    public static void setupDefaultBodyPartGUIs()
    {
        BodyPartGUIs.addGUI(BodyPartNames.DefaultBody, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.DefaultBody + ".png"), 0, 0, 18, 28, true));
        BodyPartGUIs.addGUI(BodyPartNames.DefaultHead, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.DefaultHead + ".png"), 0, -2, 18, 20, true));
        BodyPartGUIs.addGUI(BodyPartNames.DefaultLeftArm, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.DefaultLeftArm + ".png"), 2, -1, 11, 29, true));
        BodyPartGUIs.addGUI(BodyPartNames.DefaultRightArm, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.DefaultRightArm + ".png"), -2, -1, 11, 29, true));
        BodyPartGUIs.addGUI(BodyPartNames.DefaultLeftLeg, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.DefaultLeftLeg + ".png"), 1, 2, 8, 30, true));
        BodyPartGUIs.addGUI(BodyPartNames.DefaultRightLeg, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.DefaultRightLeg + ".png"), -1, 2, 8, 30, true));

        BodyPartGUIs.addGUI(BodyPartNames.jawPart, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.jawPart + ".png"), 0, -2, 18, 20, true));
        BodyPartGUIs.addGUI(BodyPartNames.reverseJointLegPart, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.reverseJointLegPart + "left.png"), 1, 2, 8, 30, true));
        BodyPartGUIs.addGUI(BodyPartNames.reverseJointLegPart, new BodyPartGUI(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/bodyparts/" + BodyPartNames.reverseJointLegPart + "right.png"), -1, 2, 8, 30, true));
    }
}
