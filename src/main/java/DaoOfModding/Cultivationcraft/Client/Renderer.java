package DaoOfModding.Cultivationcraft.Client;

import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

public class Renderer
{
    public static void renderTechniques()
    {
        // Loop through all players in the world
        for (PlayerEntity player : Minecraft.getInstance().world.getPlayers())
        {
            // Grab the players technique list and try to render them
            ICultivatorTechniques techniques = CultivatorTechniques.getCultivatorTechniques(player);

            for (int i = 0; i < 10; i++)
                if (techniques.getTechnique(i) != null && techniques.getTechnique(i).isActive())
                    techniques.getTechnique(i).render();
        }

        // Grab the player characters techniques and render them from the players view
        if (Minecraft.getInstance().player != null)
        {
            ICultivatorTechniques techniques = CultivatorTechniques.getCultivatorTechniques(Minecraft.getInstance().player);

            for (int i = 0; i < 10; i++)
                if (techniques.getTechnique(i) != null && techniques.getTechnique(i).isActive())
                    techniques.getTechnique(i).renderPlayerView();
        }
    }
}
