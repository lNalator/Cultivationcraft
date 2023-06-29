package DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.Lungs;

import DaoOfModding.Cultivationcraft.Client.ClientListeners;
import DaoOfModding.Cultivationcraft.Client.GUI.animatedTexture;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.Lungs.Lung.Lung;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.awt.*;

public class Lungs
{
    protected animatedTexture lungs = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/lungtube.png"));
    protected final animatedTexture lungsFilling = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/lungtubefilling.png"));

    protected Color breathingColor = Breath.AIR.color;

    LungConnection[] lung = {new LungConnection(new Lung())};


    public void breath(Player player)
    {
        Breath breathing = BreathingHandler.getBreath(player);
        breathingColor = breathing.color;

        lung[0].getLung().breath(10, breathing, player);
    }

    public void render(int x, int y)
    {
        float time = 0.4f + (1 - ((ClientListeners.tick-1) % 20) / 20.0f) * 0.6f;

        RenderSystem.setShaderColor(breathingColor.getRed() / 255.0f, breathingColor.getGreen() / 255.0f, breathingColor.getBlue() / 255.0f, 0.3f * breathingColor.getAlpha() / 255.0f);
        lungsFilling.render(x, (int)(y + (40 * (1 - time))), 40, (int)(40 * time), time);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        lungs.render(x, y, 40, 40);

        for (int i = 0; i < lung.length; i++)
            lung[i].renderLungs(x, y);
    }

    public int getLungAmount()
    {
        return lung.length;
    }

    public void setLung(int position, Lung newLung)
    {
        lung[position].setLung(newLung);
    }

    public LungConnection getConnection(int number)
    {
        return lung[number];
    }

    public float getCurrent(Breath breath)
    {
        return lung[0].getCurrent(breath);
    }

    public Lungs copy(Lungs lungCopy)
    {
        Lungs newLung = new Lungs();

        for (int i = 0; i < getLungAmount(); i++)
            newLung.setLung(i, getConnection(i).getLung());

        newLung.lung[0].getLung().setCurrent(lungCopy.getCurrent(lung[0].getLung().getBreath()) / 2);
        newLung.breathingColor = lungCopy.breathingColor;

        return newLung;
    }
}