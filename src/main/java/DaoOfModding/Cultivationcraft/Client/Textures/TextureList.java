package DaoOfModding.Cultivationcraft.Client.Textures;

import DaoOfModding.mlmanimator.Client.Models.TextureHandler;
import net.minecraft.client.model.geom.builders.UVPair;

public class TextureList
{
    public static final String skin = "SKIN";
    public static final String bone = "BONE";
    public static final String petal = "PETAL";
    public static final String elementalColored = "EBLANK";

    public static final UVPair boneSize = new UVPair(64, 64);
    public static final UVPair petalSize = new UVPair(64, 64);

    public static void updateTextures(TextureHandler handler)
    {
        handler.addTexture(elementalColored, handler.getTexture(TextureHandler.BLANK));
    }
}
